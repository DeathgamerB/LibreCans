package org.librecans.protocol.decode

import org.librecans.protocol.event.DeviceEvent
import org.librecans.protocol.mdr.NcAsmLayout
import org.librecans.protocol.mdr.T1
import org.librecans.protocol.model.NoiseControl
import org.librecans.protocol.model.NoiseMode
import org.librecans.protocol.wire.PayloadReader

internal object NoiseControlDecoder {
    private const val CHANGED = 0x01
    private const val ON = 0x01
    private const val ASM = 0x01
    private const val VOICE = 0x01

    fun decode(command: Int, r: PayloadReader): DeviceEvent? {
        if (command != T1.NCASM_RET_PARAM && command != T1.NCASM_NTFY_PARAM) return null
        val payload = listOf(command) + List(r.remaining) { r.u8() }
        val type = payload.getOrNull(1) ?: return null
        val at = NcAsmLayout.indicesFor(type, payload.size) ?: return null
        if (payload.size < at.size) return null

        val enabled = payload[NcAsmLayout.EFFECT] == ON
        val mode = when {
            !enabled -> NoiseMode.OFF
            at.mode == null || payload[at.mode] == ASM -> NoiseMode.AMBIENT
            else -> NoiseMode.NOISE_CANCELLING
        }
        val control = NoiseControl(
            mode = mode,
            ambientLevel = payload[at.level].coerceIn(0, NoiseControl.MAX_AMBIENT_LEVEL),
            focusOnVoice = payload[at.ambientMode] == VOICE,
            inquiredType = type,
            template = payload,
        )
        return DeviceEvent.NoiseControlChanged(control, settled = payload[NcAsmLayout.CHANGE_STATUS] == CHANGED)
    }
}
