package org.librecans.protocol.decode

import org.librecans.protocol.event.DeviceEvent
import org.librecans.protocol.mdr.Inquired
import org.librecans.protocol.mdr.T1
import org.librecans.protocol.mdr.isOn
import org.librecans.protocol.model.SpeakToChatSensitivity
import org.librecans.protocol.model.SpeakToChatTimeout
import org.librecans.protocol.wire.PayloadReader

internal object SystemDecoder {
    fun decode(command: Int, r: PayloadReader): DeviceEvent? {
        val type = r.u8()
        return when (command) {
            T1.SYSTEM_RET_CAPABILITY ->
                if (type == Inquired.SYSTEM_ASSIGNABLE) DeviceEvent.TouchCapability(AssignableCodec.readKeys(r)) else null
            T1.SYSTEM_RET_PARAM, T1.SYSTEM_NTFY_PARAM -> param(type, r)
            T1.SYSTEM_RET_EXT_PARAM, T1.SYSTEM_NTFY_EXT_PARAM -> extParam(type, r)
            else -> null
        }
    }

    private fun param(type: Int, r: PayloadReader): DeviceEvent? = when (type) {
        Inquired.SYSTEM_ASSIGNABLE -> DeviceEvent.TouchPresetsChanged(r.list { u8() })
        Inquired.SYSTEM_PLAYBACK_BY_WEARING -> DeviceEvent.PauseWhenRemovedChanged(isOn(r.u8()))
        Inquired.SYSTEM_SMART_TALKING -> DeviceEvent.SpeakToChatChanged(isOn(r.u8()))
        Inquired.SYSTEM_HEAD_GESTURE -> DeviceEvent.HeadGesturesChanged(isOn(r.u8()))
        Inquired.SYSTEM_AUTO_VOLUME -> DeviceEvent.AdaptiveVolumeChanged(isOn(r.u8()))
        else -> null
    }

    private fun extParam(type: Int, r: PayloadReader): DeviceEvent? = when (type) {
        Inquired.SYSTEM_ASSIGNABLE -> DeviceEvent.TouchCustomizations(r.list { AssignableCodec.readPreset(this) })
        Inquired.SYSTEM_SMART_TALKING -> DeviceEvent.SpeakToChatConfigChanged(
            SpeakToChatSensitivity.fromCode(r.u8()),
            SpeakToChatTimeout.fromCode(r.u8()),
        )
        else -> null
    }
}
