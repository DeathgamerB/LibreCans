package org.librecans.app.ui.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.librecans.app.data.bluetooth.BluetoothSnapshot
import org.librecans.app.data.bluetooth.PairedDevice
import org.librecans.app.data.connection.ConnectionState
import org.librecans.app.data.connection.DemoHeadphones
import org.librecans.app.data.connection.DeviceTarget
import org.librecans.app.data.connection.HeadphonesRepository
import org.librecans.app.data.settings.SettingsRepository
import kotlinx.coroutines.flow.Flow

data class DevicesUiState(
    val bluetooth: BluetoothSnapshot = BluetoothSnapshot(available = true, enabled = true, permitted = true, devices = emptyList()),
    val connection: ConnectionState = ConnectionState.Idle,
    val showDemo: Boolean = false,
)

class DevicesViewModel(
    private val observeBluetooth: () -> Flow<BluetoothSnapshot>,
    private val permissionGranted: () -> Boolean,
    private val repository: HeadphonesRepository,
    private val settings: SettingsRepository,
    showDemo: Boolean,
) : ViewModel() {
    private val permission = MutableStateFlow(permissionGranted())
    private var autoConnectTried = false

    private val snapshots = permission.flatMapLatest { observeBluetooth() }

    val state: StateFlow<DevicesUiState> = combine(snapshots, repository.connection) { bluetooth, connection ->
        DevicesUiState(bluetooth, connection, showDemo)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DevicesUiState(showDemo = showDemo))

    init {
        viewModelScope.launch { autoConnect() }
    }

    fun onPermissionResult() {
        permission.value = permissionGranted()
    }

    fun connect(device: PairedDevice) {
        repository.connect(DeviceTarget(device.address, device.name, device.kind))
    }

    fun connectDemo() = repository.connect(DemoHeadphones.target)

    fun retry() {
        state.value.connection.target?.let(repository::connect)
    }

    fun disconnect() = repository.disconnect()

    /** Connects once per launch to headphones that are already playing audio through this phone. */
    private suspend fun autoConnect() {
        if (autoConnectTried) return
        val prefs = settings.load()
        if (!prefs.autoConnect) return
        val snapshot = snapshots.filter { it.permitted && it.enabled }.first()
        autoConnectTried = true
        if (repository.connection.value != ConnectionState.Idle) return
        AutoConnectPolicy.choose(snapshot.devices, prefs.lastDevice?.address)?.let(::connect)
    }
}

object AutoConnectPolicy {
    /** Prefers the last used headphones, then any Sony headphones currently connected for audio. */
    fun choose(devices: List<PairedDevice>, lastAddress: String?): PairedDevice? {
        val connected = devices.filter { it.audioConnected }
        return connected.firstOrNull { it.address == lastAddress } ?: connected.singleOrNull()
    }
}
