package org.librecans.app.ui.equalizer

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/** A smooth response curve through the band levels, animated with spatial springs. */
@Composable
fun EqCurve(levels: List<Int>, range: IntRange, modifier: Modifier = Modifier) {
    val spec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    val animated: List<State<Float>> = levels.map { animateFloatAsState(it.toFloat(), spec, label = "band") }
    val line = MaterialTheme.colorScheme.primary
    val fill = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
    val grid = MaterialTheme.colorScheme.outlineVariant
    val span = (range.last - range.first).coerceAtLeast(1).toFloat()

    Canvas(modifier) {
        if (animated.isEmpty()) return@Canvas
        val stroke = 4.dp.toPx()
        val inset = stroke
        val height = size.height - inset * 2
        fun y(value: Float) = inset + height * (1f - (value - range.first) / span)
        val step = size.width / (animated.size - 1).coerceAtLeast(1)
        val points = animated.mapIndexed { i, v -> Offset(i * step, y(v.value)) }

        drawLine(grid, Offset(0f, y(0f)), Offset(size.width, y(0f)), strokeWidth = 1.dp.toPx())

        val curve = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) {
                val a = points[i - 1]
                val b = points[i]
                val mid = (a.x + b.x) / 2f
                cubicTo(mid, a.y, mid, b.y, b.x, b.y)
            }
        }
        val area = Path().apply {
            addPath(curve)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(area, Brush.verticalGradient(listOf(fill, fill.copy(alpha = 0f))))
        drawPath(curve, line, style = Stroke(width = stroke, cap = StrokeCap.Round))
        points.forEach { drawCircle(line, radius = stroke * 1.25f, center = it) }
    }
}
