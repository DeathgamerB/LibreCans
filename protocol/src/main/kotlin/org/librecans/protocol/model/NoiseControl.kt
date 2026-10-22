package org.librecans.protocol.model

enum class NoiseMode { NOISE_CANCELLING, AMBIENT, OFF }

/**
 * [template] is the last parameter payload received from the device. Writes patch it so that
 * model specific bytes (NC value, adaptive settings) are echoed back untouched.
 */
data class NoiseControl(
    val mode: NoiseMode,
    val ambientLevel: Int,
    val focusOnVoice: Boolean,
    val inquiredType: Int,
    val template: List<Int> = emptyList(),
) {
    companion object {
        const val MIN_AMBIENT_LEVEL = 1
        const val MAX_AMBIENT_LEVEL = 20
        const val MIN_FOCUS_ON_VOICE_LEVEL = 2
    }
}
