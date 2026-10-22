package org.librecans.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.toPath
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon

private const val TURN_DEGREES = 90f
private const val SPIN_MILLIS = 30_000

/**
 * A slowly spinning Material shape that morphs and turns with a spring whenever [shape] changes,
 * e.g. to reflect the current noise control mode behind the device artwork. The path is only
 * rebuilt while morphing; the spin is read in the draw phase alone, so it never recomposes,
 * relayouts or, unlike a rotating graphics layer, floods accessibility with change events.
 */
@Composable
fun MorphingShape(
    shape: RoundedPolygon,
    color: Color,
    modifier: Modifier = Modifier,
) {
    var from by remember { mutableStateOf(shape) }
    var to by remember { mutableStateOf(shape) }
    var turns by remember { mutableIntStateOf(0) }
    val progress = remember { Animatable(1f) }
    val spec = MaterialTheme.motionScheme.slowSpatialSpec<Float>()
    LaunchedEffect(shape) {
        if (shape == to) return@LaunchedEffect
        from = to
        to = shape
        turns++
        progress.snapTo(0f)
        progress.animateTo(1f, spec)
    }
    val morph = remember(from, to) { Morph(from, to) }
    val path = remember { Path() }
    val spin = rememberInfiniteTransition(label = "spin").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(SPIN_MILLIS, easing = LinearEasing)),
        label = "spin",
    )
    Spacer(
        modifier.drawWithCache {
            val value = progress.value
            path.rewind()
            morph.toPath(value, path)
            path.transform(Matrix().apply { scale(size.width, size.height) })
            path.translate(size.center - path.getBounds().center)
            val angle = (turns - 1 + value) * TURN_DEGREES
            onDrawBehind { rotate(angle + spin.value) { drawPath(path, color) } }
        },
    )
}
