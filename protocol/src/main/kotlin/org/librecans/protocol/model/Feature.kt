package org.librecans.protocol.model

import org.librecans.protocol.mdr.Table

/** A "support function" advertised by the headphones in CONNECT_RET_SUPPORT_FUNCTION. */
data class Feature(val table: Table, val code: Int)

object Features {
    val CODEC_INDICATOR = t1(0x12)
    val BATTERY_LEVEL = t1(0x20)
    val LEFT_RIGHT_BATTERY = t1(0x21)
    val CRADLE_BATTERY = t1(0x22)
    val POWER_OFF = t1(0x23)
    val AUTO_POWER_OFF = t1(0x24)
    val AUTO_POWER_OFF_WEARING = t1(0x25)
    val BATTERY_THRESHOLD = t1(0x28)
    val LR_BATTERY_THRESHOLD = t1(0x29)
    val CRADLE_BATTERY_THRESHOLD = t1(0x2A)
    val PRESET_EQ = t1(0x50)
    val PRESET_EQ_NON_CUSTOMIZABLE = t1(0x52)
    val NC_ASM_DUAL_AUTO = t1(0x68)
    val NC_ASM_DUAL_SINGLE = t1(0x6A)
    val NC_ASM_DUAL = t1(0x6B)
    val NC_ASM_NOISE_ADAPTATION = t1(0x6D)
    val AMBIENT_SOUND_LEVEL = t1(0x67)
    val PLAYBACK_CONTROLLER = t1(0xA1)
    val PLAYBACK_CONTROLLER_MUTE = t1(0xA2)
    val GENERAL_SETTING_1 = t1(0xD1)
    val GENERAL_SETTING_2 = t1(0xD2)
    val GENERAL_SETTING_3 = t1(0xD3)
    val GENERAL_SETTING_4 = t1(0xD4)
    val CONNECTION_MODE = t1(0xE1)
    val UPSCALING = t1(0xE2)
    val PLAYBACK_BY_WEARING = t1(0xF1)
    val ASSIGNABLE_SETTING = t1(0xF3)
    val AUTO_VOLUME = t1(0xFA)
    val SMART_TALKING_TYPE2 = t1(0xFC)
    val HEAD_GESTURE = t1(0xFF)

    val VOICE_GUIDANCE = t2(0x42)

    val GENERAL_SETTINGS = listOf(GENERAL_SETTING_1, GENERAL_SETTING_2, GENERAL_SETTING_3, GENERAL_SETTING_4)

    private fun t1(code: Int) = Feature(Table.ONE, code)
    private fun t2(code: Int) = Feature(Table.TWO, code)
}
