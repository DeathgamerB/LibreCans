package org.librecans.protocol.sim

import java.io.IOException
import kotlinx.coroutines.channels.Channel
import org.librecans.protocol.session.ByteTransport

/** Two connected in-memory transports, standing in for an RFCOMM socket pair. */
class InMemoryLink {
    private val toDevice = Channel<ByteArray>(Channel.UNLIMITED)
    private val toApp = Channel<ByteArray>(Channel.UNLIMITED)

    val app: ByteTransport = Endpoint(inbox = toApp, outbox = toDevice)
    val device: ByteTransport = Endpoint(inbox = toDevice, outbox = toApp)

    private class Endpoint(
        private val inbox: Channel<ByteArray>,
        private val outbox: Channel<ByteArray>,
    ) : ByteTransport {
        private var current = ByteArray(0)
        private var offset = 0

        override suspend fun read(buffer: ByteArray): Int {
            if (offset >= current.size) {
                current = inbox.receiveCatching().getOrNull() ?: return -1
                offset = 0
            }
            val count = minOf(buffer.size, current.size - offset)
            current.copyInto(buffer, 0, offset, offset + count)
            offset += count
            return count
        }

        override suspend fun write(bytes: ByteArray) {
            if (outbox.trySend(bytes.copyOf()).isFailure) throw IOException("Link closed")
        }

        override fun close() {
            outbox.close()
            inbox.cancel()
        }
    }
}
