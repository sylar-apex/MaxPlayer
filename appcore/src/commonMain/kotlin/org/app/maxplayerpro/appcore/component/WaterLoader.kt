package org.app.maxplayerpro.appcore.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * A round loader that fills with water as [progress] (0..1) goes up, the percent written inside. Two soft white
 * waves on a dark glass disc, so it sits well on any photo.
 */
@Composable
fun WaterLoader(progress: Float, modifier: Modifier = Modifier, size: Dp = 60.dp) {
    val level by animateFloatAsState(progress.coerceIn(0f, 1f), tween(300), label = "level")
    val phase by rememberInfiniteTransition(label = "wave").animateFloat(
        0f, (2 * PI).toFloat(), infiniteRepeatable(tween(1600, easing = LinearEasing)), label = "phase",
    )
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val r = this.size.minDimension / 2
            val w = this.size.width
            val h = this.size.height
            drawCircle(Color.Black.copy(alpha = 0.4f), r)
            val circle = Path().apply { addOval(androidx.compose.ui.geometry.Rect(center, r)) }
            clipPath(circle) {
                // A level just above 0 still shows a sliver of water; a full one has a flat top.
                val top = h * (1f - level)
                val amp = if (level < 0.995f) r * 0.07f else 0f
                fun wave(shift: Float, cycles: Float) = Path().apply {
                    moveTo(0f, h)
                    var x = 0f
                    while (x <= w) {
                        lineTo(x, top + amp * sin(x / w * 2 * PI.toFloat() * cycles + phase + shift))
                        x += 3f
                    }
                    lineTo(w, h)
                    close()
                }
                drawPath(wave(PI.toFloat(), 1.2f), Color.White.copy(alpha = 0.18f))
                drawPath(wave(0f, 1f), Color.White.copy(alpha = 0.30f))
            }
            drawCircle(Color.White.copy(alpha = 0.65f), r - 1.dp.toPx(), style = Stroke(1.5.dp.toPx()))
        }
        Text("${(level * 100).roundToInt()}%", color = Color.White, fontSize = (size.value / 4.6f).sp, fontWeight = FontWeight.SemiBold,
            style = androidx.compose.ui.text.TextStyle(shadow = androidx.compose.ui.graphics.Shadow(Color.Black.copy(alpha = 0.5f), Offset(0f, 1f), 3f)))
    }
}
