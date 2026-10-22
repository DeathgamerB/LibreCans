package org.librecans.protocol.sim

import org.librecans.protocol.model.TouchActions as A
import org.librecans.protocol.model.TouchFunctions as F
import org.librecans.protocol.model.TouchPresets as P

/**
 * Mutable state of a simulated WF-1000XM5. Defaults and the support function lists are
 * taken from a real capture of firmware 6.1.0.
 */
class SimulatorModel {
    var modelName = "WF-1000XM5"
    var firmware = "6.1.0"
    var uniqueId = "0000DEMO5A17E5"

    var leftBattery = 80
    var rightBattery = 76
    var caseBattery = 53
    var caseCharging = false

    var ncEnabled = true
    var ncAmbient = false
    var focusOnVoice = false
    var ambientLevel = 20

    var eqPreset = 0xA0
    var eqSteps = mutableListOf(0x14, 0x0b, 0x0a, 0x0d, 0x09, 0x0c)

    var upscaling = true
    var touchPresets = mutableListOf(P.AMBIENT_SOUND_CONTROL_QUICK_ACCESS, P.PLAYBACK_CONTROL)
    var ambientCycle = F.NC_ASM_OFF

    var speakToChat = false
    var speakToChatSensitivity = 0x00
    var speakToChatTimeout = 0x00
    var pauseWhenRemoved = true
    var headGestures = false

    var autoPowerOff = 0x10
    var autoPowerOffLast = 0x00

    val generalSettings = linkedMapOf(0xD1 to false, 0xD2 to true)

    var volume = 12
    var playing = false

    var voiceGuidance = true
    var voiceGuidanceVolume = 0

    var poweredOff = false

    fun presetForEqId(id: Int): List<Int> = when (id) {
        0x10 -> listOf(10, 11, 12, 13, 14, 13)
        0x11 -> listOf(13, 12, 10, 10, 12, 13)
        0x12 -> listOf(10, 9, 8, 9, 10, 11)
        0x13 -> listOf(10, 10, 9, 8, 8, 9)
        0x14 -> listOf(10, 8, 12, 13, 11, 9)
        0x15 -> listOf(10, 10, 10, 11, 13, 15)
        0x16 -> listOf(14, 14, 11, 10, 10, 10)
        0x17 -> listOf(10, 7, 12, 13, 12, 8)
        else -> eqSteps
    }

    companion object {
        val SUPPORT_TABLE1: List<Int> = listOf(
            0x10, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x23, 0x25, 0x11, 0x27, 0x29, 0x2a, 0x92, 0x50, 0x6b,
            0x70, 0x90, 0xa1, 0xc1, 0xc2, 0xe2, 0xf1, 0xfc, 0x93, 0xf6, 0xf9, 0xd1, 0x94, 0x40, 0x42, 0x44,
            0xff, 0x19, 0xf3, 0xe7, 0xd2, 0xfd, 0xb6, 0xb0, 0xb2, 0xb8, 0xb7, 0x34, 0xf4, 0xf5,
        )
        val SUPPORT_TABLE2: List<Int> = listOf(0x42, 0x53, 0x54, 0xf1, 0x60, 0x61, 0xf2, 0x63, 0xf8, 0x32, 0x31)

        val EQ_PRESET_NAMES: List<Pair<Int, String>> = listOf(
            0x00 to "Off", 0x10 to "Bright", 0x11 to "Excited", 0x12 to "Mellow", 0x13 to "Relaxed",
            0x14 to "Vocal", 0x15 to "Treble Boost", 0x16 to "Bass Boost", 0x17 to "Speech",
            0xA0 to "Custom", 0xA1 to "User Setting 1", 0xA2 to "User Setting 2",
        )

        val GENERAL_SETTING_KEYS: Map<Int, Pair<String, String>> = mapOf(
            0xD1 to ("SIDETONE_SETTING" to "SIDETONE_SETTING_SUMMARY"),
            0xD2 to ("MULTIPOINT_SETTING" to "MULTIPOINT_SETTING_SUMMARY_LDAC_AVAILABLE"),
        )

        /** Preset capabilities per earbud: preset to (action, function, customizable functions). */
        val TOUCH_OPTIONS: List<Pair<Int, List<Triple<Int, Int, List<Int>>>>> = listOf(
            P.AMBIENT_SOUND_CONTROL to listOf(
                Triple(A.SINGLE_TAP, F.NC_ASM_OFF, F.AMBIENT_CYCLES),
                Triple(A.TAP_AND_HOLD, F.QUICK_ATTENTION, emptyList()),
            ),
            P.AMBIENT_SOUND_CONTROL_QUICK_ACCESS to listOf(
                Triple(A.SINGLE_TAP, F.NC_ASM_OFF, F.AMBIENT_CYCLES),
                Triple(A.DOUBLE_TAP, F.QUICK_ACCESS_1, emptyList()),
                Triple(A.TRIPLE_TAP, F.QUICK_ACCESS_2, emptyList()),
                Triple(A.TAP_AND_HOLD, F.QUICK_ATTENTION, emptyList()),
            ),
            P.PLAYBACK_CONTROL to listOf(
                Triple(A.SINGLE_TAP, F.PLAY_PAUSE, emptyList()),
                Triple(A.DOUBLE_TAP, F.NEXT_TRACK, emptyList()),
                Triple(A.TRIPLE_TAP, F.PREVIOUS_TRACK, emptyList()),
                Triple(A.TAP_AND_HOLD, F.VOICE_RECOGNITION, emptyList()),
            ),
            P.VOLUME_CONTROL to listOf(
                Triple(A.SINGLE_TAP, F.VOLUME_UP, emptyList()),
                Triple(A.TAP_AND_HOLD, F.VOLUME_DOWN, emptyList()),
            ),
        )
    }
}
