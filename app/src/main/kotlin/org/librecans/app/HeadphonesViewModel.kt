package org.librecans.app

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.librecans.protocol.EqPreset
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

@SuppressLint("MissingPermission")
class HeadphonesViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "HeadphonesViewModel"

    private val _pairedDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    private var isReceiverRegistered = false

    private val bluetoothReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            if (action == BluetoothDevice.ACTION_ACL_CONNECTED) {
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                val name = device?.name ?: ""
                if (looksLikeSonyHeadset(name)) {
                    Log.d(TAG, "Sony headset ACL Connected: $name, triggering auto-connect")
                    autoConnectIfPaired()
                }
            }
        }
    }
    val pairedDevices: StateFlow<List<BluetoothDevice>> = _pairedDevices.asStateFlow()

    private val _connectionState = MutableStateFlow(BluetoothDeviceState())
    val connectionState: StateFlow<BluetoothDeviceState> = _connectionState.asStateFlow()

    private var isBound = false
    private var connectionService: BluetoothConnectionService? = null
    private var isServiceInitialized = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? BluetoothConnectionService.LocalBinder
            connectionService = binder?.getService()
            isBound = true
            Log.d(TAG, "Service bound successfully")

            connectionService?.let { s ->
                viewModelScope.launch {
                    s.repository.deviceState.collect { state ->
                        _connectionState.value = state
                    }
                }
            }

            // Auto connect if needed on start
            autoConnectIfPaired()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
            connectionService = null
            Log.d(TAG, "Service unbound")
        }
    }

    init {
        initializeService()
    }

    fun initializeService() {
        if (isServiceInitialized) return
        val context = getApplication<Application>().applicationContext
        val hasPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        if (hasPermissions) {
            isServiceInitialized = true
            Log.d(TAG, "Initializing and starting BluetoothConnectionService")
            val intent = Intent(context, BluetoothConnectionService::class.java)
            context.startService(intent) // Ensure service stays alive independently of activity
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)

            // Register BroadcastReceiver for real-time Bluetooth system connection detection
            val filter = android.content.IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            }
            context.registerReceiver(bluetoothReceiver, filter)
            isReceiverRegistered = true

            loadPairedDevices()
        } else {
            Log.d(TAG, "Cannot initialize BluetoothConnectionService yet: permissions not granted")
        }
    }

    fun loadPairedDevices() {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return
        try {
            val bonded = adapter.bondedDevices.toList().filter { dev ->
                val name = dev.name ?: ""
                looksLikeSonyHeadset(name)
            }
            _pairedDevices.value = bonded
            autoConnectIfPaired()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load paired devices: ${e.message}")
        }
    }

    private fun looksLikeSonyHeadset(name: String): Boolean {
        val prefixes = listOf("WH-", "WF-", "WI-", "MDR-", "XB", "LinkBuds")
        return prefixes.any { name.startsWith(it, ignoreCase = true) || name.contains(it, ignoreCase = true) }
    }

    fun getConnectedSonyDevice(): BluetoothDevice? {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return null
        return try {
            val isConnectedMethod = BluetoothDevice::class.java.getMethod("isConnected")
            adapter.bondedDevices.firstOrNull { dev ->
                val name = dev.name ?: ""
                looksLikeSonyHeadset(name) && isConnectedMethod.invoke(dev) as Boolean
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check device connection status via reflection: ${e.message}")
            null
        }
    }

    private fun autoConnectIfPaired() {
        if (_connectionState.value.connected || _connectionState.value.connecting) return
        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            var connectedDevice = getConnectedSonyDevice()

            // Poll for up to 5 seconds, checking every 500ms
            while (connectedDevice == null && (System.currentTimeMillis() - startTime) < 5000) {
                delay(500)
                connectedDevice = getConnectedSonyDevice()
            }

            if (connectedDevice != null) {
                if (!_connectionState.value.connected && !_connectionState.value.connecting) {
                    Log.d(TAG, "Auto-connecting to already connected system Sony headset: ${connectedDevice.name} (${connectedDevice.address})")
                    connectDevice(connectedDevice.address)
                }
            }
        }
    }

    fun connectDevice(macAddress: String) {
        // Instantly transition local viewmodel state to Connecting to eliminate any service binding/start latencies
        _connectionState.value = BluetoothDeviceState(connecting = true, deviceMac = macAddress)
        val context = getApplication<Application>().applicationContext
        val intent = Intent(context, BluetoothConnectionService::class.java).apply {
            putExtra("EXTRA_MAC_ADDRESS", macAddress)
        }
        context.startService(intent)
        connectionService?.repository?.connect(macAddress)
    }

    fun disconnectDevice() {
        connectionService?.repository?.disconnect()
        val context = getApplication<Application>().applicationContext
        val intent = Intent(context, BluetoothConnectionService::class.java)
        context.stopService(intent)
    }

    fun toggleMockMode(enabled: Boolean) {
        connectionService?.repository?.toggleMockMode(enabled)
    }

    // --- Control methods passed down to repository ---

    fun setAmbientMode(controlOn: Boolean, level: Int, focusVoice: Boolean) {
        connectionService?.repository?.setAmbientMode(controlOn, level, focusVoice)
    }

    fun setEqualizerPreset(preset: EqPreset) {
        connectionService?.repository?.setEqualizerPreset(preset)
    }

    fun setEqualizerCustom(clearBass: Int, bands: List<Int>) {
        connectionService?.repository?.setEqualizerCustom(clearBass, bands)
    }

    fun setDsee(enabled: Boolean) {
        connectionService?.repository?.setDsee(enabled)
    }

    fun setAutoPowerOff(index: Int) {
        connectionService?.repository?.setAutoPowerOff(index)
    }

    fun setSpeakToChat(enabled: Boolean) {
        connectionService?.repository?.setSpeakToChat(enabled)
    }

    fun setAdaptiveVolume(enabled: Boolean) {
        connectionService?.repository?.setAdaptiveVolume(enabled)
    }

    override fun onCleared() {
        super.onCleared()
        val context = getApplication<Application>().applicationContext
        if (isReceiverRegistered) {
            try {
                context.unregisterReceiver(bluetoothReceiver)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to unregister bluetoothReceiver: ${e.message}")
            }
            isReceiverRegistered = false
        }
        if (isBound) {
            context.unbindService(serviceConnection)
            isBound = false
        }
    }
}
