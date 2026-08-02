package org.librecans.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AncAmbientControl(
    state: BluetoothDeviceState,
    onSetAmbientMode: (controlOn: Boolean, level: Int, focusVoice: Boolean) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Ambient Sound Control",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Three-state segmented row: NC / Ambient / Off
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val isNc = state.ambientSoundControl && state.asmLevel == 0
                val isAmbient = state.ambientSoundControl && state.asmLevel > 0
                val isOff = !state.ambientSoundControl

                val ncInteraction = remember { MutableInteractionSource() }
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSetAmbientMode(true, 0, false)
                    },
                    interactionSource = ncInteraction,
                    modifier = Modifier.weight(1f).bounceClick(ncInteraction),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isNc) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isNc) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Text("NC", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                val ambientInteraction = remember { MutableInteractionSource() }
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val targetLevel = if (state.asmLevel > 0) state.asmLevel else 20
                        val targetFocus = if (targetLevel < 3) false else state.focusOnVoice
                        onSetAmbientMode(true, targetLevel, targetFocus)
                    },
                    interactionSource = ambientInteraction,
                    modifier = Modifier.weight(1.3f).bounceClick(ambientInteraction),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isAmbient) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isAmbient) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Text("Ambient", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                val offInteraction = remember { MutableInteractionSource() }
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSetAmbientMode(false, 0, false)
                    },
                    interactionSource = offInteraction,
                    modifier = Modifier.weight(1f).bounceClick(offInteraction),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isOff) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isOff) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Text("Off", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            // Slider for Ambient Sound Level (only shown when Ambient is selected)
            val showSlider = state.ambientSoundControl && state.asmLevel > 0
            AnimatedVisibility(
                visible = showSlider,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Ambient Sound Level",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${state.asmLevel}",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Slider(
                        value = state.asmLevel.toFloat(),
                        onValueChange = { level ->
                            val intLevel = level.toInt()
                            if (intLevel != state.asmLevel) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            // Only allow focus voice to remain enabled if level >= 3
                            val targetFocus = if (intLevel < 3) false else state.focusOnVoice
                            onSetAmbientMode(true, intLevel, targetFocus)
                        },
                        valueRange = 1f..20f,
                        steps = 18,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    // Focus on Voice Toggle (Gated behind availability check)
                    if (state.isFocusOnVoiceAvailable) {
                        Spacer(modifier = Modifier.height(12.dp))

                        AnimatedVisibility(
                            visible = state.asmLevel >= 3,
                            enter = expandVertically(),
                            exit = shrinkVertically()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Focus on Voice",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Enhance voices while filtering noise",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = state.focusOnVoice,
                                    onCheckedChange = { checked ->
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onSetAmbientMode(true, state.asmLevel, checked)
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = state.asmLevel < 3,
                            enter = expandVertically(),
                            exit = shrinkVertically()
                        ) {
                            Text(
                                text = "Focus on Voice is unavailable below level 3",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
