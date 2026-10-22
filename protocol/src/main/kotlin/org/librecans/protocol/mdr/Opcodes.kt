package org.librecans.protocol.mdr

/** Table 1 command bytes (Sony "MDR v2 T1"). Only the families LibreCans speaks are listed. */
object T1 {
    const val CONNECT_GET_PROTOCOL_INFO = 0x00
    const val CONNECT_RET_PROTOCOL_INFO = 0x01
    const val CONNECT_GET_DEVICE_INFO = 0x04
    const val CONNECT_RET_DEVICE_INFO = 0x05
    const val CONNECT_GET_SUPPORT_FUNCTION = 0x06
    const val CONNECT_RET_SUPPORT_FUNCTION = 0x07

    const val COMMON_GET_STATUS = 0x12
    const val COMMON_RET_STATUS = 0x13
    const val COMMON_NTFY_STATUS = 0x15

    const val POWER_GET_CAPABILITY = 0x20
    const val POWER_RET_CAPABILITY = 0x21
    const val POWER_GET_STATUS = 0x22
    const val POWER_RET_STATUS = 0x23
    const val POWER_SET_STATUS = 0x24
    const val POWER_NTFY_STATUS = 0x25
    const val POWER_GET_PARAM = 0x26
    const val POWER_RET_PARAM = 0x27
    const val POWER_SET_PARAM = 0x28
    const val POWER_NTFY_PARAM = 0x29

    const val EQEBB_GET_CAPABILITY = 0x50
    const val EQEBB_RET_CAPABILITY = 0x51
    const val EQEBB_GET_STATUS = 0x52
    const val EQEBB_RET_STATUS = 0x53
    const val EQEBB_NTFY_STATUS = 0x55
    const val EQEBB_GET_PARAM = 0x56
    const val EQEBB_RET_PARAM = 0x57
    const val EQEBB_SET_PARAM = 0x58
    const val EQEBB_NTFY_PARAM = 0x59

    const val NCASM_GET_PARAM = 0x66
    const val NCASM_RET_PARAM = 0x67
    const val NCASM_SET_PARAM = 0x68
    const val NCASM_NTFY_PARAM = 0x69

    const val PLAY_GET_STATUS = 0xA2
    const val PLAY_RET_STATUS = 0xA3
    const val PLAY_SET_STATUS = 0xA4
    const val PLAY_NTFY_STATUS = 0xA5
    const val PLAY_GET_PARAM = 0xA6
    const val PLAY_RET_PARAM = 0xA7
    const val PLAY_SET_PARAM = 0xA8
    const val PLAY_NTFY_PARAM = 0xA9

    const val GENERAL_SETTING_GET_CAPABILITY = 0xD0
    const val GENERAL_SETTING_RET_CAPABILITY = 0xD1
    const val GENERAL_SETTING_GET_PARAM = 0xD6
    const val GENERAL_SETTING_RET_PARAM = 0xD7
    const val GENERAL_SETTING_SET_PARAM = 0xD8
    const val GENERAL_SETTING_NTFY_PARAM = 0xD9

    const val AUDIO_GET_CAPABILITY = 0xE0
    const val AUDIO_RET_CAPABILITY = 0xE1
    const val AUDIO_GET_PARAM = 0xE6
    const val AUDIO_RET_PARAM = 0xE7
    const val AUDIO_SET_PARAM = 0xE8
    const val AUDIO_NTFY_PARAM = 0xE9

    const val SYSTEM_GET_CAPABILITY = 0xF0
    const val SYSTEM_RET_CAPABILITY = 0xF1
    const val SYSTEM_GET_PARAM = 0xF6
    const val SYSTEM_RET_PARAM = 0xF7
    const val SYSTEM_SET_PARAM = 0xF8
    const val SYSTEM_NTFY_PARAM = 0xF9
    const val SYSTEM_GET_EXT_PARAM = 0xFA
    const val SYSTEM_RET_EXT_PARAM = 0xFB
    const val SYSTEM_SET_EXT_PARAM = 0xFC
    const val SYSTEM_NTFY_EXT_PARAM = 0xFD
}

/** Table 2 command bytes. */
object T2 {
    const val CONNECT_GET_SUPPORT_FUNCTION = 0x06
    const val CONNECT_RET_SUPPORT_FUNCTION = 0x07
    const val VOICE_GUIDANCE_GET_PARAM = 0x46
    const val VOICE_GUIDANCE_RET_PARAM = 0x47
    const val VOICE_GUIDANCE_SET_PARAM = 0x48
    const val VOICE_GUIDANCE_NTFY_PARAM = 0x49
}

/** Sub-types ("inquired types") shared by several command families. */
object Inquired {
    const val FIXED = 0x00

    const val DEVICE_INFO_MODEL_NAME = 0x01
    const val DEVICE_INFO_FW_VERSION = 0x02
    const val DEVICE_INFO_SERIES_AND_COLOR = 0x03

    const val COMMON_AUDIO_CODEC = 0x02

    const val POWER_BATTERY = 0x00
    const val POWER_LEFT_RIGHT_BATTERY = 0x01
    const val POWER_CRADLE_BATTERY = 0x02
    const val POWER_POWER_OFF = 0x03
    const val POWER_AUTO_POWER_OFF = 0x04
    const val POWER_AUTO_POWER_OFF_WEARING = 0x05
    const val POWER_BATTERY_THRESHOLD = 0x08
    const val POWER_LR_BATTERY_THRESHOLD = 0x09
    const val POWER_CRADLE_BATTERY_THRESHOLD = 0x0A

    const val EQ_PRESET = 0x00

    const val NCASM_ASM_SEAMLESS = 0x22
    const val NCASM_DUAL_SWITCH_ASM_SEAMLESS = 0x17

    const val PLAY_CONTROL = 0x01
    const val PLAY_MUSIC_VOLUME = 0x20

    const val AUDIO_CONNECTION_MODE = 0x00
    const val AUDIO_UPSCALING = 0x01

    const val SYSTEM_PLAYBACK_BY_WEARING = 0x01
    const val SYSTEM_ASSIGNABLE = 0x03
    const val SYSTEM_AUTO_VOLUME = 0x0A
    const val SYSTEM_SMART_TALKING = 0x0C
    const val SYSTEM_HEAD_GESTURE = 0x0F

    const val VOICE_GUIDANCE_SETTING = 0x01
    const val VOICE_GUIDANCE_VOLUME = 0x20

    val GENERAL_SETTINGS = listOf(0xD1, 0xD2, 0xD3, 0xD4)
}

/** On/off bytes are inverted on MDR v2: 0x00 means enabled. */
internal fun onOff(enabled: Boolean): Int = if (enabled) 0x00 else 0x01

internal fun isOn(value: Int): Boolean = value == 0x00
