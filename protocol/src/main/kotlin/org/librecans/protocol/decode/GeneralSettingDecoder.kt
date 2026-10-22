package org.librecans.protocol.decode

import org.librecans.protocol.event.DeviceEvent
import org.librecans.protocol.mdr.T1
import org.librecans.protocol.mdr.isOn
import org.librecans.protocol.model.GeneralSetting
import org.librecans.protocol.wire.PayloadReader

/** Only boolean general settings are supported; list settings are ignored. */
internal object GeneralSettingDecoder {
    private const val BOOLEAN_TYPE = 0x00
    private const val ENUM_NAME = 0x01

    fun decode(command: Int, r: PayloadReader): DeviceEvent? {
        val id = r.u8()
        if (r.u8() != BOOLEAN_TYPE) return null
        return when (command) {
            T1.GENERAL_SETTING_RET_CAPABILITY -> {
                val titleIsKey = r.u8() == ENUM_NAME
                val title = r.prefixedString()
                val summary = if (r.remaining > 0) r.prefixedString().ifBlank { null } else null
                DeviceEvent.GeneralSettingDescribed(GeneralSetting(id, title, summary, titleIsKey))
            }
            T1.GENERAL_SETTING_RET_PARAM, T1.GENERAL_SETTING_NTFY_PARAM ->
                DeviceEvent.GeneralSettingChanged(id, isOn(r.u8()))
            else -> null
        }
    }
}
