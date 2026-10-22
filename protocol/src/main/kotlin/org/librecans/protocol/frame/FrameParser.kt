package org.librecans.protocol.frame

import java.io.ByteArrayOutputStream

/**
 * Incremental parser for a raw RFCOMM byte stream. Bytes outside of frames are dropped,
 * a START inside a frame restarts it, and corrupt frames are reported and skipped.
 */
class FrameParser(private val onError: (FrameException) -> Unit = {}) {
    private val body = ByteArrayOutputStream(64)
    private var inFrame = false
    private var escaping = false

    fun feed(bytes: ByteArray, offset: Int = 0, length: Int = bytes.size - offset): List<Frame> {
        val frames = ArrayList<Frame>(1)
        for (i in offset until offset + length) {
            val b = bytes[i]
            when {
                b == FrameCodec.START -> startFrame()
                !inFrame -> Unit
                b == FrameCodec.END -> finishFrame()?.let(frames::add)
                escaping -> {
                    body.write((b.toInt() or FrameCodec.ESCAPE_MASK.inv()) and 0xFF)
                    escaping = false
                }
                b == FrameCodec.ESCAPE -> escaping = true
                else -> body.write(b.toInt())
            }
            if (inFrame && body.size() > FrameCodec.MAX_FRAME_SIZE) {
                reset()
                onError(FrameException("Frame exceeded ${FrameCodec.MAX_FRAME_SIZE} bytes"))
            }
        }
        return frames
    }

    private fun startFrame() {
        if (inFrame && body.size() > 0) onError(FrameException("Frame restarted before end marker"))
        reset()
        inFrame = true
    }

    private fun finishFrame(): Frame? {
        val bytes = body.toByteArray()
        val danglingEscape = escaping
        reset()
        if (danglingEscape) {
            onError(FrameException("Escape byte before end marker"))
            return null
        }
        return try {
            FrameCodec.decodeBody(bytes)
        } catch (e: FrameException) {
            onError(e)
            null
        }
    }

    private fun reset() {
        body.reset()
        inFrame = false
        escaping = false
    }
}
