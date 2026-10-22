package org.librecans.protocol.model

data class EqPreset(val id: Int, val name: String? = null)

data class EqualizerCapability(val bandCount: Int, val stepCount: Int, val presets: List<EqPreset>)

/** [steps] are the raw band values; use [levels] for signed dB-like steps. */
data class Equalizer(val presetId: Int, val steps: List<Int>, val available: Boolean = true) {
    val levels: EqLevels get() = EqualizerMath.levels(steps)
    val isCustom: Boolean get() = EqPresets.isCustom(presetId)
}

data class EqLevels(val clearBass: Int?, val bands: List<Int>)

object EqPresets {
    const val OFF = 0x00
    const val BRIGHT = 0x10
    const val EXCITED = 0x11
    const val MELLOW = 0x12
    const val RELAXED = 0x13
    const val VOCAL = 0x14
    const val TREBLE = 0x15
    const val BASS = 0x16
    const val SPEECH = 0x17
    const val CUSTOM = 0xA0
    const val USER_1 = 0xA1
    const val USER_2 = 0xA2

    /** Presets every V2 device with a preset EQ offers, used until the capability arrives. */
    val DEFAULTS: List<EqPreset> = listOf(OFF, BRIGHT, EXCITED, MELLOW, RELAXED, VOCAL, TREBLE, BASS, SPEECH, CUSTOM)
        .map { EqPreset(it) }

    fun isCustom(id: Int): Boolean = id in 0xA0..0xA5
}

/**
 * Six step devices send Clear Bass followed by five bands, centred on 10.
 * Ten band devices have no Clear Bass and are centred on 6.
 */
object EqualizerMath {
    fun offsetFor(stepCount: Int): Int = if (stepCount == 10) 6 else 10

    fun rangeFor(stepCount: Int): IntRange = offsetFor(stepCount).let { -it..it }

    fun hasClearBass(stepCount: Int): Boolean = stepCount == 6

    fun levels(steps: List<Int>): EqLevels {
        val offset = offsetFor(steps.size)
        val signed = steps.map { it - offset }
        return if (hasClearBass(steps.size)) EqLevels(signed.first(), signed.drop(1)) else EqLevels(null, signed)
    }

    fun steps(levels: EqLevels): List<Int> {
        val signed = listOfNotNull(levels.clearBass) + levels.bands
        val offset = offsetFor(signed.size)
        val range = rangeFor(signed.size)
        return signed.map { it.coerceIn(range) + offset }
    }
}
