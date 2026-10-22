package org.librecans.protocol.decode

import org.librecans.protocol.event.DeviceEvent
import org.librecans.protocol.mdr.Inquired
import org.librecans.protocol.mdr.T1
import org.librecans.protocol.model.AutoPowerOff
import org.librecans.protocol.model.BatteryLevel
import org.librecans.protocol.model.BatterySlot
import org.librecans.protocol.model.ChargingState
import org.librecans.protocol.wire.PayloadReader

internal object PowerDecoder {
    fun decode(command: Int, r: PayloadReader): DeviceEvent? = when (command) {
        T1.POWER_RET_CAPABILITY -> capability(r)
        T1.POWER_RET_STATUS, T1.POWER_NTFY_STATUS -> battery(r)
        T1.POWER_RET_PARAM, T1.POWER_NTFY_PARAM -> autoPowerOff(r)
        else -> null
    }

    private fun capability(r: PayloadReader): DeviceEvent? = when (r.u8()) {
        Inquired.POWER_AUTO_POWER_OFF, Inquired.POWER_AUTO_POWER_OFF_WEARING ->
            DeviceEvent.AutoPowerOffOptions(r.list { u8() }.mapNotNull(AutoPowerOff::fromCode))
        else -> null
    }

    /**
     * Battery replies only carry one kind of battery, so they are emitted per slot.
     * A level of 0 on an earbud means it is not connected (e.g. resting in the case).
     */
    private fun battery(r: PayloadReader): DeviceEvent? = when (r.u8()) {
        Inquired.POWER_BATTERY, Inquired.POWER_BATTERY_THRESHOLD ->
            DeviceEvent.Battery(mapOf(BatterySlot.SINGLE to level(r)))
        Inquired.POWER_CRADLE_BATTERY, Inquired.POWER_CRADLE_BATTERY_THRESHOLD ->
            DeviceEvent.Battery(mapOf(BatterySlot.CASE to level(r)))
        Inquired.POWER_LEFT_RIGHT_BATTERY, Inquired.POWER_LR_BATTERY_THRESHOLD -> {
            val left = level(r)
            DeviceEvent.Battery(mapOf(BatterySlot.LEFT to left, BatterySlot.RIGHT to level(r)))
        }
        else -> null
    }

    private fun level(r: PayloadReader): BatteryLevel? {
        val percent = r.u8()
        val charging = ChargingState.fromCode(r.u8())
        return if (percent == 0) null else BatteryLevel(percent.coerceAtMost(100), charging)
    }

    private fun autoPowerOff(r: PayloadReader): DeviceEvent? {
        val wearing = when (r.u8()) {
            Inquired.POWER_AUTO_POWER_OFF -> false
            Inquired.POWER_AUTO_POWER_OFF_WEARING -> true
            else -> return null
        }
        val current = AutoPowerOff.fromCode(r.u8()) ?: return null
        val last = if (r.remaining > 0) r.u8() else current.code
        return DeviceEvent.AutoPowerOffChanged(current, last, wearing)
    }
}
