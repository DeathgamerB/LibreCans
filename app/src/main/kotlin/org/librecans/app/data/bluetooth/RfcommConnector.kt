package org.librecans.app.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import java.io.IOException
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import org.librecans.app.data.connection.ConnectException
import org.librecans.app.data.connection.FailureReason
import org.librecans.protocol.session.ByteTransport

/**
 * Opens the Sony RFCOMM channel. Tries the service records the headphones advertise first and
 * retries a few times, since the channel is often refused for a moment right after the audio link
 * comes up.
 */
@SuppressLint("MissingPermission")
class RfcommConnector(
    private val context: Context,
    private val connectTimeoutMillis: Long = 8_000,
    private val rounds: Int = 3,
    private val retryDelayMillis: Long = 1_500,
) {
    suspend fun open(address: String, onAttempt: (Int) -> Unit = {}): ByteTransport {
        if (!BluetoothPermissions.granted(context)) throw ConnectException(FailureReason.NO_PERMISSION)
        val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
            ?: throw ConnectException(FailureReason.BLUETOOTH_UNAVAILABLE)
        if (!adapter.isEnabled) throw ConnectException(FailureReason.BLUETOOTH_OFF)
        val device = runCatching { adapter.getRemoteDevice(address) }.getOrNull()
            ?: throw ConnectException(FailureReason.NOT_FOUND)

        val services = SonyDevices.serviceOrder(device.uuids?.map { it.uuid }.orEmpty())
        var lastError: IOException? = null
        for (round in 1..rounds) {
            onAttempt(round)
            for (uuid in services) {
                try {
                    return RfcommTransport(connect(device, uuid))
                } catch (e: IOException) {
                    lastError = e
                }
            }
            if (round < rounds) delay(retryDelayMillis)
        }
        throw ConnectException(FailureReason.CONNECTION_FAILED, lastError)
    }

    /** BluetoothSocket.connect blocks and ignores interrupts, so a watchdog closes it instead. */
    private suspend fun connect(device: BluetoothDevice, uuid: UUID): BluetoothSocket = coroutineScope {
        val socket = device.createRfcommSocketToServiceRecord(uuid)
        val connected = AtomicBoolean(false)
        val watchdog = launch(Dispatchers.IO) {
            try {
                delay(connectTimeoutMillis)
            } finally {
                if (!connected.get()) runCatching { socket.close() }
            }
        }
        try {
            runInterruptible(Dispatchers.IO) { socket.connect() }
            connected.set(true)
            socket
        } catch (e: IOException) {
            runCatching { socket.close() }
            throw e
        } finally {
            watchdog.cancel()
        }
    }
}
