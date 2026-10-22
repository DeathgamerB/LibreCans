package org.librecans.protocol.wire

class MalformedPayloadException(message: String) : Exception(message)

/** Bounds-checked cursor over an MDR payload. Arrays and strings carry a one byte length prefix. */
class PayloadReader(private val bytes: ByteArray, start: Int = 0) {
    var position: Int = start
        private set

    val remaining: Int get() = bytes.size - position

    fun u8(): Int {
        require(1)
        return bytes[position++].toInt() and 0xFF
    }

    fun s8(): Int {
        require(1)
        return bytes[position++].toInt()
    }

    fun u32(): Long {
        var value = 0L
        repeat(4) { value = (value shl 8) or u8().toLong() }
        return value
    }

    fun bytes(count: Int): ByteArray {
        require(count)
        return bytes.copyOfRange(position, position + count).also { position += count }
    }

    fun prefixedString(): String = String(bytes(u8()), Charsets.UTF_8)

    fun prefixedBytes(): ByteArray = bytes(u8())

    inline fun <T> list(read: PayloadReader.() -> T): List<T> = List(u8()) { read() }

    fun skip(count: Int) {
        require(count)
        position += count
    }

    private fun require(count: Int) {
        if (count < 0 || remaining < count) {
            throw MalformedPayloadException("Need $count bytes at $position, only $remaining left")
        }
    }
}
