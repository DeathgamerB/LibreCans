package org.librecans.protocol.session

import org.librecans.protocol.frame.Frame

data class SessionConfig(
    val ackTimeoutMillis: Long = 1_000,
    val maxAttempts: Int = 3,
    val readBufferSize: Int = 1_024,
)

enum class Direction { OUT, IN }

/** Receives every frame that crosses the wire, for diagnostics. */
fun interface FrameLog {
    fun onFrame(direction: Direction, frame: Frame, note: String?)

    fun onError(message: String) = Unit

    companion object {
        val NONE = FrameLog { _, _, _ -> }
    }
}
