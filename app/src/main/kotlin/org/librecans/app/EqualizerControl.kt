package org.librecans.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlin.math.roundToInt
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.librecans.protocol.EqPreset

@Composable
fun EqualizerControl(
    eqPreset: EqPreset,
    eqClearBass: Int,
    eqBands: List<Int>,
    onSetPreset: (EqPreset) -> Unit,
    onSetCustomEq: (clearBass: Int, bands: List<Int>, sendWrite: Boolean) -> Unit
) {
    var isSlidersExpanded by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    val presets = remember {
        listOf(
            Pair("Off", EqPreset.OFF),
            Pair("Bright", EqPreset.BRIGHT),
            Pair("Excited", EqPreset.EXCITED),
            Pair("Mellow", EqPreset.MELLOW),
            Pair("Relaxed", EqPreset.RELAXED),
            Pair("Vocal", EqPreset.VOCAL),
            Pair("Treble", EqPreset.TREBLE_BOOST),
            Pair("Bass", EqPreset.BASS_BOOST),
            Pair("Speech", EqPreset.SPEECH),
            Pair("Manual", EqPreset.MANUAL)
        )
    }

    val bandLabels = remember { listOf("400Hz", "1kHz", "2.5kHz", "6.3kHz", "16kHz") }

    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(animationSpec = androidx.compose.animation.core.spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy, stiffness = androidx.compose.animation.core.Spring.StiffnessMedium)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Equalizer",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

                Column(modifier = Modifier.testTag("eq_panel_content")) {
                    Spacer(modifier = Modifier.height(16.dp))

            // Horizontally scrollable row of presets (Material 3 Expressive)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(presets) { (label, preset) ->
                    val isSelected = if (preset == EqPreset.MANUAL) {
                        eqPreset == EqPreset.MANUAL ||
                        eqPreset == EqPreset.MANUAL_V2 ||
                        eqPreset == EqPreset.CUSTOM_1 ||
                        eqPreset == EqPreset.CUSTOM_2
                    } else {
                        eqPreset == preset
                    }
                    val chipInteractionSource = remember { MutableInteractionSource() }
                    val chipShape = if (isSelected) {
                        RoundedCornerShape(topStart = 8.dp, topEnd = 16.dp, bottomEnd = 8.dp, bottomStart = 12.dp)
                    } else {
                        RoundedCornerShape(8.dp)
                    }
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSetPreset(preset)
                            isSlidersExpanded = (preset == EqPreset.MANUAL || preset == EqPreset.MANUAL_V2 || preset == EqPreset.CUSTOM_1 || preset == EqPreset.CUSTOM_2)
                        },
                        interactionSource = chipInteractionSource,
                        modifier = Modifier.bounceClick(chipInteractionSource),
                        label = {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        },
                        shape = chipShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }

            // Manual band sliders (shown when MANUAL or V2 custom presets are selected)
            val isCustomPresetActive = remember(eqPreset) { eqPreset == EqPreset.MANUAL ||
                              eqPreset == EqPreset.MANUAL_V2 ||
                              eqPreset == EqPreset.CUSTOM_1 ||
                              eqPreset == EqPreset.CUSTOM_2 }
                AnimatedVisibility(visible = isCustomPresetActive) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { isSlidersExpanded = !isSlidersExpanded }
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Custom EQ Bands",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isSlidersExpanded) "▲" else "▼",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            AnimatedVisibility(
                visible = isCustomPresetActive && isSlidersExpanded,
                enter = androidx.compose.animation.expandVertically(animationSpec = androidx.compose.animation.core.spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy, stiffness = androidx.compose.animation.core.Spring.StiffnessMedium)),
                exit = androidx.compose.animation.shrinkVertically(animationSpec = androidx.compose.animation.core.spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy, stiffness = androidx.compose.animation.core.Spring.StiffnessMedium))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Divider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(vertical = 12.dp))

                    // 5 frequency bands
                    for (i in 0 until 5) {
                        val label = bandLabels[i]
                        val bandValue = eqBands.getOrElse(i) { 0 }
                        var sliderValue by remember(bandValue) { mutableStateOf(bandValue.toFloat()) }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.width(60.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Slider(
                                value = sliderValue,
                                onValueChange = { newValue ->
                                    val intValue = newValue.roundToInt().coerceIn(-10, 10)
                                    if (intValue != sliderValue.roundToInt()) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                    sliderValue = newValue
                                    val newBands = eqBands.toMutableList()
                                    while (newBands.size < 5) newBands.add(0)
                                    newBands[i] = intValue
                                    onSetCustomEq(eqClearBass, newBands, false)
                                },
                                onValueChangeFinished = {
                                    val intValue = sliderValue.roundToInt().coerceIn(-10, 10)
                                    val newBands = eqBands.toMutableList()
                                    while (newBands.size < 5) newBands.add(0)
                                    newBands[i] = intValue
                                    onSetCustomEq(eqClearBass, newBands, true)
                                },
                                valueRange = -10f..10f,
                                steps = 19,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = (if (sliderValue.roundToInt() > 0) "+" else "") + sliderValue.roundToInt().toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(32.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Clear Bass Slider
                    var cbSliderValue by remember(eqClearBass) { mutableStateOf(eqClearBass.toFloat()) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Clear Bass",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(80.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Slider(
                            value = cbSliderValue,
                            onValueChange = { newValue ->
                                val intValue = newValue.roundToInt()
                                if (intValue != cbSliderValue.roundToInt()) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                                cbSliderValue = newValue
                                onSetCustomEq(intValue, eqBands, false)
                            },
                            onValueChangeFinished = {
                                onSetCustomEq(cbSliderValue.roundToInt(), eqBands, true)
                            },
                            valueRange = -10f..10f,
                            steps = 19,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = (if (cbSliderValue.roundToInt() > 0) "+" else "") + cbSliderValue.roundToInt().toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(32.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
                }
        }
    }
}
