package org.librecans.protocol.session

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.librecans.protocol.event.DeviceEvent
import org.librecans.protocol.frame.DataType
import org.librecans.protocol.frame.Frame
import org.librecans.protocol.frame.FrameCodec
import org.librecans.protocol.frame.FrameParser
import org.librecans.protocol.payloadOf
import org.librecans.protocol.request.AudioRequests
import org.librecans.protocol.request.PlayRequests
import org.librecans.protocol.sim.InMemoryLink
import org.librecans.protocol.wire.toHex

class MdrSessionTest {
    /** The far end of the link, driven by the test. */
    private class Device(private val transport: ByteTransport) {
        private val parser = FrameParser { throw it }
        private val buffer = ByteArray(256)
        private val queue = ArrayDeque<Frame>()

        suspend fun next(): Frame {
            while (queue.isEmpty()) {
                val count = transport.read(buffer)
                check(count >= 0) { "Link closed" }
                queue += parser.feed(buffer, 0, count)
            }
            return queue.removeFirst()
        }

        suspend fun send(frame: Frame) = transport.write(FrameCodec.encode(frame))
    }

    private fun TestScope.session(config: SessionConfig = SessionConfig()): Pair<MdrSession, Device> {
        val link = InMemoryLink()
        val session = MdrSession(link.app, backgroundScope, config)
        session.start()
        return session to Device(link.device)
    }

    @Test
    fun `a request completes once acknowledged and the sequence follows the ack`() = runTest {
        val (session, device) = session()

        val first = async { session.send(AudioRequests.codec()) }
        val frame = device.next()
        assertThat(frame.type).isEqualTo(DataType.DATA_MDR)
        assertThat(frame.sequence).isEqualTo(0)
        assertThat(frame.payload.toHex()).isEqualTo("12 02")
        device.send(Frame.ack(1))
        assertThat(first.await()).isTrue()

        val second = async { session.send(AudioRequests.upscaling()) }
        assertThat(device.next().sequence).isEqualTo(1)
        device.send(Frame.ack(0))
        assertThat(second.await()).isTrue()
    }

    @Test
    fun `unacknowledged requests are retried with the same sequence then given up`() = runTest {
        val (session, device) = session(SessionConfig(ackTimeoutMillis = 500, maxAttempts = 3))

        val result = async { session.send(AudioRequests.codec()) }
        val attempts = List(3) { device.next() }

        assertThat(attempts.map { it.sequence }.distinct()).containsExactly(0)
        assertThat(result.await()).isFalse()
    }

    @Test
    fun `a late ack on retry still succeeds`() = runTest {
        val (session, device) = session(SessionConfig(ackTimeoutMillis = 500, maxAttempts = 3))

        val result = async { session.send(AudioRequests.codec()) }
        device.next()
        device.next()
        device.send(Frame.ack(1))

        assertThat(result.await()).isTrue()
    }

    @Test
    fun `requests are sent one at a time`() = runTest {
        val (session, device) = session()

        session.post(AudioRequests.codec())
        session.post(AudioRequests.upscaling())
        val first = device.next()
        runCurrent()

        device.send(Frame.ack(1))
        val second = device.next()

        assertThat(first.payload.toHex()).isEqualTo("12 02")
        assertThat(second.payload.toHex()).isEqualTo("e6 01")
    }

    @Test
    fun `incoming data is acknowledged with the inverted sequence and decoded`() = runTest {
        val (session, device) = session()
        val event = async(start = CoroutineStart.UNDISPATCHED) { session.events.first() }

        device.send(Frame(DataType.DATA_MDR, 1, payloadOf(0xA9, 0x20, 0x07)))

        val ack = device.next()
        assertThat(ack.type).isEqualTo(DataType.ACK)
        assertThat(ack.sequence).isEqualTo(0)
        assertThat(event.await()).isEqualTo(DeviceEvent.VolumeChanged(7))
    }

    @Test
    fun `undecodable data is still acknowledged`() = runTest {
        val (_, device) = session()

        device.send(Frame(DataType.DATA_MDR, 0, payloadOf(0x99, 0x42)))

        assertThat(device.next()).isEqualTo(Frame.ack(1))
    }

    @Test
    fun `coalesced posts only send the newest value`() = runTest {
        val (session, device) = session()

        session.post(AudioRequests.codec())
        (1..10).forEach { session.post(PlayRequests.setVolume(it), coalesceKey = "volume") }
        device.next()
        device.send(Frame.ack(1))

        assertThat(device.next().payload.toHex()).isEqualTo("a8 20 0a")
    }

    @Test
    fun `query waits for the matching event`() = runTest {
        val (session, device) = session()

        val reply = async { session.query(PlayRequests.volume(), 1_000) { it as? DeviceEvent.VolumeChanged } }
        device.next()
        device.send(Frame.ack(1))
        device.send(Frame(DataType.DATA_MDR, 0, payloadOf(0xA7, 0x20, 0x11)))

        assertThat(reply.await()).isEqualTo(DeviceEvent.VolumeChanged(17))
    }

    @Test
    fun `query times out when no reply arrives`() = runTest {
        val (session, device) = session()

        val reply = async { session.query(PlayRequests.volume(), 1_000) { it as? DeviceEvent.VolumeChanged } }
        device.next()
        device.send(Frame.ack(1))

        assertThat(reply.await()).isNull()
    }

    @Test
    fun `the session closes when the link ends and fails pending work`() = runTest {
        val link = InMemoryLink()
        val session = MdrSession(link.app, backgroundScope)
        session.start()

        val pending = async { session.send(AudioRequests.codec()) }
        runCurrent()
        link.device.close()

        assertThat(session.closed.await()).isNull()
        assertThat(pending.await()).isFalse()
        assertThat(session.send(AudioRequests.codec())).isFalse()
    }

    @Test
    fun `closing the session closes the link`() = runTest {
        val (session, device) = session()
        session.close()

        val error = runCatching { device.next() }.exceptionOrNull()
        assertThat(error).isInstanceOf(IllegalStateException::class.java)
    }
}

