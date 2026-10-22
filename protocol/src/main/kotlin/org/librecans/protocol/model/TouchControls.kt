package org.librecans.protocol.model

/** What a touch sensor (or button) does as a whole, e.g. "Playback control". */
object TouchPresets {
    const val AMBIENT_SOUND_CONTROL = 0x00
    const val VOLUME_CONTROL = 0x10
    const val PLAYBACK_CONTROL = 0x20
    const val TRACK_CONTROL = 0x21
    const val PLAYBACK_CONTROL_VOICE_ASSISTANT_LIMITATION = 0x22
    const val VOICE_RECOGNITION = 0x30
    const val GOOGLE_ASSISTANT = 0x31
    const val AMAZON_ALEXA = 0x32
    const val AMBIENT_SOUND_CONTROL_QUICK_ACCESS = 0x35
    const val QUICK_ACCESS = 0x36
    const val NO_FUNCTION = 0xFF

    fun controlsAmbientSound(preset: Int): Boolean =
        preset == AMBIENT_SOUND_CONTROL || preset == AMBIENT_SOUND_CONTROL_QUICK_ACCESS
}

/** The gesture performed on the sensor. */
object TouchActions {
    const val SINGLE_TAP = 0x00
    const val DOUBLE_TAP = 0x01
    const val TRIPLE_TAP = 0x02
    const val REPEAT_TAP = 0x03
    const val TAP_AND_HOLD = 0x10
    const val DOUBLE_TAP_AND_HOLD = 0x11
    const val LONG_PRESS_THEN_ACTIVATE = 0x21
    const val LONG_PRESS_DURING_ACTIVATE = 0x22
}

/** What a single gesture triggers. */
object TouchFunctions {
    const val NO_FUNCTION = 0x00
    const val NC_ASM_OFF = 0x01
    const val NC_ASM = 0x02
    const val NC_OFF = 0x03
    const val ASM_OFF = 0x04
    const val QUICK_ATTENTION = 0x10
    const val NC_OPTIMIZER = 0x11
    const val PLAY_PAUSE = 0x20
    const val NEXT_TRACK = 0x21
    const val PREVIOUS_TRACK = 0x22
    const val VOLUME_UP = 0x23
    const val VOLUME_DOWN = 0x24
    const val VOICE_RECOGNITION = 0x30
    const val GET_YOUR_NOTIFICATION = 0x31
    const val TALK_TO_GOOGLE_ASSISTANT = 0x32
    const val STOP_GOOGLE_ASSISTANT = 0x33
    const val VOICE_INPUT_AMAZON_ALEXA = 0x37
    const val SPOTIFY_TAP = 0x42
    const val QUICK_ACCESS_1 = 0x43
    const val QUICK_ACCESS_2 = 0x44

    /** Selectable cycles for the ambient sound control gesture. */
    val AMBIENT_CYCLES: List<Int> = listOf(NC_ASM_OFF, NC_ASM, NC_OFF, ASM_OFF)
}

object TouchKeys {
    const val LEFT = 0x00
    const val RIGHT = 0x01
}

data class GestureBinding(val action: Int, val function: Int)

data class PresetOption(val preset: Int, val gestures: List<GestureBinding>)

/** Capability of one key: which presets it can take and what each one does. */
data class TouchKey(val key: Int, val type: Int, val defaultPreset: Int, val options: List<PresetOption>)

data class TouchControls(
    /** Current preset per key, ordered left, right. */
    val presets: List<Int> = emptyList(),
    val keys: List<TouchKey> = emptyList(),
    /** Per-gesture overrides, notably which modes the ambient sound gesture cycles through. */
    val customizations: List<PresetOption> = emptyList(),
) {
    fun presetFor(key: Int): Int? = presets.getOrNull(key)

    fun ambientCycle(): Int? = customizations
        .firstOrNull { TouchPresets.controlsAmbientSound(it.preset) }
        ?.gestures?.firstOrNull { it.action == TouchActions.SINGLE_TAP }
        ?.function

    fun withPreset(key: Int, preset: Int): TouchControls {
        if (key !in presets.indices) return this
        return copy(presets = presets.toMutableList().also { it[key] = preset })
    }
}
