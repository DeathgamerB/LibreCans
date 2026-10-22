package org.librecans.protocol.frame

import java.io.ByteArrayOutputStream

class FrameException(message: String) : Exception(message)

/**
 * Wire format: START | escape(type, seq, len32BE, payload, checksum) | END.
 * The checksum is the byte sum of everything between the markers; START, END and ESCAPE
 * inside the body are sent as ESCAPE followed by the byte masked with [ESCAPE_MASK].
 */
object FrameCodec {
    const val START: Byte = 0x3E
    const val END: Byte = 0x3C
    const val ESCAPE: Byte = 0x3D
    const val ESCAPE_MASK: Int = 0xEF
    const val MAX_FRAME_SIZE: Int = 2048
    private const val HEADER_SIZE = 6

    fun encode(frame: Frame): ByteArray {
        val size = frame.payload.size
        val body = ByteArray(HEADER_SIZE + size + 1)
        body[0] = frame.type.code.toByte()
        body[1] = frame.sequence.toByte()
        body[2] = (size ushr 24).toByte()
        body[3] = (size ushr 16).toByte()
        body[4] = (size ushr 8).toByte()
        body[5] = size.toByte()
        frame.payload.copyInto(body, HEADER_SIZE)
        body[body.lastIndex] = checksum(body, 0, body.lastIndex)

        val out = ByteArrayOutputStream(body.size + 8)
        out.write(START.toInt())
        for (b in body) {
            if (isSpecial(b)) {
                out.write(ESCAPE.toInt())
                out.write(b.toInt() and ESCAPE_MASK)
            } else {
                out.write(b.toInt())
            }
        }
        out.write(END.toInt())
        val encoded = out.toByteArray()
        if (encoded.size > MAX_FRAME_SIZE) throw FrameException("Frame exceeds $MAX_FRAME_SIZE bytes")
        return encoded
    }

    /** Decodes an already unescaped body (no markers). */
    fun decodeBody(body: ByteArray): Frame {
        if (body.size < HEADER_SIZE + 1) throw FrameException("Frame too short: ${body.size}")
        val type = DataType.fromCode(body[0].toInt() and 0xFF)
            ?: throw FrameException("Unknown data type 0x%02x".format(body[0]))
        val size = ((body[2].toInt() and 0xFF) shl 24) or ((body[3].toInt() and 0xFF) shl 16) or
            ((body[4].toInt() and 0xFF) shl 8) or (body[5].toInt() and 0xFF)
        if (size < 0 || HEADER_SIZE + size + 1 != body.size) {
            throw FrameException("Declared size $size does not match frame of ${body.size} bytes")
        }
        val expected = checksum(body, 0, body.lastIndex)
        if (expected != body.last()) {
            throw FrameException("Checksum mismatch: 0x%02x != 0x%02x".format(body.last(), expected))
        }
        return Frame(type, body[1].toInt() and 0xFF, body.copyOfRange(HEADER_SIZE, HEADER_SIZE + size))
    }

    fun checksum(bytes: ByteArray, from: Int, until: Int): Byte {
        var sum = 0
        for (i in from until until) sum += bytes[i].toInt() and 0xFF
        return sum.toByte()
    }

    internal fun isSpecial(b: Byte): Boolean = b == START || b == END || b == ESCAPE
}
