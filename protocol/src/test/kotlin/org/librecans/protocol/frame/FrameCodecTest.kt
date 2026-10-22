package org.librecans.protocol.frame

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.librecans.protocol.Xm5Capture
import org.librecans.protocol.payloadOf
import org.librecans.protocol.wire.hexBytes
import org.librecans.protocol.wire.toHex

class FrameCodecTest {
    @Test
    fun `encodes acknowledgements exactly like the headphones`() {
        assertThat(FrameCodec.encode(Frame.ack(1)).toHex("")).isEqualTo(Xm5Capture.ACK_SEQ_1)
        assertThat(FrameCodec.encode(Frame.ack(0)).toHex("")).isEqualTo(Xm5Capture.ACK_SEQ_0)
    }

    @Test
    fun `re-encoding a captured frame reproduces the original bytes`() {
        listOf(Xm5Capture.NOISE_CONTROL, Xm5Capture.SUPPORT_TABLE2, Xm5Capture.EQ_PARAM, Xm5Capture.MODEL_NAME)
            .forEach { hex ->
                val bytes = hexBytes(hex)
                val frame = FrameCodec.decodeBody(unescape(bytes.copyOfRange(1, bytes.size - 1)))
                assertThat(FrameCodec.encode(frame).toHex("")).isEqualTo(hex)
            }
    }

    @Test
    fun `escapes marker bytes in payload and checksum`() {
        val frame = Frame(DataType.DATA_MDR, 0, payloadOf(0x3c, 0x3d, 0x3e, 0x41))
        val encoded = FrameCodec.encode(frame)

        assertThat(encoded.first()).isEqualTo(FrameCodec.START)
        assertThat(encoded.last()).isEqualTo(FrameCodec.END)
        val inner = encoded.copyOfRange(1, encoded.size - 1)
        assertThat(inner.none { it == FrameCodec.START || it == FrameCodec.END }).isTrue()
        assertThat(inner.toHex("")).contains("3d2c3d2d3d2e41")
    }

    @Test
    fun `round trips every byte value`() {
        val payload = ByteArray(256) { it.toByte() }
        val frame = Frame(DataType.DATA_MDR_NO2, 1, payload)

        val decoded = FrameParser().feed(FrameCodec.encode(frame)).single()

        assertThat(decoded).isEqualTo(frame)
    }

    @Test
    fun `rejects a bad checksum`() {
        val body = unescape(hexBytes(Xm5Capture.CODEC).let { it.copyOfRange(1, it.size - 1) })
        body[body.lastIndex] = (body.last() + 1).toByte()

        assertThrows(FrameException::class.java) { FrameCodec.decodeBody(body) }
    }

    @Test
    fun `rejects a size that disagrees with the frame`() {
        val body = payloadOf(0x0c, 0x00, 0x00, 0x00, 0x00, 0x05, 0x01, 0x12)
        assertThrows(FrameException::class.java) { FrameCodec.decodeBody(body) }
    }

    @Test
    fun `rejects frames over the size limit`() {
        val frame = Frame(DataType.DATA_MDR, 0, ByteArray(FrameCodec.MAX_FRAME_SIZE))
        assertThrows(FrameException::class.java) { FrameCodec.encode(frame) }
    }

    private fun unescape(bytes: ByteArray): ByteArray {
        val out = ArrayList<Byte>()
        var i = 0
        while (i < bytes.size) {
            if (bytes[i] == FrameCodec.ESCAPE) {
                out += (bytes[i + 1].toInt() or 0x10).toByte()
                i += 2
            } else {
                out += bytes[i++]
            }
        }
        return out.toByteArray()
    }
}
