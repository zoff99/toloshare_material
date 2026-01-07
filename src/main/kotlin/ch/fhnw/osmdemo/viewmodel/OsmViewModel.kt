@file:OptIn(DelicateCoroutinesApi::class) @file:Suppress("ConvertToStringTemplate", "LocalVariableName", "FunctionName", "SpellCheckingInspection", "LiftReturnOrAssignment")

package ch.fhnw.osmdemo.viewmodel

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.SnapSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zoffcc.applications.trifa.Log
import com.zoffcc.applications.trifa.TAG
import geostore
import io.ktor.utils.io.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.launch
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
import ovh.plrapps.mapcompose.api.visibleBoundingBox
import ovh.plrapps.mapcompose.core.TileStreamProvider
import ovh.plrapps.mapcompose.ui.layout.Forced
import ovh.plrapps.mapcompose.ui.state.MapState
import randomDebugBorder
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.pow

/**
 * Shows how to use WMTS tile servers with MapCompose, such as OpenStreetMap.
 */
class OsmViewModel : ViewModel(){
    private val tileLoader = CachingOsmTileLoader()

    private val tileStreamProvider = TileStreamProvider { row, col, zoomLvl ->
                                                           // Log.i(TAG, "TileStreamProvider: " + row + " " + col + " " + zoomLvl)
                                                           tileLoader.loadTile(row, col, zoomLvl).asRawSource()
                                                        }

    private val tapToDismissId = "Tap me to dismiss"
    private val markerColor = Color(0xCC2196F3)

    private val maxLevel = 21
    private val minLevel = 2
    private val tileSize = 256
    private val mapSize  = mapSizeAtLevel(wmtsLevel = maxLevel, tileSize = tileSize)

    private var markerCount = 0

    val state = MapState(levelCount  = maxLevel + 1,
                         fullWidth   = mapSize,
                         fullHeight  = mapSize,
                         workerCount = 10) {
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
          onMarkerClick { pk_string, x, y ->
              var shouldAnimate by mutableStateOf(true)
              if (geostore.getFollowPk().equals(pk_string))
              {
                  // on second click reset "follow me" to "null" (do not follow anyone)
                  geostore.setFollowPk(null)
              }
              else
              {
                  geostore.setFollowPk(pk_string)
              }
              /*
              addCallout(id             = pk_string,
                         x              = x,
                         y              = y,
                         absoluteOffset = DpOffset(0.dp, (-50).dp),
                         autoDismiss    = pk_string != tapToDismissId,
                         clickable      = pk_string == tapToDismissId) {

                  Callout(point         = NormalizedPoint(x, y),
                          title         = pk_string,
                          shouldAnimate = shouldAnimate) {
                      shouldAnimate = false
                  }
              }
             */
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
        addMarker1(ST_STEPHEN_MARKER_ID, ST_STEPHEN_GEOPOS)
        viewModelScope.launch {
            state.centerOnMarker(id            = ST_STEPHEN_MARKER_ID,
                                 destScale     = 0.1,
                                 animationSpec = SnapSpec())
        }
        // state.removeMarker(ST_STEPHEN_MARKER_ID)
    }

    fun moveMarker(id: String, geoPos : GeoPosition) = moveMarker(id, geoPos.asNormalizedWebMercator())

    fun addMarker1(id: String, geoPos: GeoPosition) = addMarker(id, 0.0f,
        true, geoPos.asNormalizedWebMercator(), name = id, last_location_millis = -1)

    fun addMarker2(id: String, geoPos: GeoPosition, name: String) =
        addMarker(id, bearing = 0.0f, has_bearing = false,geoPos.asNormalizedWebMercator(), name)

    fun addMarker3(id: String, bearing: Float, has_bearing: Boolean, geoPos: GeoPosition, name: String, last_location_millis: Long) =
        addMarker(id, bearing, has_bearing, geoPos.asNormalizedWebMercator(), name, last_location_millis)

    fun addMarker(pk_string: String, bearing: Float, has_bearing: Boolean, point : NormalizedPoint, name: String, last_location_millis: Long = -1L) {
        viewModelScope.launch {
            state.addMarker(pk_string, point.x, point.y,
                relativeOffset = if (has_bearing) Offset(-0.5f, -0.76f) else Offset(-0.5f, -0.93f)) {
                Column(modifier = Modifier.randomDebugBorder()) {
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

                    if ((!pk_string.isNullOrEmpty()) && (geostore.getFollowPk().equals(pk_string)))
                    {
                        Icon(imageVector       = Icons.Filled.PinDrop,
                            contentDescription = "follow me",
                            modifier           = Modifier.size(30.dp)
                                .align(Alignment.CenterHorizontally),
                            tint               = pin_and_text_color
                        )
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
                        Spacer(modifier = Modifier.height(2.dp))
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

                    var icon = if (has_bearing) {
                        Icons.Filled.Navigation // Use appropriate navigation arrow icon
                    } else {
                        Icons.Filled.LocationOn // Use location icon
                    }
                    Box(contentAlignment = Alignment.Center) {
                        val iconSize = 50.dp
                        val outlineWidth = 2.dp // Adjust for border thickness

                        // Render the "Border" by placing offset versions behind the main icon
                        listOf(
                            Offset(-1f, -1f), Offset(1f, -1f),
                            Offset(-1f, 1f), Offset(1f, 1f)
                        ).forEach { offset ->
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = Color.Black, // Border color
                                modifier = Modifier
                                    .size(iconSize)
                                    .offset(x = offset.x.dp, y = offset.y.dp) // Manual stroke simulation
                                    .graphicsLayer(rotationZ = if (has_bearing) bearing else 0.0f)
                            )
                        }

                        // Main Icon
                        Icon(
                            imageVector = icon,
                            contentDescription = pk_string,
                            modifier = Modifier
                                .size(iconSize)
                                .graphicsLayer(rotationZ = if (has_bearing) bearing else 0.0f),
                            tint = pin_and_text_color
                        )
                    }
                }
            }
            state.disableMarkerDrag(pk_string)
            markerCount++
        }
    }

    fun moveMarker(id: String, point : NormalizedPoint){
        viewModelScope.launch {
            state.moveMarker(id, point.x, point.y)
        }
    }

    fun zoomIn() =
        viewModelScope.launch {
            state.scrollTo(x             = state.centroidX,
                y             = state.centroidY,
                destScale     = state.scale * 1.5f,
                animationSpec = TweenSpec(durationMillis = 0,
                easing         = FastOutLinearInEasing))
        }

    fun zoomOut() =
        viewModelScope.launch {
            state.scrollTo(x             = state.centroidX,
                y             = state.centroidY,
                destScale     = state.scale / 1.5f,
                animationSpec = TweenSpec(durationMillis = 0,
                easing         = FastOutLinearInEasing))
        }

    /*
     * Zoom in to the given center coordinates but do NOT move the map center to them
     * this is more like the expected behaviour
     */
    fun zoomIn(center_x: Double, center_y: Double) =
        viewModelScope.launch {
            var bbox = state.visibleBoundingBox()
            val new_center_x = bbox.xLeft + (center_x * (bbox.xRight - bbox.xLeft))
            val new_center_y = bbox.yTop + (center_y * (bbox.yBottom - bbox.yTop))

            state.scrollTo(x             = new_center_x,
                y             = new_center_y,
                destScale     = state.scale * 1.5f,
                screenOffset = Offset((-center_x).toFloat(), (-center_y).toFloat()),
                animationSpec = TweenSpec(durationMillis = 0,
                easing         = FastOutLinearInEasing))
        }

    /*
     * Zoom out to the given center coordinates but do NOT move the map center to them
     * this is more like the expected behaviour
     */
    fun zoomOut(center_x: Double, center_y: Double) =
        viewModelScope.launch {
            var bbox = state.visibleBoundingBox()
            val new_center_x = bbox.xLeft + (center_x * (bbox.xRight - bbox.xLeft))
            val new_center_y = bbox.yTop + (center_y * (bbox.yBottom - bbox.yTop))

            state.scrollTo(x             = new_center_x,
                y             = new_center_y,
                destScale     = state.scale / 1.5f,
                screenOffset = Offset((-center_x).toFloat(), (-center_y).toFloat()),
                animationSpec = TweenSpec(durationMillis = 0,
                easing         = FastOutLinearInEasing))
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

