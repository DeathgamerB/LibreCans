package org.librecans.protocol.request

import org.librecans.protocol.mdr.MdrRequest
import org.librecans.protocol.mdr.NcAsmLayout
import org.librecans.protocol.mdr.T1
import org.librecans.protocol.mdr.Table
import org.librecans.protocol.mdr.t1
import org.librecans.protocol.model.NoiseControl
import org.librecans.protocol.model.NoiseMode

object NoiseControlRequests {
    fun get(inquiredType: Int): MdrRequest = t1("noiseControl", T1.NCASM_GET_PARAM, inquiredType)

    /**
     * [settled] false marks an intermediate value while a slider is dragged; the headphones
     * apply it but treat the final settled write as the one to remember.
     */
    fun set(control: NoiseControl, settled: Boolean): MdrRequest {
        val type = control.inquiredType
        val base = control.template.takeIf { it.size >= 2 && it[1] == type } ?: NcAsmLayout.defaultTemplate(type)
        val at = requireNotNull(NcAsmLayout.indicesFor(type, base.size)) { "Unsupported NC/ASM type $type" }
        val bytes = base.toMutableList()
        bytes[0] = T1.NCASM_SET_PARAM
        bytes[NcAsmLayout.CHANGE_STATUS] = if (settled) 0x01 else 0x00
        bytes[NcAsmLayout.EFFECT] = if (control.mode == NoiseMode.OFF) 0x00 else 0x01
        at.mode?.let { bytes[it] = if (control.mode == NoiseMode.NOISE_CANCELLING) 0x00 else 0x01 }
        bytes[at.ambientMode] = if (control.focusOnVoice) 0x01 else 0x00
        bytes[at.level] = control.ambientLevel.coerceIn(0, NoiseControl.MAX_AMBIENT_LEVEL)
        return MdrRequest(Table.ONE, ByteArray(bytes.size) { bytes[it].toByte() }, "setNoiseControl")
    }
}
