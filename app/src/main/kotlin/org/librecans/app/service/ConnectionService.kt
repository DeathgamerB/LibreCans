package org.librecans.app.service

import android.annotation.SuppressLint
import android.app.Notification
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.librecans.app.LibreCansApp
import org.librecans.app.data.bluetooth.SonyDevices
import org.librecans.app.data.connection.ConnectionState
import org.librecans.app.data.connection.FailureReason
import org.librecans.app.ui.common.next

/**
 * Holds the RFCOMM link while the app is in the background, shows battery and noise mode in
 * an ongoing notification and reconnects when the headphones come back.
 */
class ConnectionService : LifecycleService() {
    private val container by lazy { (application as LibreCansApp).container }

    private val aclReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            val device = IntentCompat.getParcelableExtra(intent, BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                ?: return
            val state = container.headphones.connection.value
            val lost = state as? ConnectionState.Failed ?: return
            val target = lost.target ?: return
            if (lost.reason == FailureReason.CONNECTION_LOST && device.address == target.address) {
                container.headphones.connect(target.copy(kind = SonyDevices.kindOf(device.name)))
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        ConnectionNotification.createChannel(this)
        val headphones = container.headphones
        val initial = ConnectionNotification.build(this, headphones.connection.value, headphones.headphones.value)
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE else 0
        try {
            ServiceCompat.startForeground(this, ConnectionNotification.ID, initial, type)
        } catch (_: SecurityException) {
            stopSelf()
            return
        }

        ContextCompat.registerReceiver(
            this, aclReceiver, IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED), ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        lifecycleScope.launch {
            combine(headphones.connection, headphones.headphones, container.settings.settings) { connection, state, prefs ->
                Triple(connection, state, prefs.keepConnectedInBackground)
            }.collect { (connection, state, keep) ->
                if (!keep || !ServiceController.shouldRun(connection)) {
                    stopSelf()
                } else {
                    notify(ConnectionNotification.build(this@ConnectionService, connection, state))
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_DISCONNECT -> container.headphones.disconnect()
            ACTION_CYCLE_NOISE_MODE -> container.headphones.control {
                val current = state.value.noiseControl ?: return@control
                setNoiseControl(current.mode.next(), current.ambientLevel, current.focusOnVoice)
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(aclReceiver) }
        super.onDestroy()
    }

    @SuppressLint("MissingPermission")
    private fun notify(notification: Notification) {
        val manager = NotificationManagerCompat.from(this)
        if (manager.areNotificationsEnabled()) manager.notify(ConnectionNotification.ID, notification)
    }

    companion object {
        const val ACTION_DISCONNECT = "org.librecans.app.action.DISCONNECT"
        const val ACTION_CYCLE_NOISE_MODE = "org.librecans.app.action.CYCLE_NOISE_MODE"
    }
}
