@file:OptIn(DelicateCoroutinesApi::class) @file:Suppress("ConvertToStringTemplate", "LocalVariableName", "FunctionName", "SpellCheckingInspection", "LiftReturnOrAssignment", "RedundantIf", "PropertyName")

package ch.fhnw.osmdemo.viewmodel

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.SnapSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import geostore
import io.ktor.utils.io.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.processNextEventInCurrentThread
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
import ovh.plrapps.mapcompose.api.visibleBoundingBox
import ovh.plrapps.mapcompose.core.TileStreamProvider
import ovh.plrapps.mapcompose.demo.viewmodels.INITIAL_ZOOM_LEVEL
import ovh.plrapps.mapcompose.demo.viewmodels.OsmVM
import ovh.plrapps.mapcompose.ui.layout.Forced
import ovh.plrapps.mapcompose.ui.state.MapState
import randomDebugBorder
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.pow

const val HOTSPOT_MARKER_ID_ADDON = "x_marks_the_spot_eeJ3heeg"
const val ACCURACY_MARKER_ID_ADDON = "y_acc_aeYaiqu5_Daey9ei5"

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
                                 destScale     = 1 / (2.0.pow(18 - INITIAL_ZOOM_LEVEL)),
                                 animationSpec = SnapSpec())
        }
        // state.removeMarker(ST_STEPHEN_MARKER_ID)
    }

    fun addMarker1(id: String, geoPos: GeoPosition) = addMarker(id, 0.0f,
        true, geoPos.asNormalizedWebMercator(), name = id, last_location_millis = -1,
        accuracy = 0.0f, provider = "unknown")

    fun addMarker3(
        id: String, bearing: Float, has_bearing: Boolean, geoPos: GeoPosition, name: String,
        last_location_millis: Long, accuracy: Float,
        provider: String
    ) =
        addMarker(id, bearing, has_bearing, geoPos.asNormalizedWebMercator(), name,
            last_location_millis, true, accuracy,
            provider = provider)

    fun addMarker(pk_string: String, bearing: Float, has_bearing: Boolean,
                  point : NormalizedPoint, name: String, last_location_millis: Long = -1L,
                  hotspot: Boolean = false, accuracy: Float, provider: String,) {
        viewModelScope.launch {
            val is_pinned: Boolean
            if ((!pk_string.isNullOrEmpty()) && (geostore.getFollowPk().equals(pk_string)))
            {
                is_pinned = true
            }
            else
            {
                is_pinned = false
            }

            var name_ = name
            if (!name.isNullOrEmpty())
            {
            }
            else
            {
                name_ = "???"
            }

            name_ = name_ + " (" + provider + ")"

            val has_delta_time: Boolean
            if (last_location_millis > -1L)
            {
                has_delta_time = true
            }
            else
            {
                has_delta_time = false
            }
            var offset_y_bearing = -0.76f
            var offset_y_no_bearing = -0.93f
            if (is_pinned)
            {
                offset_y_bearing = -0.81f
                offset_y_no_bearing = -0.946f
            }
            if (hotspot)
            {
                state.addMarker(pk_string + ACCURACY_MARKER_ID_ADDON,
                    point.x, point.y,
                    zIndex = if (is_pinned) 10f else 2f,
                    relativeOffset = Offset(-0.5f, -0.5f)) {

                    val size = 18.0 * accuracy * state.scale // TODO: calculate the actual correct size !!
                    Box(
                        modifier = Modifier
                            .clip(RectangleShape) // clip the content to container bounds
                    ) {
                        if (!((size < 1) || (size > 5000)))
                        {
                            Canvas(modifier = Modifier.size(size.dp)) {
                                drawCircle(color = Color.Blue.copy(alpha = 0.14f), style = Fill)
                                drawCircle(color = Color.Blue.copy(alpha = 0.45f), style = Stroke(width = 1.dp.toPx()))
                            }
                        }
                    }
                }
            }
            state.disableMarkerDrag(pk_string + ACCURACY_MARKER_ID_ADDON)
            state.addMarker(pk_string, point.x, point.y,
                zIndex = if (is_pinned) 10f else 2f,
                relativeOffset = if (has_bearing) Offset(-0.5f, offset_y_bearing) else
                    Offset(-0.5f, offset_y_no_bearing)) {
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

                    if (is_pinned)
                    {
                        Icon(imageVector       = Icons.Filled.PinDrop,
                            contentDescription = "follow me",
                            modifier           = Modifier.size(30.dp)
                                .align(Alignment.CenterHorizontally),
                            tint               = pin_and_text_color
                        )
                    }
                    Text(text = "" + name_,
                        modifier = Modifier
                            .clip(RoundedCornerShape((7.dp)))
                            .background(pin_and_text_color)
                            .align(Alignment.CenterHorizontally)
                            .padding(4.dp)
                        ,
                        fontSize = 18.sp,
                        color = if (is_pinned) Color.Black.copy(alpha = 1.0f) else Color.Black.copy(alpha = 0.6f)
                    )

                    if (has_delta_time)
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
                            color = if (is_pinned) Color.Black.copy(alpha = 1.0f) else Color.Black.copy(alpha = 0.6f)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // 1. Prepare your icon and painter
                    val icon = if (has_bearing) Icons.Filled.Navigation else Icons.Filled.LocationOn
                    val iconPainter = rememberVectorPainter(icon)
                    val iconSize = 50.dp

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(iconSize + 5.dp)
                            .randomDebugBorder()
                            .align(Alignment.CenterHorizontally)
                    ) {
                        val rotation = if (has_bearing) bearing else 0.0f

                        // 2. Background "Shadow" Icon (The solid border/outline)
                        // We draw this slightly larger and blur it to fill all "holes"
                        Icon(
                            painter = iconPainter,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier
                                .size(iconSize)
                                .graphicsLayer {
                                    rotationZ = rotation
                                    // Apply a small blur to act as a solid outline/shadow
                                    // This eliminates the "holes" caused by vector path gaps
                                    renderEffect = BlurEffect(2f, 2f, TileMode.Decal)
                                    // Slightly scale up to ensure it peeks out from behind
                                    scaleX = 1.1f
                                    scaleY = 1.1f
                                }
                        )

                        // 3. Main Foreground Icon
                        Icon(
                            painter = iconPainter,
                            contentDescription = pk_string,
                            tint = pin_and_text_color,
                            modifier = Modifier
                                .size(iconSize)
                                .graphicsLayer { rotationZ = rotation }
                        )
                    }

                }
            }
            state.disableMarkerDrag(pk_string)
            if (hotspot)
            {
                state.addMarker(pk_string + HOTSPOT_MARKER_ID_ADDON, point.x, point.y,
                    zIndex = 20f, relativeOffset = Offset(-0.5f, -0.5f)) {
                    SmallFilledRedCircle()
                }
                state.disableMarkerDrag(pk_string + HOTSPOT_MARKER_ID_ADDON)
            }
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
        } else if (seconds > 1) // to avoid flickering between "now" and "1 seconds ago"
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

@Composable
fun SmallFilledRedCircle()
{
    // The Canvas composable acts as a drawing area.
    // The size modifier controls the total area this composable occupies in the layout.
    Canvas(modifier = Modifier.size(8.dp) // Make the Canvas area small, e.g., 40x40 dp
        .padding(1.dp) // Add some padding around the circle if needed
    ) {
        // Inside the DrawScope, 'center' refers to the center of the Canvas area,
        // and 'size.minDimension / 2f' calculates the maximum possible radius that fits.
        // You can specify an explicit radius for a fixed size circle relative to the Canvas size.
        drawCircle(color = Color.Red, // Set the fill color to Red
            radius = size.minDimension / 2f, // Fills the available canvas size
            center = center // Draws the circle at the center of the Canvas
        )
    }
}
