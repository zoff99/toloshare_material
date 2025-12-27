package ovh.plrapps.mapcompose.demo.viewmodels

import cafe.adriel.voyager.core.model.ScreenModel
import kotlinx.io.asSource
import ovh.plrapps.mapcompose.api.addLayer
import ovh.plrapps.mapcompose.api.scale
import ovh.plrapps.mapcompose.api.shouldLoopScale
import ovh.plrapps.mapcompose.core.TileStreamProvider
import ovh.plrapps.mapcompose.ui.layout.Forced
import ovh.plrapps.mapcompose.ui.state.MapState
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.pow

/**
 * Shows how to use WMTS tile servers with MapCompose, such as Open Street Map.
 */
class OsmVM : ScreenModel {
    private val tileStreamProvider = makeOsmTileStreamProvider()

    private val maxLevel = 16
    private val minLevel = 1
    private val mapSize = mapSizeAtLevel(maxLevel, tileSize = 256)
    val state = MapState(levelCount = maxLevel + 1, mapSize, mapSize, workerCount = 10) {
        minimumScaleMode(Forced(1 / 2.0.pow(maxLevel - minLevel)))
        scroll(0.5064745545387268, 0.3440358340740204)
        // 48.2085, 16.3730 // St. Stephan
    }.apply {
        addLayer(tileStreamProvider)
        shouldLoopScale = false
        scale = 5.0  // initial zoom level
    }
}

/**
 * wmts level are 0 based.
 * At level 0, the map corresponds to just one tile.
 */
private fun mapSizeAtLevel(wmtsLevel: Int, tileSize: Int): Int {
    return tileSize * 2.0.pow(wmtsLevel).toInt()
}

/**
 * A [TileStreamProvider] which queries OSM server.
 */
fun makeOsmTileStreamProvider() : TileStreamProvider {
    return TileStreamProvider { row, col, zoomLvl ->
        try {
            val url = URL("https://tile.openstreetmap.org/$zoomLvl/$col/$row.png")
            val connection = url.openConnection() as HttpURLConnection
            // OSM requires a user-agent
            connection.setRequestProperty("User-Agent", "com.zoffcc.applications.toloshare_material")
            connection.doInput = true
            connection.connect()
            connection.inputStream.asSource()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}