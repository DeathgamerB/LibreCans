package org.librecans.protocol.decode

import org.librecans.protocol.event.DeviceEvent
import org.librecans.protocol.mdr.Inquired
import org.librecans.protocol.mdr.T2
import org.librecans.protocol.mdr.Table
import org.librecans.protocol.mdr.isOn
import org.librecans.protocol.wire.PayloadReader

internal object Table2Decoder {
    fun decode(command: Int, r: PayloadReader): DeviceEvent? = when (command) {
        T2.CONNECT_RET_SUPPORT_FUNCTION -> ConnectDecoder.supportedFeatures(Table.TWO, r)
        T2.VOICE_GUIDANCE_RET_PARAM, T2.VOICE_GUIDANCE_NTFY_PARAM -> voiceGuidance(r)
        else -> null
    }

    /** Types 0x00..0x03 are on/off variants that differ only in language switching support. */
    private fun voiceGuidance(r: PayloadReader): DeviceEvent? = when (r.u8()) {
        Inquired.VOICE_GUIDANCE_VOLUME -> DeviceEvent.VoiceGuidanceVolumeChanged(r.s8())
        in 0x00..0x03 -> DeviceEvent.VoiceGuidanceChanged(isOn(r.u8()))
        else -> null
    }
}
