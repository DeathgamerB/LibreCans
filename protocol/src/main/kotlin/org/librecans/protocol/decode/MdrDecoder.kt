package org.librecans.protocol.decode

import org.librecans.protocol.event.DeviceEvent
import org.librecans.protocol.mdr.Table
import org.librecans.protocol.wire.MalformedPayloadException
import org.librecans.protocol.wire.PayloadReader

/** Turns a RET/NTFY payload into a [DeviceEvent]. Unknown or malformed payloads yield null. */
object MdrDecoder {
    fun decode(table: Table, payload: ByteArray): DeviceEvent? {
        if (payload.isEmpty()) return null
        val command = payload[0].toInt() and 0xFF
        val reader = PayloadReader(payload, start = 1)
        return try {
            when (table) {
                Table.ONE -> decodeTable1(command, reader)
                Table.TWO -> Table2Decoder.decode(command, reader)
            }
        } catch (_: MalformedPayloadException) {
            null
        }
    }

    private fun decodeTable1(command: Int, reader: PayloadReader): DeviceEvent? = when (command) {
        in 0x00..0x0F -> ConnectDecoder.decode(command, reader)
        in 0x10..0x1F -> CommonDecoder.decode(command, reader)
        in 0x20..0x2F -> PowerDecoder.decode(command, reader)
        in 0x50..0x5F -> EqualizerDecoder.decode(command, reader)
        in 0x60..0x6F -> NoiseControlDecoder.decode(command, reader)
        in 0xA0..0xAF -> PlayDecoder.decode(command, reader)
        in 0xD0..0xDF -> GeneralSettingDecoder.decode(command, reader)
        in 0xE0..0xEF -> AudioDecoder.decode(command, reader)
        in 0xF0..0xFF -> SystemDecoder.decode(command, reader)
        else -> null
    }
}
