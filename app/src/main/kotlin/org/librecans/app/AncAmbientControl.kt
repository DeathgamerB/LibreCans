package org.librecans.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AncAmbientControl(
    ambientSoundControl: Boolean,
    asmLevel: Int,
    focusOnVoice: Boolean,
    isFocusOnVoiceAvailable: Boolean,
    onSetAmbientMode: (controlOn: Boolean, level: Int, focusVoice: Boolean, sendWrite: Boolean) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(animationSpec = androidx.compose.animation.core.spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy, stiffness = androidx.compose.animation.core.Spring.StiffnessMedium)),
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
                val isNc = remember(ambientSoundControl, asmLevel) { ambientSoundControl && asmLevel == 0 }
                val isAmbient = remember(ambientSoundControl, asmLevel) { ambientSoundControl && asmLevel > 0 }
                val isOff = remember(ambientSoundControl) { !ambientSoundControl }

                val ncInteraction = remember { MutableInteractionSource() }
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSetAmbientMode(true, 0, false, true)
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
                val context = androidx.compose.ui.platform.LocalContext.current
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val prefs = PrefsHelper.getEncryptedPrefs(context, "librecans_ambient_prefs")
                        val storedLevel = prefs.getInt("ambient_sound_level", 20)
                        val storedFocus = prefs.getBoolean("focus_on_voice", false)
                        val targetLevel = if (asmLevel > 0) asmLevel else storedLevel
                        val targetFocus = if (targetLevel < 3) false else if (asmLevel > 0) focusOnVoice else storedFocus
                        onSetAmbientMode(true, targetLevel, targetFocus, true)
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
                        onSetAmbientMode(false, 0, false, true)
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
            val showSlider = remember(ambientSoundControl, asmLevel) { ambientSoundControl && asmLevel > 0 }
            AnimatedVisibility(
                visible = showSlider,
                enter = androidx.compose.animation.expandVertically(animationSpec = androidx.compose.animation.core.spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy, stiffness = androidx.compose.animation.core.Spring.StiffnessMedium)),
                exit = androidx.compose.animation.shrinkVertically(animationSpec = androidx.compose.animation.core.spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy, stiffness = androidx.compose.animation.core.Spring.StiffnessMedium))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp)
                ) {
                    var sliderValue by remember(asmLevel) { mutableStateOf(asmLevel.toFloat()) }
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
                            text = "${kotlin.math.round(sliderValue).toInt()}",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Slider(
                        modifier = Modifier.semantics { contentDescription = "Ambient Sound Level Slider" },
                        value = sliderValue,
                        onValueChange = { level ->
                            val intLevel = kotlin.math.round(level).toInt()
                            if (intLevel != kotlin.math.round(sliderValue).toInt()) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            sliderValue = level
                            val targetFocus = if (intLevel < 3) false else focusOnVoice
                            // Update local state optimistic without network
                            onSetAmbientMode(true, intLevel, targetFocus, false)
                        },
                        onValueChangeFinished = {
                            val intLevel = kotlin.math.round(sliderValue).toInt()
                            val targetFocus = if (intLevel < 3) false else focusOnVoice
                            onSetAmbientMode(true, intLevel, targetFocus, true)
                        },
                        valueRange = 1f..20f,
                        steps = 18,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    // Focus on Voice Toggle (Gated behind availability check)
                    if (isFocusOnVoiceAvailable) {
                        Spacer(modifier = Modifier.height(12.dp))

                        AnimatedVisibility(
                            visible = asmLevel >= 3,
                            enter = androidx.compose.animation.expandVertically(animationSpec = androidx.compose.animation.core.spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy, stiffness = androidx.compose.animation.core.Spring.StiffnessMedium)),
                            exit = androidx.compose.animation.shrinkVertically(animationSpec = androidx.compose.animation.core.spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy, stiffness = androidx.compose.animation.core.Spring.StiffnessMedium))
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
                                    modifier = Modifier.semantics { contentDescription = "Toggle Focus on Voice" },
                                    checked = focusOnVoice,
                                    onCheckedChange = { checked ->
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        val targetLevel = if (checked && asmLevel < 3) 3 else asmLevel
                                        onSetAmbientMode(true, targetLevel, checked, true)
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = asmLevel < 3,
                            enter = androidx.compose.animation.expandVertically(animationSpec = androidx.compose.animation.core.spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy, stiffness = androidx.compose.animation.core.Spring.StiffnessMedium)),
                            exit = androidx.compose.animation.shrinkVertically(animationSpec = androidx.compose.animation.core.spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy, stiffness = androidx.compose.animation.core.Spring.StiffnessMedium))
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
