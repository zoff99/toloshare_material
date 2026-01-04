@file:Suppress("SpellCheckingInspection", "LocalVariableName", "ConvertToStringTemplate", "PrivatePropertyName", "CascadeIf")

package ch.fhnw.osmdemo.viewmodel

import APPDIRS
import com.kdroid.composetray.utils.SingleInstanceManager.configuration
import com.zoffcc.applications.trifa.Log
import com.zoffcc.applications.trifa.MainActivity.Companion.PREF__tox_savefile_dir
import com.zoffcc.applications.trifa.TAG
import io.ktor.client.*
import io.ktor.client.engine.apache5.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager
import org.apache.hc.core5.util.TimeValue
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.Rect
import org.jetbrains.skia.SamplingMode
import org.jetbrains.skia.Surface
import java.io.File

class CachingOsmTileLoader() {
    private val fs: FileSystem = FileSystem.SYSTEM
    private val cacheDir = platformCacheDir()
    private val client = createHttpClient()
    private val MEMCACHE_MAX_ENTRIES = 2000
    private val HIGHDPI_MODE = 0
    private val inMemoryCache = LRUCache<String, ByteArray>(MEMCACHE_MAX_ENTRIES, { k, v ->
                                                                    val path = tilePath(k)
                                                                    if(!fs.exists(path)){
                                                                        writeTile(path = path, bytes = v)
                                                                    }})

    // private fun createOSMUrl(row: Int, col: Int, zoomLvl: Int): String = "https://tile.openstreetmap.org/$zoomLvl/$col/$row.png"

    private fun createOSMUrl(row: Int, col: Int, zoomLvl: Int): String
    {
        if (HIGHDPI_MODE == 0)
        {
            return "https://tile.openstreetmap.org/$zoomLvl/$col/$row.png"
        }
        else if (HIGHDPI_MODE == 1)
        {
            val new_x_y = getParentCoordinates(col, row)
            val new_zoom = zoomLvl - 1
            val new_url = "https://tile.openstreetmap.org/${new_zoom}/${new_x_y.first}/${new_x_y.second}.png" // println("XXXXXXX: $row $col $new_x_y $new_url ")
            return new_url
        }
        else // (HIGHDPI_MODE == 2)
        {
            val new_x_y = getGrandParentCoords(col , row)
            val new_zoom = zoomLvl - 2
            val new_url = "https://tile.openstreetmap.org/${new_zoom}/${new_x_y.first}/${new_x_y.second}.png" // println("XXXXXXX: $row $col $new_x_y $new_url ")
            return new_url
        }
    }

    //private fun createOSMUrl(row: Int, col: Int, zoomLvl: Int): String = "https://tile.osm.ch/osm-swiss-style/$zoomLvl/$col/$row.png"
    //private fun createOSMUrl(row: Int, col: Int, zoomLvl: Int): String = "https://tile.osm.ch/switzerland/$zoomLvl/$col/$row.png"
    //private fun createOSMUrl(row: Int, col: Int, zoomLvl: Int): String = "https://b.tile.opentopomap.org/$zoomLvl/$col/$row.png"

    private val tileSize = 256

    suspend fun loadTile(row: Int, col: Int, zoomLvl: Int): ByteArray {
        val cacheKey = "$zoomLvl/$col/$row"
        return when {
            inMemoryCache.containsKey(cacheKey) -> { inMemoryCache[cacheKey]!! }
            tileExists(zoomLvl, col, row)       -> {
                val tile = readTile(tilePath(zoomLvl, col, row))
                                                        if (HIGHDPI_MODE == 0)
                                                        {
                                                            inMemoryCache[cacheKey] = tile
                                                            tile
                                                        }
                                                        else if (HIGHDPI_MODE == 1)
                                                        {
                                                            val tile2 = magnifyTileBytes(tile, getQuadrantIndex(col, row))
                                                            inMemoryCache[cacheKey] = tile2
                                                            tile2
                                                        }
                                                        else // (HIGHDPI_MODE == 2)
                                                        {
                                                            val tile2 = magnifyTwoZoomLevels(tile, getSubQuadrantOffset(col, row))
                                                            inMemoryCache[cacheKey] = tile2
                                                            tile2
                                                        }
                                                   }
            else                                -> { try {
                                                         Log.i(TAG, "DDDDDDDDDD:")
                                                         val response = client.get(createOSMUrl(row, col, zoomLvl))
                                                         // val response = client.get("http://127.0.0.1/")
                                                         Log.i(TAG, "DDDDDDDDDD:****")
                                                         if (response.status == HttpStatusCode.OK) {
                                                             val tile = response.readRawBytes()
                                                             if (HIGHDPI_MODE == 0)
                                                             {
                                                                 inMemoryCache[cacheKey] = tile
                                                                 tile
                                                             }
                                                             else if (HIGHDPI_MODE == 1)
                                                             {
                                                                 val tile2 = magnifyTileBytes(tile, getQuadrantIndex(col, row))
                                                                 inMemoryCache[cacheKey] = tile2
                                                                 tile2
                                                             }
                                                             else // (HIGHDPI_MODE == 2)
                                                             {
                                                                 val tile2 = magnifyTwoZoomLevels(tile, getSubQuadrantOffset(col, row))
                                                                 inMemoryCache[cacheKey] = tile2
                                                                 tile2
                                                             }
                                                         } else {
                                                             Log.i(TAG, "DDDDDD:res=" + response.status)
                                                             ByteArray(tileSize)
                                                         }
                                                     } catch (e: Exception) {
                                                         e.printStackTrace()
                                                         Log.i(TAG, "DDDDDD:EE02")
                                                         ByteArray(tileSize)
                                                     }
                                                   }
        }
    }


    private fun readTile(path: Path)                     = fs.read(path) {
        Log.i(TAG, "readTile:" + path)
        readByteArray()
    }
    private fun writeTile(path: Path, bytes: ByteArray)  = fs.write(path) {
        Log.i(TAG, "writeTile:" + path)
        write(bytes)
    }

    private fun tilePath(cacheKey: String) : Path {
        val parts = cacheKey.split("/")
        val dir = cacheDir / parts[0] / parts[1]
        if (!fs.exists(dir)) {
            fs.createDirectories(dir)
        }
        return dir / "${parts[2]}.png"
    }

    private fun tilePath(z: Int, x: Int, y: Int): Path {
        val dir = cacheDir / z.toString() / x.toString()
        if (!fs.exists(dir)) {
            fs.createDirectories(dir)
        }
        return dir / "$y.png"
    }

    private fun tileExists(z: Int, x: Int, y: Int) : Boolean {
        val dir = cacheDir / z.toString() / x.toString()
        return fs.exists(dir) && fs.exists(dir / "$y.png" )
    }

    /**
     * Extracts a quarter from an OSM tile (ByteArray), magnifies it to 256x256,
     * and returns the result as a new ByteArray.
     *
     * @param tileData Raw ByteArray of the source 256x256 tile (PNG/JPG).
     * @param quarter Index (0: Top-Left, 1: Top-Right, 2: Bottom-Left, 3: Bottom-Right).
     * @return A magnified 256x256 tile as an encoded ByteArray (PNG).
     */
    fun magnifyTileBytes(tileData: ByteArray, quarter: Int): ByteArray {
        // 1. Decode input bytes into a Skia Image
        val source = Image.makeFromEncoded(tileData)

        val size = 256f
        val half = size / 2f

        // 2. Define the source 128x128 crop area
        val srcRect = when (quarter) {
            0 -> Rect.makeXYWH(0f, 0f, half, half)        // Top-Left
            1 -> Rect.makeXYWH(half, 0f, half, half)     // Top-Right
            2 -> Rect.makeXYWH(0f, half, half, half)     // Bottom-Left
            3 -> Rect.makeXYWH(half, half, half, half)  // Bottom-Right
            else -> throw IllegalArgumentException("Quarter must be 0-3")
        }

        // 3. Create a surface to draw the magnified 256x256 result
        val surface = Surface.makeRasterN32Premul(256, 256)
        val canvas = surface.canvas

        // 4. Draw the crop with high-quality cubic upscaling (MITCHELL)
        canvas.drawImageRect(
            source,
            srcRect,
            Rect.makeWH(size, size),
            SamplingMode.MITCHELL,
            null,
            true
        )

        // 5. Capture the result and encode it back to bytes
        val magnifiedImage = surface.makeImageSnapshot()
        val data = magnifiedImage.encodeToData(EncodedImageFormat.PNG, 100)
            ?: throw IllegalStateException("Failed to encode result image")

        return data.bytes
    }


    /**
     * Extracts and magnifies an OSM tile quarter using Skiko (Skia).
     *
     * @param source The original 256x256 Skia Image.
     * @param quarter Index (0: Top-Left, 1: Top-Right, 2: Bottom-Left, 3: Bottom-Right).
     * @return A new 256x256 magnified Image.
     */
    fun magnifyTileQuarterSkiko(source: Image, quarter: Int): Image {
        val size = 256f
        val half = size / 2f

        // 1. Define the source 128x128 crop area (src)
        val srcRect = when (quarter) {
            0 -> Rect.makeXYWH(0f, 0f, half, half)        // Top-Left
            1 -> Rect.makeXYWH(half, 0f, half, half)     // Top-Right
            2 -> Rect.makeXYWH(0f, half, half, half)     // Bottom-Left
            3 -> Rect.makeXYWH(half, half, half, half)  // Bottom-Right
            else -> throw IllegalArgumentException("Quarter must be 0-3")
        }

        // 2. Define the destination 256x256 area (dst)
        val dstRect = Rect.makeWH(size, size)

        // 3. Create a surface to draw the result
        val surface = Surface.makeRasterN32Premul(256, 256)
        val canvas = surface.canvas

        // 4. Draw with high-quality sampling to minimize aliasing
        // drawImageRect automatically scales the 'src' selection to fill 'dst'
        canvas.drawImageRect(
            source,
            srcRect,
            dstRect,
            SamplingMode.MITCHELL, // High quality cubic sampling for 2026 standards
            null,
            true
        )

        // 5. Capture the result as a new Image
        return surface.makeImageSnapshot()
    }

    /**
     * Magnifies a specific sub-tile from a tile 2 zoom levels higher.
     *
     * @param tileData Raw ByteArray of the parent tile (Zoom Z).
     * @param offsetX The x-offset (0-3) in the 4x4 grid.
     * @param offsetY The y-offset (0-3) in the 4x4 grid.
     */
    fun magnifyTwoZoomLevels(tileData: ByteArray, offset: Pair<Int, Int>): ByteArray {
        val source = Image.makeFromEncoded(tileData)

        // Each sub-tile at Z+2 is 1/4 the width/height of Z (256 / 4 = 64px)
        val subSize = 64f
        val targetSize = 256f

        // 1. Define the 64x64 source rectangle
        val srcRect = Rect.makeXYWH(
            offset.first * subSize,
            offset.second * subSize,
            subSize,
            subSize
        )

        // 2. Define the 256x256 destination rectangle
        val dstRect = Rect.makeWH(targetSize, targetSize)

        // 3. Render and Scale
        val surface = Surface.makeRasterN32Premul(256, 256)
        val canvas = surface.canvas

        // Use CATMULL_ROM or MITCHELL for extreme 4x upscaling in 2026
        canvas.drawImageRect(
            source,
            srcRect,
            dstRect,
            SamplingMode.CATMULL_ROM,
            null,
            true
        )

        val data = surface.makeImageSnapshot().encodeToData(EncodedImageFormat.PNG, 100)
        return data?.bytes ?: byteArrayOf()
    }

    /**
     * Calculates which quadrant index (0-3) a child tile occupies within its parent.
     */
    fun getQuadrantIndex(childX: Int, childY: Int): Int {
        val xMod = childX % 2
        val yMod = childY % 2
        return yMod * 2 + xMod
    }

    /**
     * Calculates parent tile coordinates from child coordinates.
     */
    fun getParentCoordinates(childX: Int, childY: Int): Pair<Int, Int> {
        return Pair(childX / 2, childY / 2)
    }

    fun getSubQuadrantOffset(targetX: Int, targetY: Int): Pair<Int, Int> {
        // Returns the (0-3, 0-3) grid position within the parent tile
        return Pair(targetX % 4, targetY % 4)
    }

    fun getGrandParentCoords(targetX: Int, targetY: Int): Pair<Int, Int> {
        // Returns the coordinates of the tile 2 zoom levels up
        return Pair(targetX / 4, targetY / 4)
    }

}

fun platformCacheDir(): Path {
    // HINT: make this more elegant?
    val tilecache_dir = (APPDIRS.getUserCacheDir() + File.separator + "/tilecache/")
    File(tilecache_dir).mkdirs()
    return tilecache_dir.toPath()
}

fun createHttpClient(): HttpClient = HttpClient(Apache5) {
    engine {
        connectTimeout           = 1_000 // 3 seconds
        socketTimeout            = 1_000
        connectionRequestTimeout = 300

        // Configure async connection pooling
        customizeClient {
            val connectionManager = PoolingAsyncClientConnectionManager().apply {
                maxTotal           = 100
                defaultMaxPerRoute = 8
            }

            setConnectionManager(connectionManager)
            evictIdleConnections(TimeValue.ofSeconds(3))
        }
    }

    // Add timeout plugin for request-level control
    install(HttpTimeout) {
        requestTimeoutMillis = 500
        connectTimeoutMillis = 500
        socketTimeoutMillis  = 500
    }

    // Add default request configuration
    install(DefaultRequest) {
        // OSM services require a User-Agent header
        headers.append("User-Agent", "com.zoffcc.applications.toloshare_material")
        headers.append("Accept", "*/*")
    }
}
