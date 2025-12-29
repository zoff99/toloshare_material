@file:OptIn(DelicateCoroutinesApi::class) @file:Suppress("ConvertToStringTemplate", "LocalVariableName", "FunctionName")

package ch.fhnw.osmdemo.viewmodel

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.SnapSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.fhnw.osmdemo.view.Callout
import com.zoffcc.applications.trifa.Log
import com.zoffcc.applications.trifa.TAG
import io.ktor.utils.io.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.launch
import ovh.plrapps.mapcompose.api.addCallout
import ovh.plrapps.mapcompose.api.addLayer
import ovh.plrapps.mapcompose.api.addMarker
import ovh.plrapps.mapcompose.api.centerOnMarker
import ovh.plrapps.mapcompose.api.centroidX
import ovh.plrapps.mapcompose.api.centroidY
import ovh.plrapps.mapcompose.api.disableMarkerDrag
import ovh.plrapps.mapcompose.api.moveMarker
import ovh.plrapps.mapcompose.api.onCalloutClick
import ovh.plrapps.mapcompose.api.onLongPress
import ovh.plrapps.mapcompose.api.onMarkerClick
import ovh.plrapps.mapcompose.api.onMarkerLongPress
import ovh.plrapps.mapcompose.api.onMarkerMove
import ovh.plrapps.mapcompose.api.onTap
import ovh.plrapps.mapcompose.api.removeCallout
import ovh.plrapps.mapcompose.api.scale
import ovh.plrapps.mapcompose.api.scrollTo
import ovh.plrapps.mapcompose.api.setScrollOffsetRatio
import ovh.plrapps.mapcompose.api.visibleArea
import ovh.plrapps.mapcompose.core.TileStreamProvider
import ovh.plrapps.mapcompose.ui.layout.Forced
import ovh.plrapps.mapcompose.ui.state.MapState
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlin.math.pow

/**
 * Shows how to use WMTS tile servers with MapCompose, such as OpenStreetMap.
 */
class OsmViewModel : ViewModel(){
    private val tileLoader = CachingOsmTileLoader()

    private val tileStreamProvider = TileStreamProvider { row, col, zoomLvl ->
                                                           tileLoader.loadTile(row, col, zoomLvl).asRawSource()
                                                        }

    private val tapToDismissId = "Tap me to dismiss"
    private val markerColor = Color(0xCC2196F3)

    private val maxLevel = 19
    private val minLevel = 2
    private val tileSize = 256
    private val mapSize  = mapSizeAtLevel(wmtsLevel = maxLevel, tileSize = tileSize)

    private var markerCount = 0

    val state = MapState(levelCount  = maxLevel + 1,
                         fullWidth   = mapSize,
                         fullHeight  = mapSize,
                         workerCount = 4) {
        minimumScaleMode(Forced(1 / 2.0.pow(maxLevel - minLevel))) }
                             .apply {
          addLayer(tileStreamProvider)

          onMarkerMove { id, x, y, _, _ ->
              // println("move $id $x $y")
          }

          /**
           * On Marker click, add a callout. If the id is [tapToDismissId], set auto-dismiss
           * to false. For this particular id, we programmatically remove the callout on tap.
           */
          onMarkerClick { id, x, y ->
              var shouldAnimate by mutableStateOf(true)
              addCallout(id             = id,
                         x              = x,
                         y              = y,
                         absoluteOffset = DpOffset(0.dp, (-50).dp),
                         autoDismiss    = id != tapToDismissId,
                         clickable      = id == tapToDismissId) {

                  Callout(point         = NormalizedPoint(x, y),
                          title         = id,
                          shouldAnimate = shouldAnimate) {
                      shouldAnimate = false
                  }
              }
          }

          /**
           * Register a click listener on callouts. We don't need to remove the other callouts
           * because they automatically dismiss on tap.
           */
          onCalloutClick { id, _, _ ->
              if (id == tapToDismissId) removeCallout(tapToDismissId)
          }

          onMarkerLongPress { id, x, y ->
              // println("on marker long press $id $x $y")
              // removeMarker(id)
          }

          onTap { x, y ->
              // println("on tap $x $y")
          }

          onLongPress { x, y ->
              // println("on long press $x $y")
          }

         // enableRotation()
          setScrollOffsetRatio(0.5f, 0.5f)
    }

    init {
        addMarker(ST_STEPHEN_MARKER_ID, ST_STEPHEN_GEOPOS)
        viewModelScope.launch {
            state.centerOnMarker(id            = ST_STEPHEN_MARKER_ID,
                                 destScale     = 0.1,
                                 animationSpec = SnapSpec())
        }
        // state.removeMarker(ST_STEPHEN_MARKER_ID)
    }

    fun addMarker(id: String, geoPos: GeoPosition) = addMarker(id, geoPos.asNormalizedWebMercator())
    fun addMarker(id: String, geoPos: GeoPosition, name: String) = addMarker(id, geoPos.asNormalizedWebMercator(), name)
    fun addMarker(id: String, geoPos: GeoPosition, name: String, last_location_millis: Long) = addMarker(id, geoPos.asNormalizedWebMercator(), name, last_location_millis)

    fun moveMarker(id: String, geoPos : GeoPosition) = moveMarker(id, geoPos.asNormalizedWebMercator())

    fun addMarker(id: String, point : NormalizedPoint) {
        addMarker(id, point, "")
    }

    fun addMarker(id: String, point : NormalizedPoint, name: String, last_location_millis: Long = -1L) {
        viewModelScope.launch {
            state.addMarker(id, point.x, point.y) {
                Column {
                    var pin_and_text_color: Color = markerColor
                    val age_millis = location_age_millis(last_location_millis)
                    if (last_location_millis > -1L)
                    {
                        if (age_millis > 2 * 60 * 1000L)
                        {
                            // HINT: if location is older than 2 minutes, make pin and text color red-ish
                            pin_and_text_color = Color.Red
                        }
                    }

                    if (!name.isNullOrEmpty())
                    {
                        Text(text = "" + name,
                            modifier = Modifier
                                .clip(RoundedCornerShape((7.dp)))
                                .background(pin_and_text_color)
                                .align(Alignment.CenterHorizontally)
                                .padding(4.dp)
                            ,
                            fontSize = 18.sp,
                            color = Color.Black)
                    }
                    if (last_location_millis > -1L)
                    {
                        Text(text = "" + location_age_text(last_location_millis),
                            modifier = Modifier
                                .clip(RoundedCornerShape((4.dp)))
                                .background(pin_and_text_color)
                                .align(Alignment.CenterHorizontally)
                                .padding(4.dp)
                            ,
                            fontSize = 15.sp,
                            color = Color.Black)
                    }
                    Icon(imageVector       = Icons.Filled.LocationOn,
                        contentDescription = id,
                        modifier           = Modifier.size(50.dp)
                            .align(Alignment.CenterHorizontally),
                        tint               = pin_and_text_color
                    )
                }
            }
            state.disableMarkerDrag(id)
            markerCount++
        }
    }

    fun moveMarker(id: String, point : NormalizedPoint){
        viewModelScope.launch {
            state.moveMarker(id, point.x, point.y)
        }
    }

    fun addMarkerInCenter() {
        viewModelScope.launch {
            val area = state.visibleArea()
            val centerX = area.p1x + ((area.p2x - area.p1x) * 0.5)
            val centerY = area.p1y + ((area.p4y - area.p1y) * 0.5)
            addMarker("marker$markerCount", NormalizedPoint(centerX, centerY))
        }
    }

    fun zoomIn() =
        viewModelScope.launch {
            state.scrollTo(x             = state.centroidX,
                           y             = state.centroidY,
                           destScale     = state.scale * 1.5f,
                           animationSpec = TweenSpec(durationMillis = 5,
                                                     easing         = FastOutSlowInEasing))
    }

    fun zoomOut() =
        viewModelScope.launch {
            state.scrollTo(x             = state.centroidX,
                           y             = state.centroidY,
                           destScale     = state.scale / 1.5f,
                           animationSpec = TweenSpec(durationMillis = 5,
                                                     easing         = FastOutSlowInEasing))
    }

    /**
     * WMTS levels are 0-based. At level 0, the map corresponds to just one tile.
     */
    private fun mapSizeAtLevel(wmtsLevel: Int, tileSize: Int): Int = tileSize * 2.0.pow(wmtsLevel).toInt()

    private fun ByteArray.asRawSource() = ByteReadChannel(this).asSource()

}

fun location_age_millis(timestamp_millis: Long): Long
{
    val current_ts_millis = System.currentTimeMillis()
    if (timestamp_millis > -1L)
    {
        val diff_millis: Long = Date(current_ts_millis).time - Date(timestamp_millis).time
        // val seconds = TimeUnit.MILLISECONDS.toSeconds(diff)
        return diff_millis
    }
    // HINT: No age yet, just give -1
    return -1L
}

fun location_age_text(timestamp_millis: Long): String
{
    val current_ts_millis = System.currentTimeMillis()
    var location_time_txt = "???"

    if (timestamp_millis > -1)
    {
        val diff: Long = Date(current_ts_millis).time - Date(timestamp_millis).time
        val seconds = TimeUnit.MILLISECONDS.toSeconds(diff)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)

        if (minutes > 0)
        {
            location_time_txt = "" + minutes + " minutes ago"
        } else if (seconds > 0)
        {
            location_time_txt = "" + seconds + " seconds ago"
        } else
        {
            location_time_txt = "" + "now"
        }
        return location_time_txt
    }
    // HINT: No age yet, just give empty String
    return ""
}

