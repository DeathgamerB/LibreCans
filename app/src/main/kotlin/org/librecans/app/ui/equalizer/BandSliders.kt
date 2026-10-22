package org.librecans.app.ui.equalizer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalSlider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import org.librecans.app.R
import org.librecans.app.ui.common.rememberHaptics

private val FIVE_BANDS = listOf("400", "1k", "2.5k", "6.3k", "16k")
private val TEN_BANDS = listOf("31", "63", "125", "250", "500", "1k", "2k", "4k", "8k", "16k")

fun bandLabels(count: Int): List<String> = when (count) {
    5 -> FIVE_BANDS
    10 -> TEN_BANDS
    else -> List(count) { "${it + 1}" }
}

@Composable
fun BandSliders(
    levels: List<Int>,
    range: IntRange,
    onBandChange: (index: Int, level: Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val labels = bandLabels(levels.size)
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        levels.forEachIndexed { index, level ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (level > 0) "+$level" else "$level",
                    style = MaterialTheme.typography.labelLargeEmphasized,
                    color = MaterialTheme.colorScheme.primary,
                )
                BandSlider(
                    value = level,
                    range = range,
                    enabled = enabled,
                    onValueChange = { onBandChange(index, it) },
                    label = stringResource(R.string.eq_band_content_description, labels[index]),
                    modifier = Modifier.height(220.dp).width(48.dp).testTag("band_$index"),
                )
                Text(labels[index], style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** A vertical integer slider that ignores device echoes while it is being dragged. */
@Composable
private fun BandSlider(
    value: Int,
    range: IntRange,
    enabled: Boolean,
    onValueChange: (Int) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    val state = remember(range) {
        SliderState(value = value.toFloat(), steps = range.last - range.first - 1, trackRange = range.first.toFloat()..range.last.toFloat())
    }
    var dragging by remember { mutableStateOf(false) }
    val haptics = rememberHaptics()
    if (!dragging && state.value.roundToInt() != value) state.value = value.toFloat()
    VerticalSlider(
        state = state,
        onValueChange = {
            dragging = true
            val previous = state.value.roundToInt()
            state.value = it
            if (it.roundToInt() != previous) {
                haptics.step()
                onValueChange(it.roundToInt())
            }
        },
        onValueChangeFinished = {
            dragging = false
            haptics.release()
        },
        topToBottom = false,
        enabled = enabled,
        track = { SliderDefaults.Track(sliderState = it, enabled = enabled, modifier = Modifier.width(24.dp), drawTick = { _, _ -> }) },
        modifier = modifier.semantics { contentDescription = label },
    )
}
