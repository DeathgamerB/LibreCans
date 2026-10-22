package org.librecans.protocol.request

import org.librecans.protocol.mdr.Inquired
import org.librecans.protocol.mdr.MdrRequest
import org.librecans.protocol.mdr.T1
import org.librecans.protocol.mdr.Table
import org.librecans.protocol.mdr.t1

object EqualizerRequests {
    private const val ENGLISH = 0x01

    fun capability(): MdrRequest = t1("eqCapability", T1.EQEBB_GET_CAPABILITY, Inquired.EQ_PRESET, ENGLISH)

    fun status(): MdrRequest = t1("eqStatus", T1.EQEBB_GET_STATUS, Inquired.EQ_PRESET)

    fun get(): MdrRequest = t1("equalizer", T1.EQEBB_GET_PARAM, Inquired.EQ_PRESET)

    fun setPreset(presetId: Int): MdrRequest = t1("setEqPreset", T1.EQEBB_SET_PARAM, Inquired.EQ_PRESET, presetId, 0)

    fun setSteps(presetId: Int, steps: List<Int>): MdrRequest {
        val bytes = listOf(T1.EQEBB_SET_PARAM, Inquired.EQ_PRESET, presetId, steps.size) + steps
        return MdrRequest(Table.ONE, ByteArray(bytes.size) { bytes[it].toByte() }, "setEqSteps")
    }
}
