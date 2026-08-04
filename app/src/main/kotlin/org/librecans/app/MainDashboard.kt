package org.librecans.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent

val LocalScrollValue = compositionLocalOf<() -> Int> { { 0 } }

@Composable
fun Modifier.shrinkAtTop(scrollState: androidx.compose.foundation.ScrollState, topThreshold: Float): Modifier {
    var yPos by remember { mutableFloatStateOf(0f) }
    return this
        .onGloballyPositioned { yPos = it.positionInParent().y }
        .graphicsLayer {
            val currentY = yPos - scrollState.value
            if (currentY < topThreshold) {
                val overflow = topThreshold - currentY
                val scale = (1f - (overflow / (400f * density))).coerceAtLeast(0.5f)
                scaleX = scale
                scaleY = scale
                alpha = (1f - (overflow / (400f * density))).coerceAtLeast(0f)
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.5f)
            }
        }
}

@Composable
fun OverlayLayout(topBar: @Composable () -> Unit, content: @Composable (Dp) -> Unit) {
    val density = LocalDensity.current
    SubcomposeLayout { constraints ->
        val topBarPlaceables = subcompose("topBar", topBar).map { it.measure(constraints) }
        val topPadding = if (topBarPlaceables.isNotEmpty()) with(density) { topBarPlaceables.maxOf { it.height }.toDp() } else 0.dp
        val contentPlaceables = subcompose("content") { content(topPadding) }.map { it.measure(constraints) }
        layout(constraints.maxWidth, constraints.maxHeight) {
            contentPlaceables.forEach { it.placeRelative(0, 0) }
            topBarPlaceables.forEach { it.placeRelative(0, 0) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboard(
    viewModel: HeadphonesViewModel,
    onNavigateToAbout: () -> Unit,
    onNavigateToPairing: () -> Unit
) {
    val state by viewModel.connectionState.collectAsState()
    val scrollState = rememberScrollState()
    val density = LocalDensity.current

    // If connection is lost, navigate back to pairing screen
    LaunchedEffect(state.connected) {
        if (!state.connected && !state.connecting) {
            onNavigateToPairing()
        }
    }

    OverlayLayout(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "LibreCans",
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                actions = {
                    TextButton(onClick = onNavigateToAbout) {
                        Text("About", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent
                )
            )
        },
        content = { topPadding ->
            CompositionLocalProvider(LocalScrollValue provides { scrollState.value }) {
                val topThreshold = with(density) { topPadding.toPx() }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(horizontal = 20.dp)
                        .verticalScroll(scrollState)
                        .padding(top = topPadding * 0.6f, bottom = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Hero Image and Battery
            DeviceHeroCard(
                deviceName = state.deviceName,
                deviceMac = state.deviceMac,
                batteryLevel = state.batteryLevel,
                batteryCharging = state.batteryCharging,
                batteryLeft = state.batteryLeft,
                batteryRight = state.batteryRight,
                batteryCase = state.batteryCase
            )

            // ANC/Ambient Controls Card
            Box(modifier = Modifier.shrinkAtTop(scrollState, topThreshold)) {
            AncAmbientControl(
                ambientSoundControl = state.ambientSoundControl,
                asmLevel = state.asmLevel,
                focusOnVoice = state.focusOnVoice,
                isFocusOnVoiceAvailable = state.isFocusOnVoiceAvailable,
                onSetAmbientMode = { controlOn, level, focusVoice, sendWrite ->
                    viewModel.setAmbientMode(controlOn, level, focusVoice, sendWrite)
                }
            )
            }

            // Equalizer Controls Card (Gated behind v2 protocol or mock check)
            val supportsEq = state.protocolVersion == org.librecans.protocol.SonyProtocolVersion.V2 || state.deviceName.contains("Mock")
            if (supportsEq) {
                Box(modifier = Modifier.shrinkAtTop(scrollState, topThreshold)) {
                EqualizerControl(
                    eqPreset = state.eqPreset,
                    eqClearBass = state.eqClearBass,
                    eqBands = state.eqBands,
                    onSetPreset = { preset ->
                        viewModel.setEqualizerPreset(preset)
                    },
                    onSetCustomEq = { clearBass, bands, sendWrite ->
                        viewModel.setEqualizerCustom(clearBass, bands, sendWrite)
                    }
                )
                }
            } else {
                // Read-only/No EQ card for older V1 devices
                Card(
                    modifier = Modifier.fillMaxWidth().shrinkAtTop(scrollState, topThreshold),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Equalizer",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Equalizer control is not supported on this V1 device (e.g. WH-1000XM4). Upstream protocol uses separate non-reversible layouts.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // General Settings Card
            Box(modifier = Modifier.shrinkAtTop(scrollState, topThreshold)) {
            SettingsControl(
                dsee = state.dsee,
                hasAutoPowerOff = state.hasAutoPowerOff,
                autoPowerOff = state.autoPowerOff,
                hasSpeakToChat = state.hasSpeakToChat,
                speakToChat = state.speakToChat,
                hasAdaptiveVolume = state.hasAdaptiveVolume,
                adaptiveVolume = state.adaptiveVolume,
                onSetDsee = { viewModel.setDsee(it) },
                onSetAutoPowerOff = { viewModel.setAutoPowerOff(it) },
                onSetSpeakToChat = { viewModel.setSpeakToChat(it) },
                onSetAdaptiveVolume = { viewModel.setAdaptiveVolume(it) }
            )
            }

            // Hardware Information Card
            Box(modifier = Modifier.shrinkAtTop(scrollState, topThreshold)) {
            HardwareInformationCard(
                deviceMac = state.deviceMac,
                protocolVersion = state.protocolVersion,
                hasFirmware = state.hasFirmware,
                firmware = state.firmware,
                hasCodec = state.hasCodec,
                codec = state.codec
            )
            }

            // Disconnect Button
            Button(
                onClick = {
                    onNavigateToPairing()
                    viewModel.disconnectDevice()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .shrinkAtTop(scrollState, topThreshold),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text(
                    "Disconnect",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onError
                )
            }
            }
        }
    })
}
