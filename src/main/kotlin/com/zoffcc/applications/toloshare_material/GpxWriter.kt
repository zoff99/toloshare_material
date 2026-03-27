@file:Suppress("SpellCheckingInspection")

package com.zoffcc.applications.toloshare_material

import java.io.File
import java.io.RandomAccessFile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GpxWriter(directoryPath: String, filename: String) {
    private val gpxFile: File = File(directoryPath,
        if (filename.endsWith(".gpx")) filename else "$filename.gpx")

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        // CRITICAL: Forces the formatter to treat input 'Long' as UTC
        timeZone = java.util.TimeZone.getTimeZone("UTC")
    }
    private val footer = "\n    </trkseg>\n  </trk>\n</gpx>"

    // Lock object to synchronize file access
    private val fileLock = Any()

    init {
        synchronized(fileLock) {
            val dir = File(directoryPath)
            if (!dir.exists()) dir.mkdirs()

            if (!gpxFile.exists()) {
                val header = """<?xml version="1.0" encoding="UTF-8"?>
<gpx version="1.1" creator="GpxWriter" 
  xmlns="http://www.topografix.com"
  xmlns:gpxtpx="http://www.garmin.com">
  <trk>
    <name>${gpxFile.nameWithoutExtension}</name>
    <trkseg>$footer"""
                gpxFile.writeText(header)
            }
        }
    }

    /**
     * Appends a new track point to the GPX file.
     * Thread-safe: uses synchronized lock to prevent concurrent file modifications.
     *
     * @param lat The latitude of the point in decimal degrees (WGS84).
     * @param lon The longitude of the point in decimal degrees (WGS84).
     * @param timestamp Epoch time in milliseconds (e.g., from System.currentTimeMillis() or Location.getTime()) in your local timezone.
     * @param elevation The altitude above sea level in meters. Pass null if unknown.
     * @param speed The current travel speed in meters per second (m/s).
     * @param bearing The direction of travel in degrees (0.0 to 360.0), where 0 is North.
     */
    fun addPoint(
        lat: Double,
        lon: Double,
        timestamp: Long,
        elevation: Double? = null,
        speed: Float? = null,
        bearing: Float? = null
    ) {
        synchronized(fileLock) {
            val timeStr = dateFormat.format(Date(timestamp))
            val eleTag = if (elevation != null) "\n      <ele>$elevation</ele>" else ""

            val extensions = StringBuilder().apply {
                if (speed != null || bearing != null) {
                    append("\n      <extensions>\n        <gpxtpx:TrackPointExtension>")
                    speed?.let { append("\n          <gpxtpx:speed>$it</gpxtpx:speed>") }
                    bearing?.let { append("\n          <gpxtpx:course>$it</gpxtpx:course>") }
                    append("\n        </gpxtpx:TrackPointExtension>\n      </extensions>")
                }
            }.toString()

            val newEntry = """
      <trkpt lat="$lat" lon="$lon">$eleTag
        <time>$timeStr</time>$extensions
      </trkpt>$footer"""

            RandomAccessFile(gpxFile, "rw").use { raf ->
                val fileLength = raf.length()
                val footerBytes = footer.toByteArray(Charsets.UTF_8).size

                // Move pointer to just before the footer to overwrite it
                if (fileLength >= footerBytes) {
                    raf.seek(fileLength - footerBytes)
                } else {
                    raf.seek(fileLength)
                }

                raf.write(newEntry.toByteArray(Charsets.UTF_8))
            }
        }
    }
}
