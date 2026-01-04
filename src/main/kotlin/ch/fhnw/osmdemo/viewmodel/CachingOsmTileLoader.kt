@file:Suppress("SpellCheckingInspection", "LocalVariableName", "ConvertToStringTemplate", "PrivatePropertyName", "CascadeIf", "FunctionName", "MoveLambdaOutsideParentheses", "unused")

package ch.fhnw.osmdemo.viewmodel

import APPDIRS
import com.zoffcc.applications.trifa.Log
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
    private val tileSize = 256
    private val MEMCACHE_MAX_ENTRIES = 2000
    private val HIGHDPI_MODE: Int = 2
    private val inMemoryCache = LRUCache<String, ByteArray>(MEMCACHE_MAX_ENTRIES,
        { k, v ->
            // HINT: we dont write the tiles to disk cache right after we download them
        })

    @Suppress("SameParameterValue")
    private fun convert_xyz(row: Int, col: Int, zoomLvl: Int, highdpi_mode: Int): Triple<Int, Int, Int>
    {
        if (highdpi_mode == 1)
        {
            val new_x_y = getParentCoordinates(col, row)
            val new_zoom = zoomLvl - 1
            return Triple(new_x_y.second, new_x_y.first, new_zoom)
        }
        else if (highdpi_mode == 2)
        {
            val new_x_y = getGrandParentCoords(col, row)
            val new_zoom = zoomLvl - 2
            return Triple(new_x_y.second, new_x_y.first, new_zoom)
        }
        return Triple(row, col, zoomLvl)
    }

    private fun createOSMUrl(row: Int, col: Int, zoomLvl: Int): String
    {
        val res = convert_xyz(row, col, zoomLvl, HIGHDPI_MODE)
        val row_new = res.first
        val col_new = res.second
        val zoomLvl_new = res.third
        return "https://tile.openstreetmap.org/$zoomLvl_new/$col_new/$row_new.png"
    }

    fun get_cachekey(row: Int, col: Int, zoomLvl: Int): String
    {
        return ("$zoomLvl/$col/$row")
    }

    suspend fun loadTile(row: Int, col: Int, zoomLvl: Int): ByteArray {
        return when {
            inMemoryCache.containsKey(get_cachekey(row, col, zoomLvl)) -> { inMemoryCache[get_cachekey(row, col, zoomLvl)]!! }
            tileExists(row, col, zoomLvl) -> {
                val path_ = tilePath_on_disk(row, col, zoomLvl)
                Log.i(TAG, "EXISTS: path_=" + path_)
                val tile = readTile(path_)
                inMemoryCache[get_cachekey(row, col, zoomLvl)] = tile
                tile
            }
            else -> {
                try {
                    val osm_url = createOSMUrl(row, col, zoomLvl)
                    Log.i(TAG, "DDDDDDDDDD: $zoomLvl, $col, $row osm_url=" + osm_url)
                    val response = client.get(osm_url)
                    // val response = client.get("http://127.0.0.1/")
                    Log.i(TAG, "DDDDDDDDDD:****")
                    if (response.status == HttpStatusCode.OK) {
                        val tile = response.readRawBytes()
                        if (HIGHDPI_MODE == 0)
                        {
                            inMemoryCache[get_cachekey(row, col, zoomLvl)] = tile
                            val path = tilePath_on_disk(row, col, zoomLvl)
                            if(!fs.exists(path)){
                                writeTile(path = path, bytes = tile)
                            }
                            tile
                        }
                        else if (HIGHDPI_MODE == 1)
                        {
                            val tile2 = magnifyTileBytes(tile, getQuadrantIndex(col, row))
                            inMemoryCache[get_cachekey(row, col, zoomLvl)] = tile2
                            val path = tilePath_on_disk(row, col, zoomLvl)
                            if(!fs.exists(path)){
                                writeTile(path = path, bytes = tile2)
                            }
                            tile2
                        }
                        else // (HIGHDPI_MODE == 2)
                        {
                            val tile2 = magnifyTwoZoomLevels(tile, getSubQuadrantOffset(col, row))
                            inMemoryCache[get_cachekey(row, col, zoomLvl)] = tile2
                            val path = tilePath_on_disk(row, col, zoomLvl)
                           if(!fs.exists(path)){
                                writeTile(path = path, bytes = tile2)
                            }
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

    private fun tilePath_on_disk(row: Int, col: Int, zoomLvl: Int): Path {
        val dir = cacheDir / zoomLvl.toString() / col.toString()
        if (!fs.exists(dir)) {
            fs.createDirectories(dir)
        }
        return dir / "$row.png"
    }

    private fun tileExists(row: Int, col: Int, zoomLvl: Int) : Boolean {
        val dir = cacheDir / zoomLvl.toString() / col.toString()
        Log.i(TAG, "tileExists:1:" + (dir / "$row.png" ))
        return fs.exists(dir) && fs.exists(dir / "$row.png" )
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
    @Suppress("unused")
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
    Log.i(TAG, "CCCCCCD: " + tilecache_dir)
    File(tilecache_dir).mkdirs()
    return tilecache_dir.toPath()
}

fun createHttpClient(): HttpClient = HttpClient(Apache5) {
    engine {
        connectTimeout           = 3_000 // 3 seconds
        socketTimeout            = 3_000
        connectionRequestTimeout = 3_000

        // Configure async connection pooling
        customizeClient {
            val connectionManager = PoolingAsyncClientConnectionManager().apply {
                maxTotal           = 20
                defaultMaxPerRoute = 8
            }

            setConnectionManager(connectionManager)
            evictIdleConnections(TimeValue.ofSeconds(3))
        }
    }

    // Add timeout plugin for request-level control
    install(HttpTimeout) {
        requestTimeoutMillis = 2500
        connectTimeoutMillis = 2500
        socketTimeoutMillis  = 2500
    }

    // Add default request configuration
    install(DefaultRequest) {
        // OSM services require a User-Agent header
        headers.append("User-Agent", "com.zoffcc.applications.toloshare_material")
        headers.append("Accept", "*/*")
    }
}
