package org.librecans.app.ui.devices

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.librecans.app.BuildConfig
import org.librecans.app.R
import org.librecans.app.data.bluetooth.BluetoothPermissions
import org.librecans.app.data.bluetooth.PairedDevice
import org.librecans.app.data.connection.ConnectionState
import org.librecans.app.data.connection.DemoHeadphones
import org.librecans.app.ui.common.appViewModel
import org.librecans.app.ui.common.rememberHaptics
import org.librecans.app.ui.components.AppScaffold
import org.librecans.app.ui.components.Segment
import org.librecans.app.ui.components.section

@Composable
fun DevicesEntry(onOpenDashboard: () -> Unit, onOpenAbout: () -> Unit) {
    val context = LocalContext.current
    val viewModel = appViewModel { container ->
        DevicesViewModel(
            observeBluetooth = container.bluetooth::observe,
            permissionGranted = { BluetoothPermissions.granted(context) },
            repository = container.headphones,
            settings = container.settings,
            showDemo = BuildConfig.SHOW_DEMO,
        )
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        viewModel.onPermissionResult()
    }
    val enableLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

    DevicesScreen(
        state = state,
        actions = DevicesActions(
            onConnect = { device ->
                if (state.connection is ConnectionState.Connected && state.connection.target?.address == device.address) {
                    onOpenDashboard()
                } else {
                    viewModel.connect(device)
                }
            },
            onConnectDemo = {
                if (state.connection.target?.isDemo == true && state.connection is ConnectionState.Connected) onOpenDashboard()
                else viewModel.connectDemo()
            },
            onRetry = viewModel::retry,
            onCancel = viewModel::disconnect,
            onOpenDashboard = onOpenDashboard,
            onGrantPermission = {
                permissionLauncher.launch(BluetoothPermissions.required + BluetoothPermissions.optional)
            },
            onEnableBluetooth = { enableLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)) },
            onOpenBluetoothSettings = { context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) },
            onOpenAbout = onOpenAbout,
        ),
    )
}

data class DevicesActions(
    val onConnect: (PairedDevice) -> Unit = {},
    val onConnectDemo: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onCancel: () -> Unit = {},
    val onOpenDashboard: () -> Unit = {},
    val onGrantPermission: () -> Unit = {},
    val onEnableBluetooth: () -> Unit = {},
    val onOpenBluetoothSettings: () -> Unit = {},
    val onOpenAbout: () -> Unit = {},
)

@Composable
fun DevicesScreen(state: DevicesUiState, actions: DevicesActions) {
    val bluetooth = state.bluetooth
    val pairedTitle = stringResource(R.string.devices_section_paired)
    val demoTitle = stringResource(R.string.devices_section_demo)
    val haptics = rememberHaptics()
    AppScaffold(
        title = stringResource(R.string.devices_title),
        subtitle = stringResource(R.string.app_tagline),
        actions = {
            IconButton(
                onClick = {
                    haptics.tap()
                    actions.onOpenAbout()
                },
                modifier = Modifier.testTag("about"),
            ) {
                Icon(painterResource(R.drawable.ic_info), stringResource(R.string.action_about))
            }
        },
    ) {
        (state.connection as? ConnectionState.Failed)?.let { failed ->
            item(key = "failure") { FailureCard(failed, actions.onRetry, Modifier.animateItem()) }
        }
        when {
            !bluetooth.available -> item(key = "unavailable") {
                StatusCard(
                    icon = R.drawable.ic_bluetooth_disabled,
                    title = stringResource(R.string.failure_bluetooth_unavailable),
                    body = null,
                )
            }
            !bluetooth.permitted -> item(key = "permission") {
                StatusCard(
                    icon = R.drawable.ic_bluetooth,
                    title = stringResource(R.string.devices_permission_title),
                    body = stringResource(R.string.devices_permission_body),
                    action = stringResource(R.string.devices_permission_grant) to actions.onGrantPermission,
                    modifier = Modifier.testTag("permission_card"),
                )
            }
            !bluetooth.enabled -> item(key = "bluetooth_off") {
                StatusCard(
                    icon = R.drawable.ic_bluetooth_disabled,
                    title = stringResource(R.string.devices_bluetooth_off_title),
                    body = stringResource(R.string.devices_bluetooth_off_body),
                    action = stringResource(R.string.devices_bluetooth_enable) to actions.onEnableBluetooth,
                )
            }
            bluetooth.devices.isEmpty() -> item(key = "empty") {
                StatusCard(
                    icon = R.drawable.ic_headphones,
                    title = stringResource(R.string.devices_empty_title),
                    body = stringResource(R.string.devices_empty_body),
                    action = stringResource(R.string.devices_open_bluetooth_settings) to actions.onOpenBluetoothSettings,
                    modifier = Modifier.testTag("empty_state"),
                )
            }
            else -> section("paired", pairedTitle) {
                bluetooth.devices.forEachIndexed { index, device ->
                    item(key = device.address) {
                        DeviceItem(
                            device = device,
                            connection = state.connection,
                            segment = Segment(index, bluetooth.devices.size),
                            onClick = { actions.onConnect(device) },
                            onCancel = actions.onCancel,
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
        }
        if (state.showDemo) {
            section("demo", demoTitle) {
                item(key = "demo") {
                    DemoItem(
                        connection = state.connection,
                        onClick = actions.onConnectDemo,
                        onCancel = actions.onCancel,
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }
}

internal fun ConnectionState.isFor(address: String): Boolean = target?.address == address

internal val demoAddress = DemoHeadphones.target.address
