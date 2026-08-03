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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboard(
    viewModel: HeadphonesViewModel,
    onNavigateToAbout: () -> Unit,
    onNavigateToPairing: () -> Unit
) {
    val state by viewModel.connectionState.collectAsState()
    val scrollState = rememberScrollState()

    // If connection is lost, navigate back to pairing screen
    LaunchedEffect(state.connected) {
        if (!state.connected && !state.connecting) {
            onNavigateToPairing()
        }
    }

    Scaffold(
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
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 20.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Hero Image and Battery
            DeviceHeroCard(state = state)

            // ANC/Ambient Controls Card
            AncAmbientControl(
                state = state,
                onSetAmbientMode = { controlOn, level, focusVoice, sendWrite ->
                    viewModel.setAmbientMode(controlOn, level, focusVoice, sendWrite)
                }
            )

            // Equalizer Controls Card (Gated behind v2 protocol or mock check)
            val supportsEq = state.protocolVersion == org.librecans.protocol.SonyProtocolVersion.V2 || viewModel.connectionState.value.deviceName.contains("Mock")
            if (supportsEq) {
                EqualizerControl(
                    state = state,
                    onSetPreset = { preset ->
                        viewModel.setEqualizerPreset(preset)
                    },
                    onSetCustomEq = { clearBass, bands, sendWrite ->
                        viewModel.setEqualizerCustom(clearBass, bands, sendWrite)
                    }
                )
            } else {
                // Read-only/No EQ card for older V1 devices
                Card(
                    modifier = Modifier.fillMaxWidth(),
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
            SettingsControl(
                state = state,
                onSetDsee = { viewModel.setDsee(it) },
                onSetAutoPowerOff = { viewModel.setAutoPowerOff(it) },
                onSetSpeakToChat = { viewModel.setSpeakToChat(it) },
                onSetAdaptiveVolume = { viewModel.setAdaptiveVolume(it) }
            )

            // Hardware Information Card
            HardwareInformationCard(state = state)

            // Disconnect Button
            Button(
                onClick = { viewModel.disconnectDevice() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text(
                    "Disconnect",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onError
                )
            }

            Spacer(modifier = Modifier.height(innerPadding.calculateBottomPadding()))
        }
    }
}
