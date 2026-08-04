package org.librecans.protocol

object ByteMagic {
    fun byteOrderSwap(num: Int): Int {
        return (num ushr 24) or
                ((num shl 8) and 0x00FF0000) or
                ((num ushr 8) and 0x0000FF00) or
                (num shl 24)
    }

    fun intToBytesBE(num: Int): ByteArray {
        return byteArrayOf(
            ((num ushr 24) and 0xFF).toByte(),
            ((num ushr 16) and 0xFF).toByte(),
            ((num ushr 8) and 0xFF).toByte(),
            (num and 0xFF).toByte()
        )
    }

    fun macStringToLong(str: String): Long {
        val trimmed = str.trim()
        if (trimmed.length != 17) {
            throw IllegalArgumentException("Invalid MAC address size ($trimmed): ${trimmed.length} != 17")
        }
        val parts = trimmed.split(':', '-')
        if (parts.size != 6) {
            throw IllegalArgumentException("Invalid MAC address format: $trimmed")
        }
        var result = 0L
        for (part in parts) {
            val value = part.toInt(16)
            result = (result shl 8) or (value and 0xFF).toLong()
        }
        return result
    }

    fun macBytesToString(addr: ByteArray): String {
        if (addr.size < 6) {
            throw IllegalArgumentException("MAC bytes must be at least 6 bytes")
        }
        val sb = StringBuilder()
        // C++: addr[MAC_ADDR_BYTES_LEN - 1 - i]
        for (i in 0 until 6) {
            val b = addr[5 - i].toInt() and 0xFF
            sb.append(String.format("%02x", b))
            sb.append(':')
        }
        if (sb.isNotEmpty()) {
            sb.setLength(sb.length - 1)
        }
        return sb.toString()
    }
}
