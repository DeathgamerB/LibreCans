package org.librecans.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.layout
import org.librecans.app.LocalScrollValue
import androidx.compose.ui.layout.ContentScale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsControl(
    dsee: Boolean,
    hasAutoPowerOff: Boolean,
    autoPowerOff: Int,
    hasSpeakToChat: Boolean,
    speakToChat: Boolean,
    hasAdaptiveVolume: Boolean,
    adaptiveVolume: Boolean,
    onSetDsee: (Boolean) -> Unit,
    onSetAutoPowerOff: (Int) -> Unit,
    onSetSpeakToChat: (Boolean) -> Unit,
    onSetAdaptiveVolume: (Boolean) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val apoOptions = listOf(
        "Off", "5 Minutes", "30 Minutes", "1 Hour", "3 Hours", "When Taken Off"
    )

    Column(
        modifier = Modifier.fillMaxWidth().animateContentSize(animationSpec = androidx.compose.animation.core.spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy, stiffness = androidx.compose.animation.core.Spring.StiffnessMedium)),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {


        var isFeaturesExpanded by remember { mutableStateOf(false) }

        // Settings Controls (Capability-gated extras)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { isFeaturesExpanded = !isFeaturesExpanded }
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Features & Configuration",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (isFeaturesExpanded) "▲" else "▼",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                AnimatedVisibility(
                    visible = isFeaturesExpanded,
                    enter = expandVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)),
                    exit = shrinkVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium))
                ) {
                    Column(
                        modifier = Modifier.testTag("settings_panel_content"),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Spacer(modifier = Modifier.height(4.dp))

                        // DSEE (Always supported on V2)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "DSEE Upscaling",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Upscale compressed high-frequency audio",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = dsee,
                            onCheckedChange = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSetDsee(it)
                            }
                            )
                        }

                        // Auto Power-Off (Capability-gated)
                        if (hasAutoPowerOff) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                            var expanded by remember { mutableStateOf(false) }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Auto Power-Off",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Turn off headphones when idle",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Box {
                                    Text(
                                        text = apoOptions.getOrElse(autoPowerOff) { "Off" },
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { expanded = true }
                                            .padding(8.dp)
                                    )
                                    DropdownMenu(
                                        expanded = expanded,
                                        onDismissRequest = { expanded = false }
                                    ) {
                                        apoOptions.forEachIndexed { index, option ->
                                            DropdownMenuItem(
                                                text = { Text(option) },
                                                onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    onSetAutoPowerOff(index)
                                                    expanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Speak-to-Chat (Capability-gated)
                        if (hasSpeakToChat) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Speak-to-Chat",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Pause music automatically when you speak",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = speakToChat,
                                onCheckedChange = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onSetSpeakToChat(it)
                                }
                                )
                            }
                        }

                        // Adaptive Volume (Capability-gated)
                        if (hasAdaptiveVolume) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Adaptive Volume Control",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Auto-adjust volume based on your environment",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = adaptiveVolume,
                                onCheckedChange = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onSetAdaptiveVolume(it)
                                }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BatteryIndicator(label: String, percent: Int) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = if (percent >= 0) "$percent%" else "--%",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun DeviceHeroCard(
    deviceName: String,
    deviceMac: String,
    batteryLevel: Int,
    batteryCharging: Boolean,
    batteryLeft: Int,
    batteryRight: Int,
    batteryCase: Int
) {
    val scrollValueProvider = LocalScrollValue.current
    // Hero Image and Battery Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, bottom = 20.dp, top = 0.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Premium Generic Headphone Artwork
                val isTWS = deviceName.startsWith("WF-", ignoreCase = true) || deviceName.contains("LinkBuds", ignoreCase = true)
                Image(
                    painter = painterResource(id = if (isTWS) R.drawable.ic_device_earbuds else R.drawable.ic_device_headphones),
                    contentDescription = if (isTWS) "Earbuds" else "Headphones",
                    modifier = Modifier
                        .layout { measurable, constraints ->
                            val scrollValue = scrollValueProvider()
                            val targetScale: Float
                            val topPadding: Int
                            val bottomCrop: Int

                            if (isTWS) {
                                targetScale = (0.95f - (scrollValue / (800 * density))).coerceIn(0.475f, 0.95f)
                                topPadding = 0
                                bottomCrop = (constraints.maxWidth * 0.10f).toInt()
                            } else {
                                targetScale = (0.8f - (scrollValue / (800 * density))).coerceIn(0.4f, 0.8f)
                                topPadding = ((0.8f - targetScale) * constraints.maxWidth * 0.15f).toInt()
                                bottomCrop = 0
                            }

                            val newWidth = (constraints.maxWidth * targetScale).toInt()
                            val placeable = measurable.measure(constraints.copy(
                                minWidth = newWidth,
                                maxWidth = newWidth
                            ))
                            layout(placeable.width, placeable.height + topPadding - bottomCrop) {
                                placeable.placeRelative(0, topPadding)
                            }
                        }
                        .aspectRatio(1f),
                    contentScale = ContentScale.Fit
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Model name & mac
                Text(
                    text = deviceName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = deviceMac,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Battery indicators
                if (isTWS) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        BatteryIndicator(label = "Left Bud", percent = batteryLeft)
                        BatteryIndicator(label = "Right Bud", percent = batteryRight)
                        if (batteryCase >= 0) {
                            BatteryIndicator(label = "Case", percent = batteryCase)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Battery: ",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (batteryLevel >= 0) "${batteryLevel}%" else "Unknown",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (batteryCharging) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "⚡ Charging",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }


}

@Composable
fun HardwareInformationCard(
    deviceMac: String,
    protocolVersion: org.librecans.protocol.SonyProtocolVersion?,
    hasFirmware: Boolean,
    firmware: String,
    hasCodec: Boolean,
    codec: String
) {
        var isHardwareExpanded by remember { mutableStateOf(false) }

        // Hardware details / Readout Card
        Card(
            modifier = Modifier.fillMaxWidth().animateContentSize(animationSpec = androidx.compose.animation.core.spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy, stiffness = androidx.compose.animation.core.Spring.StiffnessMedium)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { isHardwareExpanded = !isHardwareExpanded }
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Hardware Information",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (isHardwareExpanded) "▲" else "▼",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                androidx.compose.animation.AnimatedVisibility(
                    isHardwareExpanded,
                    Modifier,
                    androidx.compose.animation.expandVertically(animationSpec = androidx.compose.animation.core.spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy, stiffness = androidx.compose.animation.core.Spring.StiffnessMedium)),
                    androidx.compose.animation.shrinkVertically(animationSpec = androidx.compose.animation.core.spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy, stiffness = androidx.compose.animation.core.Spring.StiffnessMedium))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Spacer(modifier = Modifier.height(4.dp))
                        DetailRow(label = "MAC Address", value = deviceMac)
                        DetailRow(label = "Codec", value = if (hasCodec) codec else "SBC")
                        DetailRow(label = "Firmware", value = if (hasFirmware) firmware else "1.0.0")
                        DetailRow(
                            label = "Protocol Version",
                            value = if (protocolVersion != null) protocolVersion.name else "Unknown"
                        )
                    }
                }
            }
        }
}