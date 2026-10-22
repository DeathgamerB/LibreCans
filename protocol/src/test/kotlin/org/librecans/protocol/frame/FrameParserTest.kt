package org.librecans.protocol.frame

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.librecans.protocol.Xm5Capture
import org.librecans.protocol.wire.hexBytes

class FrameParserTest {
    private val errors = mutableListOf<FrameException>()
    private val parser = FrameParser { errors += it }

    @Test
    fun `parses frames split across arbitrary reads`() {
        val stream = hexBytes(Xm5Capture.NOISE_CONTROL + Xm5Capture.ACK_SEQ_1 + Xm5Capture.SUPPORT_TABLE2)

        val frames = stream.flatMap { parser.feed(byteArrayOf(it)) }

        assertThat(frames.map { it.type }).containsExactly(DataType.DATA_MDR, DataType.ACK, DataType.DATA_MDR_NO2).inOrder()
        assertThat(errors).isEmpty()
    }

    @Test
    fun `unescapes bytes inside the frame`() {
        val frame = parser.feed(hexBytes(Xm5Capture.SUPPORT_TABLE2)).single()

        assertThat(frame.payload[4].toInt()).isEqualTo(0x3d)
    }

    @Test
    fun `ignores noise between frames`() {
        val stream = hexBytes("00ff12" + Xm5Capture.CODEC + "9988" + Xm5Capture.VOLUME)

        assertThat(parser.feed(stream)).hasSize(2)
        assertThat(errors).isEmpty()
    }

    @Test
    fun `drops a corrupt frame and recovers on the next one`() {
        val corrupt = Xm5Capture.CODEC.replace("273c", "283c")

        val frames = parser.feed(hexBytes(corrupt + Xm5Capture.VOLUME))

        assertThat(frames).hasSize(1)
        assertThat(errors).hasSize(1)
    }

    @Test
    fun `a new start marker restarts the frame`() {
        val truncated = Xm5Capture.CODEC.dropLast(6)

        val frames = parser.feed(hexBytes(truncated + Xm5Capture.VOLUME))

        assertThat(frames).hasSize(1)
        assertThat(errors).hasSize(1)
    }

    @Test
    fun `discards frames that never end`() {
        val endless = ByteArray(FrameCodec.MAX_FRAME_SIZE + 10).also { it[0] = FrameCodec.START }

        assertThat(parser.feed(endless)).isEmpty()
        assertThat(errors).isNotEmpty()
        assertThat(parser.feed(hexBytes(Xm5Capture.VOLUME))).hasSize(1)
    }
}
