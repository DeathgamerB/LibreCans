package org.librecans.app

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

@Composable
fun PairingScreen(
    viewModel: HeadphonesViewModel,
    onNavigateToMain: () -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.connectionState.collectAsState()
    val pairedDevices by viewModel.pairedDevices.collectAsState()

    // Permission state
    var permissionsGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    var showRationale by remember { mutableStateOf(!permissionsGranted) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results.values.all { it }
        permissionsGranted = granted
        showRationale = !granted
        if (granted) {
            viewModel.initializeService()
        }
    }

    // Spring scaling for mock button
    var pressedMock by remember { mutableStateOf(false) }
    val mockScale by animateFloatAsState(
        targetValue = if (pressedMock) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "MockScale"
    )

    // Automatically navigate to Main if connected
    LaunchedEffect(state.connected) {
        if (state.connected) {
            kotlinx.coroutines.delay(200)
            onNavigateToMain()
        }
    }

    LaunchedEffect(permissionsGranted) {
        if (permissionsGranted) {
            viewModel.initializeService()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = WindowInsets.systemBars.asPaddingValues().calculateTopPadding(), bottom = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "LibreCans",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            Text(
                text = "Autonomous Companion for Sony Headsets",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            if (showRationale) {
                // Permission Rationale Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Bluetooth Permission Required",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "LibreCans communicates with Sony headphones using Bluetooth RFCOMM/SPP. On Android 12+, we need your permission to find and connect to your paired audio accessories.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.BLUETOOTH_CONNECT,
                                            Manifest.permission.BLUETOOTH_SCAN
                                        )
                                    )
                                }
                            },
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Grant Permission", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            } else {
                // Device List Section
                Text(
                    text = "SELECT PAIRED DEVICE",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    if (pairedDevices.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No paired Sony headsets found. Pair them in Android settings first.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .testTag("device_list")
                                .fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(pairedDevices) { device ->
                                DeviceItemRow(
                                    device = device,
                                    isConnecting = state.connecting && state.deviceMac == device.address
                                ) {
                                    if (state.connected && state.deviceMac == device.address) {
                                        onNavigateToMain()
                                    } else if (!state.connecting) {
                                        viewModel.connectDevice(device.address)
                                    }
                                }
                            }
                        }
                    }
                }

                state.errorMessage?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (BuildConfig.DEBUG) {
                    // Debug-gated Mock Mode Box (Clearly Isolated)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .scale(mockScale),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Debug Emulator Mock",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    "Simulate WH-CH720N features",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            }
                            Button(
                                onClick = {
                                    pressedMock = true
                                    viewModel.toggleMockMode(true)
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Simulate")
                            }
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun DeviceItemRow(
    device: BluetoothDevice,
    isConnecting: Boolean = false,
    onClick: () -> Unit
) {
    val deviceName = device.name ?: "Sony Headset"
    val isTWS = deviceName.startsWith("WF-", ignoreCase = true) || deviceName.contains("LinkBuds", ignoreCase = true)
    val interactionSource = remember { MutableInteractionSource() }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClickLabel = "Connect to $deviceName",
                role = Role.Button
            ) { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = if (isTWS) R.drawable.ic_device_earbuds else R.drawable.ic_device_headphones),
                contentDescription = if (isTWS) "Earbuds" else "Headphones",
                modifier = Modifier
                    .size(48.dp)
                    .padding(end = 16.dp)
            )
            Column {
                Text(
                    text = deviceName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = device.address,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isConnecting) {
                Spacer(modifier = Modifier.weight(1f))
                CircularProgressIndicator(
                    modifier = Modifier
                        .testTag("loading_state")
                        .size(24.dp)
                        .semantics { contentDescription = "Connecting" },
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
