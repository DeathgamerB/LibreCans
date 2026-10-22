package org.librecans.protocol.decode

import org.librecans.protocol.event.DeviceEvent
import org.librecans.protocol.mdr.Inquired
import org.librecans.protocol.mdr.T1
import org.librecans.protocol.model.AudioCodec
import org.librecans.protocol.wire.PayloadReader

internal object CommonDecoder {
    fun decode(command: Int, r: PayloadReader): DeviceEvent? {
        if (command != T1.COMMON_RET_STATUS && command != T1.COMMON_NTFY_STATUS) return null
        return when (r.u8()) {
            Inquired.COMMON_AUDIO_CODEC -> DeviceEvent.Codec(AudioCodec.fromCode(r.u8()))
            else -> null
        }
    }
}
