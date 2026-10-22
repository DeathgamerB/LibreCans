package org.librecans.app.data.diagnostics

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.librecans.protocol.frame.DataType
import org.librecans.protocol.frame.Frame
import org.librecans.protocol.session.Direction
import org.librecans.protocol.session.FrameLog
import org.librecans.protocol.wire.toHex

data class LogEntry(val timeMillis: Long, val kind: Kind, val text: String) {
    enum class Kind { SENT, RECEIVED, ACK, EVENT, ERROR }
}

/**
 * In-memory protocol log, opt-in and never written to disk unless the user shares it.
 * Old entries are dropped once [capacity] is reached.
 */
class DiagnosticsLog(private val capacity: Int = 2_000, private val clock: () -> Long = System::currentTimeMillis) : FrameLog {
    private val _entries = MutableStateFlow<List<LogEntry>>(emptyList())
    val entries: StateFlow<List<LogEntry>> = _entries.asStateFlow()

    @Volatile
    var enabled: Boolean = false
        set(value) {
            field = value
            if (!value) clear()
        }

    override fun onFrame(direction: Direction, frame: Frame, note: String?) {
        if (!enabled) return
        val kind = when {
            frame.type == DataType.ACK -> LogEntry.Kind.ACK
            direction == Direction.OUT -> LogEntry.Kind.SENT
            else -> LogEntry.Kind.RECEIVED
        }
        val header = "${frame.type} #${frame.sequence}"
        val body = if (frame.payload.isEmpty()) "" else " ${frame.payload.toHex()}"
        append(kind, header + body + (note?.let { "  ($it)" } ?: ""))
    }

    override fun onError(message: String) = append(LogEntry.Kind.ERROR, message)

    fun event(message: String) = append(LogEntry.Kind.EVENT, message)

    fun clear() = _entries.update { emptyList() }

    fun export(): String = entries.value.joinToString("\n") { "${it.timeMillis} ${it.kind} ${it.text}" }

    private fun append(kind: LogEntry.Kind, text: String) {
        if (!enabled) return
        _entries.update { (it + LogEntry(clock(), kind, text)).takeLast(capacity) }
    }
}
