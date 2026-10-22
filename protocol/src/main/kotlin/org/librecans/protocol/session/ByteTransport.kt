package org.librecans.protocol.session

/** A bidirectional byte stream, e.g. an RFCOMM socket or an in-memory link. */
interface ByteTransport {
    /** Reads into [buffer] and returns the byte count, or -1 once the stream has ended. */
    suspend fun read(buffer: ByteArray): Int

    suspend fun write(bytes: ByteArray)

    fun close()
}
