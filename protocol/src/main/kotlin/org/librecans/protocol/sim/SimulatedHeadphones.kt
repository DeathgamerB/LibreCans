package org.librecans.protocol.sim

import java.util.Collections
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.librecans.protocol.frame.DataType
import org.librecans.protocol.frame.Frame
import org.librecans.protocol.frame.FrameCodec
import org.librecans.protocol.frame.FrameParser
import org.librecans.protocol.mdr.Inquired
import org.librecans.protocol.mdr.T1
import org.librecans.protocol.mdr.Table
import org.librecans.protocol.session.ByteTransport

/**
 * The device end of an MDR link, behaving like a WF-1000XM5. Used by tests and by the
 * app's demo mode so that the complete protocol stack runs without hardware.
 */
class SimulatedHeadphones(
    private val transport: ByteTransport,
    val model: SimulatorModel = SimulatorModel(),
) {
    private val responder = SimulatorResponder(model)
    private val writeLock = Mutex()
    private var sequence = 0

    /** Every request payload received, in order. */
    val received: MutableList<Pair<Table, List<Int>>> = Collections.synchronizedList(mutableListOf())

    fun start(scope: CoroutineScope): Job = scope.launch {
        val parser = FrameParser()
        val buffer = ByteArray(512)
        while (isActive) {
            val count = transport.read(buffer)
            if (count < 0) break
            for (frame in parser.feed(buffer, 0, count)) {
                handle(frame)
                if (model.poweredOff) {
                    transport.close()
                    return@launch
                }
            }
        }
    }

    /** Emulates a tap on an earbud that switches NC/Ambient, as if done by the user. */
    suspend fun simulateAmbientTap() {
        with(model) {
            when {
                ncEnabled && !ncAmbient -> ncAmbient = true
                ncEnabled -> ncEnabled = false
                else -> {
                    ncEnabled = true
                    ncAmbient = false
                }
            }
        }
        notify(Table.ONE, responder.noiseControl(T1.NCASM_NTFY_PARAM, changed = true))
    }

    /** Drains the earbuds by [percent] and notifies the app. */
    suspend fun drainBattery(percent: Int = 1) {
        model.leftBattery = (model.leftBattery - percent).coerceAtLeast(1)
        model.rightBattery = (model.rightBattery - percent).coerceAtLeast(1)
        responder.battery(T1.POWER_NTFY_STATUS, Inquired.POWER_LR_BATTERY_THRESHOLD)?.let { notify(Table.ONE, it) }
    }

    /** Periodically drains the battery so live updates can be observed in demo mode. */
    fun startBatteryDrain(scope: CoroutineScope, intervalMillis: Long = 30_000): Job = scope.launch {
        while (isActive) {
            delay(intervalMillis)
            drainBattery()
        }
    }

    private suspend fun handle(frame: Frame) {
        if (frame.type == DataType.ACK) return
        val table = Table.of(frame.type) ?: return
        val payload = frame.payload.map { it.toInt() and 0xFF }
        received += table to payload
        write(Frame.ack(1 - frame.sequence))
        responder.respond(table, payload).forEach { notify(it.table, it.payload) }
    }

    private suspend fun notify(table: Table, payload: List<Int>) {
        val frame = Frame(table.dataType, sequence, ByteArray(payload.size) { payload[it].toByte() })
        sequence = 1 - sequence
        write(frame)
    }

    private suspend fun write(frame: Frame) = writeLock.withLock { transport.write(FrameCodec.encode(frame)) }
}
