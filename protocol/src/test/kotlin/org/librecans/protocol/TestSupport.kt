package org.librecans.protocol

import org.librecans.protocol.decode.MdrDecoder
import org.librecans.protocol.event.DeviceEvent
import org.librecans.protocol.frame.Frame
import org.librecans.protocol.frame.FrameParser
import org.librecans.protocol.mdr.Table
import org.librecans.protocol.wire.hexBytes

fun frameOf(hex: String): Frame = FrameParser { throw it }.feed(hexBytes(hex)).single()

fun eventOf(hex: String): DeviceEvent? {
    val frame = frameOf(hex)
    return MdrDecoder.decode(requireNotNull(Table.of(frame.type)), frame.payload)
}

fun payloadOf(vararg bytes: Int): ByteArray = ByteArray(bytes.size) { bytes[it].toByte() }
