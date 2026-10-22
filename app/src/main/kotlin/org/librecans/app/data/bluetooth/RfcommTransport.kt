package org.librecans.app.data.bluetooth

import android.bluetooth.BluetoothSocket
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.librecans.protocol.session.ByteTransport

class RfcommTransport(private val socket: BluetoothSocket) : ByteTransport {
    private val input = socket.inputStream
    private val output = socket.outputStream

    @Volatile
    private var closed = false

    override suspend fun read(buffer: ByteArray): Int = withContext(Dispatchers.IO) {
        try {
            input.read(buffer)
        } catch (e: IOException) {
            if (closed) -1 else throw e
        }
    }

    override suspend fun write(bytes: ByteArray) = withContext(Dispatchers.IO) {
        output.write(bytes)
        output.flush()
    }

    override fun close() {
        closed = true
        runCatching { socket.close() }
    }
}
