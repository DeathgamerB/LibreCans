package org.librecans.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.librecans.protocol.EqPreset

@Composable
fun EqualizerControl(
    state: BluetoothDeviceState,
    onSetPreset: (EqPreset) -> Unit,
    onSetCustomEq: (clearBass: Int, bands: List<Int>) -> Unit
) {
    val presets = listOf(
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

    val bandLabels = listOf("400Hz", "1kHz", "2.5kHz", "6.3kHz", "16kHz")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Equalizer",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Presets grid (Material 3 Expressive chip design)
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                items(presets) { (label, preset) ->
                    val isSelected = state.eqPreset == preset
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSetPreset(preset) },
                        label = {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 1
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }

            // Manual band sliders (only shown when MANUAL (0xA0) is selected)
            val showSliders = state.eqPreset == EqPreset.MANUAL
            AnimatedVisibility(
                visible = showSliders,
                enter = expandVertically(),
                exit = shrinkVertically()
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
                        val bandValue = state.eqBands.getOrElse(i) { 0 }

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
                                value = bandValue.toFloat(),
                                onValueChange = { newValue ->
                                    val newBands = state.eqBands.toMutableList()
                                    while (newBands.size < 5) newBands.add(0)
                                    newBands[i] = newValue.toInt()
                                    onSetCustomEq(state.eqClearBass, newBands)
                                },
                                valueRange = -10f..10f,
                                steps = 19,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = (if (bandValue > 0) "+" else "") + bandValue.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(32.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Clear Bass Slider
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
                            value = state.eqClearBass.toFloat(),
                            onValueChange = { newValue ->
                                onSetCustomEq(newValue.toInt(), state.eqBands)
                            },
                            valueRange = -10f..10f,
                            steps = 19,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = (if (state.eqClearBass > 0) "+" else "") + state.eqClearBass.toString(),
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
