package ch.fhnw.osmdemo.viewmodel

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

class CachingOsmTileLoader() {
    private val fs: FileSystem = FileSystem.SYSTEM

    private val cacheDir = platformCacheDir()

    private val client = createHttpClient()

    private val inMemoryCache = LRUCache<String, ByteArray>(1000, { k, v ->
                                                                    val path = tilePath(k)
                                                                    if(!fs.exists(path)){
                                                                        writeTile(path = path, bytes = v)
                                                                    }})

    private fun createOSMUrl(row: Int, col: Int, zoomLvl: Int): String = "https://tile.openstreetmap.org/$zoomLvl/$col/$row.png"
    //private fun createOSMUrl(row: Int, col: Int, zoomLvl: Int): String = "https://tile.osm.ch/osm-swiss-style/$zoomLvl/$col/$row.png"
    //private fun createOSMUrl(row: Int, col: Int, zoomLvl: Int): String = "https://tile.osm.ch/switzerland/$zoomLvl/$col/$row.png"
    //private fun createOSMUrl(row: Int, col: Int, zoomLvl: Int): String = "https://b.tile.opentopomap.org/$zoomLvl/$col/$row.png"

    private val tileSize = 256

    suspend fun loadTile(row: Int, col: Int, zoomLvl: Int): ByteArray {
        val cacheKey = "$zoomLvl/$col/$row"
        return when {
            inMemoryCache.containsKey(cacheKey) -> { inMemoryCache[cacheKey]!! }

            tileExists(zoomLvl, col, row)       -> { val tile = readTile(tilePath(zoomLvl, col, row))
                                                     inMemoryCache[cacheKey] = tile
                                                     tile
                                                   }

            else                                -> { try {
                                                         val response = client.get(createOSMUrl(row, col, zoomLvl))
                                                         if (response.status == HttpStatusCode.OK) {
                                                             val tile = response.readRawBytes()
                                                             inMemoryCache[cacheKey] = tile
                                                             tile
                                                         } else {
                                                             ByteArray(tileSize)
                                                         }
                                                     } catch (_: Exception) {
                                                         ByteArray(tileSize)
                                                     }
                                                   }
        }
            }


    private fun readTile(path: Path)                     = fs.read(path) { readByteArray() }
    private fun writeTile(path: Path, bytes: ByteArray)  = fs.write(path) { write(bytes) }

    private fun tilePath(cacheKey: String) : Path {
        val parts = cacheKey.split("/")
        val dir = cacheDir / parts[0] / parts[1]
        if (!fs.exists(dir)) {
            fs.createDirectories(dir)
        }
        return dir / "${parts[1]}.png"
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

}

fun platformCacheDir(): Path {
    val userHome = System.getProperty("user.home")

    return "$userHome/.tilecache".toPath()
}

fun createHttpClient(): HttpClient = HttpClient(Apache5) {
    engine {
        connectTimeout           = 3_000 // 3 seconds
        socketTimeout            = 3_000
        connectionRequestTimeout = 3_000

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
        requestTimeoutMillis = 5000
        connectTimeoutMillis = 5000
        socketTimeoutMillis  = 5000
    }

    // Add default request configuration
    install(DefaultRequest) {
        // OSM services require a User-Agent header
        headers.append("User-Agent", "PoiCh/1.0 (Desktop Application)")
        headers.append("Accept", "*/*")
    }
}