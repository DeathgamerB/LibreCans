package org.librecans.app.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged

data class BluetoothSnapshot(
    val available: Boolean,
    val enabled: Boolean,
    val permitted: Boolean,
    val devices: List<PairedDevice>,
)

/** Paired Sony headphones and whether they are currently connected for audio. */
@SuppressLint("MissingPermission")
class BluetoothDevicesSource(private val context: Context) {
    private val adapter: BluetoothAdapter? get() = context.getSystemService(BluetoothManager::class.java)?.adapter
    private val proxies = ConcurrentHashMap<Int, BluetoothProfile>()

    fun observe(): Flow<BluetoothSnapshot> = callbackFlow {
        val listener = object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                proxies[profile] = proxy
                trySend(snapshot())
            }

            override fun onServiceDisconnected(profile: Int) {
                proxies.remove(profile)
                trySend(snapshot())
            }
        }
        if (BluetoothPermissions.granted(context)) {
            AUDIO_PROFILES.forEach { adapter?.getProfileProxy(context, listener, it) }
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                trySend(snapshot())
            }
        }
        val filter = IntentFilter().apply { EVENTS.forEach(::addAction) }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        trySend(snapshot())

        awaitClose {
            context.unregisterReceiver(receiver)
            proxies.forEach { (profile, proxy) -> runCatching { adapter?.closeProfileProxy(profile, proxy) } }
            proxies.clear()
        }
    }.conflate().distinctUntilChanged()

    fun snapshot(): BluetoothSnapshot {
        val adapter = adapter ?: return BluetoothSnapshot(available = false, enabled = false, permitted = false, devices = emptyList())
        val permitted = BluetoothPermissions.granted(context)
        if (!permitted || !adapter.isEnabled) {
            return BluetoothSnapshot(available = true, enabled = adapter.isEnabled, permitted = permitted, devices = emptyList())
        }
        val connected = connectedAudioAddresses()
        val devices = runCatching { adapter.bondedDevices.orEmpty() }.getOrDefault(emptySet())
            .filter { SonyDevices.isSony(it.name, it.uuids?.map { u -> u.uuid }.orEmpty()) }
            .map { PairedDevice(it.address, it.name ?: it.address, SonyDevices.kindOf(it.name), it.address in connected) }
            .sortedWith(compareByDescending<PairedDevice> { it.audioConnected }.thenBy { it.name })
        return BluetoothSnapshot(available = true, enabled = true, permitted = true, devices = devices)
    }

    private fun connectedAudioAddresses(): Set<String> = proxies.values
        .flatMap { runCatching { it.connectedDevices }.getOrDefault(emptyList()) }
        .mapTo(mutableSetOf()) { it.address }

    private companion object {
        val AUDIO_PROFILES = listOf(BluetoothProfile.A2DP, BluetoothProfile.HEADSET)
        val EVENTS = listOf(
            BluetoothAdapter.ACTION_STATE_CHANGED,
            BluetoothDevice.ACTION_BOND_STATE_CHANGED,
            BluetoothDevice.ACTION_ACL_CONNECTED,
            BluetoothDevice.ACTION_ACL_DISCONNECTED,
            BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED,
            BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED,
        )
    }
}
