package org.librecans.protocol.session

import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import org.librecans.protocol.decode.MdrDecoder
import org.librecans.protocol.event.DeviceEvent
import org.librecans.protocol.frame.DataType
import org.librecans.protocol.frame.Frame
import org.librecans.protocol.frame.FrameCodec
import org.librecans.protocol.frame.FrameParser
import org.librecans.protocol.mdr.MdrRequest
import org.librecans.protocol.mdr.Table

/**
 * Speaks MDR over a [ByteTransport]: one request in flight at a time, each retried until the
 * headphones acknowledge it, and every incoming data frame acknowledged and decoded into [events].
 */
class MdrSession(
    private val transport: ByteTransport,
    scope: CoroutineScope,
    private val config: SessionConfig = SessionConfig(),
    private val log: FrameLog = FrameLog.NONE,
) {
    private class Pending(@Volatile var request: MdrRequest, val coalesceKey: String?) {
        val acked = CompletableDeferred<Boolean>()
    }

    private val job = SupervisorJob(scope.coroutineContext[Job])
    private val sessionScope = CoroutineScope(scope.coroutineContext + job)
    private val outgoing = Channel<Pending>(Channel.UNLIMITED)
    private val acks = Channel<Int>(Channel.CONFLATED)
    private val coalescing = ConcurrentHashMap<String, Pending>()
    private val writeLock = Mutex()
    private val _events = MutableSharedFlow<DeviceEvent>(extraBufferCapacity = 256)
    private var sequence = 0

    val events: SharedFlow<DeviceEvent> = _events.asSharedFlow()

    /** Completes with null on a clean close, or the failure that ended the session. */
    val closed = CompletableDeferred<Throwable?>()

    fun start() {
        sessionScope.launch { readLoop() }
        sessionScope.launch { writeLoop() }
    }

    /** Queues [request] and suspends until it is acknowledged. Returns false if it never was. */
    suspend fun send(request: MdrRequest): Boolean = enqueue(request, null).acked.await()

    /**
     * Queues [request] without waiting. Requests sharing a [coalesceKey] that are still waiting
     * are replaced, so rapid slider updates only send the newest value.
     */
    fun post(request: MdrRequest, coalesceKey: String? = null) {
        enqueue(request, coalesceKey)
    }

    /** Sends [request] and waits for the first event [match] accepts. */
    suspend fun <T : Any> query(request: MdrRequest, timeoutMillis: Long, match: (DeviceEvent) -> T?): T? =
        coroutineScope {
            val answer = async(start = CoroutineStart.UNDISPATCHED) {
                withTimeoutOrNull(timeoutMillis) { events.mapNotNull(match).first() }
            }
            send(request)
            answer.await()
        }

    fun close() = shutdown(null)

    private fun enqueue(request: MdrRequest, coalesceKey: String?): Pending {
        if (coalesceKey == null) return Pending(request, null).also(::offer)
        var created: Pending? = null
        val pending = coalescing.compute(coalesceKey) { _, existing ->
            existing?.also { it.request = request } ?: Pending(request, coalesceKey).also { created = it }
        }!!
        created?.let(::offer)
        return pending
    }

    private fun offer(pending: Pending) {
        if (outgoing.trySend(pending).isFailure) pending.acked.complete(false)
    }

    private suspend fun writeLoop() {
        for (pending in outgoing) {
            pending.coalesceKey?.let { coalescing.remove(it, pending) }
            try {
                pending.acked.complete(deliver(pending.request))
            } finally {
                pending.acked.complete(false)
            }
        }
    }

    private suspend fun deliver(request: MdrRequest): Boolean {
        val frame = Frame(request.table.dataType, sequence, request.payload)
        do {
            val stale = acks.tryReceive().isSuccess
        } while (stale)
        repeat(config.maxAttempts) { attempt ->
            write(frame, if (attempt == 0) request.label else "${request.label} (retry $attempt)")
            val ack = withTimeoutOrNull(config.ackTimeoutMillis) {
                var received = acks.receive()
                while (received == frame.sequence) received = acks.receive()
                received
            }
            if (ack != null) {
                sequence = ack
                return true
            }
        }
        log.onError("No ACK for ${request.label} after ${config.maxAttempts} attempts")
        return false
    }

    private suspend fun readLoop() {
        val parser = FrameParser { log.onError("Dropped frame: ${it.message}") }
        val buffer = ByteArray(config.readBufferSize)
        try {
            while (true) {
                val count = transport.read(buffer)
                if (count < 0) break
                for (frame in parser.feed(buffer, 0, count)) handle(frame)
            }
            shutdown(null)
        } catch (e: Exception) {
            shutdown(e)
        }
    }

    private suspend fun handle(frame: Frame) {
        if (frame.type == DataType.ACK) {
            log.onFrame(Direction.IN, frame, null)
            acks.trySend(frame.sequence)
            return
        }
        val table = Table.of(frame.type)
        val event = table?.let { MdrDecoder.decode(it, frame.payload) }
        log.onFrame(Direction.IN, frame, event?.javaClass?.simpleName)
        write(Frame.ack(1 - frame.sequence), null)
        if (event != null) _events.emit(event)
    }

    private suspend fun write(frame: Frame, note: String?) {
        writeLock.withLock {
            log.onFrame(Direction.OUT, frame, note)
            transport.write(FrameCodec.encode(frame))
        }
    }

    private fun shutdown(cause: Throwable?) {
        if (!closed.complete(cause)) return
        outgoing.close()
        for (pending in generateSequence { outgoing.tryReceive().getOrNull() }) pending.acked.complete(false)
        runCatching { transport.close() }
        sessionScope.cancel()
    }
}
