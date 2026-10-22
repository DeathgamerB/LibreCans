package org.librecans.protocol.mdr

/**
 * Byte positions inside NCASM_*_PARAM payloads. All layouts start with
 * command, inquired type, value-change status and the total on/off effect.
 */
internal object NcAsmLayout {
    const val CHANGE_STATUS = 2
    const val EFFECT = 3

    data class Indices(val mode: Int?, val ambientMode: Int, val level: Int, val size: Int)

    fun indicesFor(type: Int, size: Int): Indices? = when (type) {
        0x17 -> Indices(mode = 4, ambientMode = 5, level = 6, size = 7)
        0x19 -> Indices(mode = 4, ambientMode = 5, level = 6, size = 9)
        0x15, 0x16 ->
            if (size >= 8) Indices(mode = 4, ambientMode = 6, level = 7, size = 8)
            else Indices(mode = 4, ambientMode = 5, level = 6, size = 7)
        Inquired.NCASM_ASM_SEAMLESS -> Indices(mode = null, ambientMode = 4, level = 5, size = 6)
        else -> null
    }

    fun defaultTemplate(type: Int): List<Int> {
        val indices = requireNotNull(indicesFor(type, size = 0)) { "Unsupported NC/ASM type $type" }
        return List(indices.size) { 0 }.toMutableList().also {
            it[0] = T1.NCASM_SET_PARAM
            it[1] = type
        }
    }
}
