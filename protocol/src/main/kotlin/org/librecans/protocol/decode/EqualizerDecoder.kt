package org.librecans.protocol.decode

import org.librecans.protocol.event.DeviceEvent
import org.librecans.protocol.mdr.Inquired
import org.librecans.protocol.mdr.T1
import org.librecans.protocol.mdr.isOn
import org.librecans.protocol.model.EqPreset
import org.librecans.protocol.model.EqualizerCapability
import org.librecans.protocol.wire.PayloadReader

internal object EqualizerDecoder {
    fun decode(command: Int, r: PayloadReader): DeviceEvent? {
        if (r.u8() != Inquired.EQ_PRESET) return null
        return when (command) {
            T1.EQEBB_RET_CAPABILITY -> capability(r)
            T1.EQEBB_RET_STATUS, T1.EQEBB_NTFY_STATUS -> DeviceEvent.EqualizerAvailability(isOn(r.u8()))
            T1.EQEBB_RET_PARAM, T1.EQEBB_NTFY_PARAM -> DeviceEvent.EqualizerChanged(r.u8(), r.list { u8() })
            else -> null
        }
    }

    private fun capability(r: PayloadReader): DeviceEvent {
        val bandCount = r.u8()
        val stepCount = r.u8()
        val presets = r.list { EqPreset(u8(), prefixedString().ifBlank { null }) }
        return DeviceEvent.EqualizerCapabilityChanged(EqualizerCapability(bandCount, stepCount, presets))
    }
}
