package org.librecans.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.material3.SliderState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * An integer slider that keeps its own value while dragged, reports intermediate values through
 * [onValueChange] and the final one through [onValueChangeFinished]. Device echoes that arrive
 * mid-drag therefore cannot make the thumb jump.
 */
@Composable
fun IntSlider(
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    onValueChangeFinished: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String? = null,
) {
    val state = remember(range) {
        SliderState(
            value = value.toFloat(),
            steps = (range.last - range.first - 1).coerceAtLeast(0),
            trackRange = range.first.toFloat()..range.last.toFloat(),
        )
    }
    var dragging by remember { mutableStateOf(false) }
    if (!dragging && state.value.roundToInt() != value) state.value = value.toFloat()
    Slider(
        state = state,
        onValueChange = {
            dragging = true
            val previous = state.value.roundToInt()
            state.value = it
            if (it.roundToInt() != previous) onValueChange(it.roundToInt())
        },
        onValueChangeFinished = {
            dragging = false
            onValueChangeFinished(state.value.roundToInt())
        },
        enabled = enabled,
        modifier = modifier.semantics { label?.let { contentDescription = it } },
        track = { SliderDefaults.Track(sliderState = it, enabled = enabled, modifier = Modifier.height(16.dp), drawTick = { _, _ -> }) },
    )
}

/** A slider row inside a segmented group, with a leading icon and the current value. */
@Composable
fun SliderItem(
    title: String,
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    onValueChangeFinished: (Int) -> Unit,
    segment: Segment,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null,
    valueText: (Int) -> String = { it.toString() },
    enabled: Boolean = true,
) {
    Surface(
        shape = segment.shapes().shape,
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                icon?.let {
                    Icon(painterResource(it), null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(16.dp))
                }
                Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Text(valueText(value), style = MaterialTheme.typography.labelLargeEmphasized, color = MaterialTheme.colorScheme.primary)
            }
            IntSlider(
                value = value,
                range = range,
                onValueChange = onValueChange,
                onValueChangeFinished = onValueChangeFinished,
                enabled = enabled,
                label = title,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            )
        }
    }
}
