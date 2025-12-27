package ch.fhnw.osmdemo.view

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.fhnw.osmdemo.viewmodel.NormalizedPoint


/**
 * A callout which animates its entry with an overshoot scaling interpolator.
 */
@Composable
fun Callout(point :          NormalizedPoint,
            title:           String,
            shouldAnimate:   Boolean,
            onAnimationDone: () -> Unit) {
    var animVal by remember { mutableStateOf(if (shouldAnimate) 0f else 1f) }

    LaunchedEffect(true) {
        if (shouldAnimate) {
            Animatable(0f).animateTo(targetValue   = 1f,
                                     animationSpec = tween(250)) {
                animVal = value
            }
            onAnimationDone()
        }
    }
    Surface(modifier        = Modifier.alpha(animVal)
                                      .padding(10.dp)
                                      .graphicsLayer {scaleX          = animVal
                                                      scaleY          = animVal
                                                      transformOrigin = TransformOrigin(0.5f, 1f) },
            shape           = RoundedCornerShape(5.dp),
            tonalElevation  = 10.dp,
            shadowElevation = 10.dp) {

        Column(modifier = Modifier.padding(16.dp)) {
            Text(text       = title,
                 modifier   = Modifier.align(alignment = Alignment.CenterHorizontally),
                 fontSize   = 12.sp,
                 textAlign  = TextAlign.Center,
                 fontWeight = FontWeight.Bold,
                 color      = Color.Black)

            Text(text      = point.asGeoPosition().dms(),
                 modifier  = Modifier.align(alignment = Alignment.CenterHorizontally)
                                     .padding(top = 4.dp),
                 fontSize  = 10.sp,
                 textAlign = TextAlign.Center,
                 color     = Color.Black)
        }
    }
}

