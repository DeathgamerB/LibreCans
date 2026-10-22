package org.librecans.protocol.wire

private const val DIGITS = "0123456789abcdef"

fun ByteArray.toHex(separator: String = " "): String = buildString(size * 3) {
    this@toHex.forEachIndexed { index, byte ->
        if (index > 0) append(separator)
        val v = byte.toInt() and 0xFF
        append(DIGITS[v ushr 4])
        append(DIGITS[v and 0x0F])
    }
}

/** Parses "0c 1a ff", "0c:1a:ff" or "0c1aff". */
fun hexBytes(text: String): ByteArray {
    val clean = text.filter { it.isLetterOrDigit() }
    require(clean.length % 2 == 0) { "Odd number of hex digits in '$text'" }
    return ByteArray(clean.length / 2) { i -> clean.substring(i * 2, i * 2 + 2).toInt(16).toByte() }
}

fun bytesOf(vararg values: Int): ByteArray = ByteArray(values.size) { values[it].toByte() }
