package org.librecans.protocol.request

import org.librecans.protocol.mdr.Inquired
import org.librecans.protocol.mdr.MdrRequest
import org.librecans.protocol.mdr.T1
import org.librecans.protocol.mdr.t1
import org.librecans.protocol.model.AutoPowerOff

object PowerRequests {
    private const val USER_POWER_OFF = 0x01

    fun battery(inquiredType: Int): MdrRequest = t1("battery", T1.POWER_GET_STATUS, inquiredType)

    fun autoPowerOffOptions(wearingDetection: Boolean): MdrRequest =
        t1("autoPowerOffOptions", T1.POWER_GET_CAPABILITY, autoPowerOffType(wearingDetection))

    fun autoPowerOff(wearingDetection: Boolean): MdrRequest =
        t1("autoPowerOff", T1.POWER_GET_PARAM, autoPowerOffType(wearingDetection))

    /** [lastSelected] is the timer to restore when "when removed" is chosen again. */
    fun setAutoPowerOff(wearingDetection: Boolean, value: AutoPowerOff, lastSelected: Int): MdrRequest =
        t1("setAutoPowerOff", T1.POWER_SET_PARAM, autoPowerOffType(wearingDetection), value.code, lastSelected)

    fun powerOff(): MdrRequest = t1("powerOff", T1.POWER_SET_STATUS, Inquired.POWER_POWER_OFF, USER_POWER_OFF)

    private fun autoPowerOffType(wearingDetection: Boolean) =
        if (wearingDetection) Inquired.POWER_AUTO_POWER_OFF_WEARING else Inquired.POWER_AUTO_POWER_OFF
}
