package org.librecans.protocol.sim

import org.librecans.protocol.mdr.Inquired
import org.librecans.protocol.mdr.T1
import org.librecans.protocol.mdr.T2
import org.librecans.protocol.mdr.Table
import org.librecans.protocol.mdr.isOn
import org.librecans.protocol.mdr.onOff
import org.librecans.protocol.model.EqPresets
import org.librecans.protocol.model.TouchActions
import org.librecans.protocol.model.TouchPresets

class SimulatedReply(val table: Table, val payload: List<Int>)

/** Answers requests the way a WF-1000XM5 does: GET yields RET, SET yields NTFY. */
class SimulatorResponder(private val model: SimulatorModel) {
    fun respond(table: Table, payload: List<Int>): List<SimulatedReply> {
        if (payload.isEmpty()) return emptyList()
        return when (table) {
            Table.ONE -> table1(payload).map { SimulatedReply(Table.ONE, it) }
            Table.TWO -> table2(payload).map { SimulatedReply(Table.TWO, it) }
        }
    }

    private fun table1(p: List<Int>): List<List<Int>> = with(model) {
        val type = p.getOrElse(1) { -1 }
        when (p[0]) {
            T1.CONNECT_GET_PROTOCOL_INFO -> listOf(listOf(0x01, 0x00, 0x03, 0x00, 0x30, 0x18, 0x00, 0x00))
            T1.CONNECT_GET_CAPABILITY_INFO -> listOf(listOf(T1.CONNECT_RET_CAPABILITY_INFO, type, 0x01) + text(uniqueId))
            T1.CONNECT_GET_DEVICE_INFO -> listOfNotNull(deviceInfo(type))
            T1.CONNECT_GET_SUPPORT_FUNCTION -> listOf(supportList(T1.CONNECT_RET_SUPPORT_FUNCTION, SimulatorModel.SUPPORT_TABLE1))
            T1.COMMON_GET_STATUS -> listOf(listOf(T1.COMMON_RET_STATUS, Inquired.COMMON_AUDIO_CODEC, 0x02))
            T1.POWER_GET_STATUS -> listOfNotNull(battery(T1.POWER_RET_STATUS, type))
            T1.POWER_GET_CAPABILITY -> listOf(listOf(T1.POWER_RET_CAPABILITY, type, 6, 0x10, 0x00, 0x04, 0x01, 0x02, 0x11))
            T1.POWER_GET_PARAM -> listOf(listOf(T1.POWER_RET_PARAM, type, autoPowerOff, autoPowerOffLast))
            T1.POWER_SET_PARAM -> {
                autoPowerOff = p[2]
                autoPowerOffLast = p[3]
                listOf(listOf(T1.POWER_NTFY_PARAM, type, autoPowerOff, autoPowerOffLast))
            }
            T1.POWER_SET_STATUS -> {
                if (type == Inquired.POWER_POWER_OFF) poweredOff = true
                emptyList()
            }
            T1.NCASM_GET_PARAM -> listOf(noiseControl(T1.NCASM_RET_PARAM, changed = true))
            T1.NCASM_SET_PARAM -> {
                ncEnabled = p[3] == 1
                ncAmbient = p[4] == 1
                focusOnVoice = p[5] == 1
                ambientLevel = p[6]
                listOf(noiseControl(T1.NCASM_NTFY_PARAM, changed = p[2] == 1))
            }
            T1.EQEBB_GET_CAPABILITY -> listOf(eqCapability())
            T1.EQEBB_GET_STATUS -> listOf(listOf(T1.EQEBB_RET_STATUS, Inquired.EQ_PRESET, 0x00))
            T1.EQEBB_GET_PARAM -> listOf(equalizer(T1.EQEBB_RET_PARAM))
            T1.EQEBB_SET_PARAM -> {
                eqPreset = p[2]
                val steps = p.drop(4)
                if (steps.isNotEmpty()) eqSteps = steps.toMutableList()
                listOf(equalizer(T1.EQEBB_NTFY_PARAM))
            }
            T1.AUDIO_GET_CAPABILITY -> listOf(listOf(T1.AUDIO_RET_CAPABILITY, Inquired.AUDIO_UPSCALING, 0x02))
            T1.AUDIO_GET_PARAM -> if (type == Inquired.AUDIO_UPSCALING) {
                listOf(listOf(T1.AUDIO_RET_PARAM, type, if (upscaling) 1 else 0))
            } else {
                emptyList()
            }
            T1.AUDIO_SET_PARAM -> if (type == Inquired.AUDIO_UPSCALING) {
                upscaling = p[2] == 1
                listOf(listOf(T1.AUDIO_NTFY_PARAM, type, p[2]))
            } else {
                emptyList()
            }
            T1.SYSTEM_GET_CAPABILITY -> if (type == Inquired.SYSTEM_ASSIGNABLE) listOf(touchCapability()) else emptyList()
            T1.SYSTEM_GET_PARAM -> listOfNotNull(systemParam(T1.SYSTEM_RET_PARAM, type))
            T1.SYSTEM_SET_PARAM -> {
                applySystemParam(p)
                listOfNotNull(systemParam(T1.SYSTEM_NTFY_PARAM, type))
            }
            T1.SYSTEM_GET_EXT_PARAM -> listOfNotNull(systemExtParam(T1.SYSTEM_RET_EXT_PARAM, type))
            T1.SYSTEM_SET_EXT_PARAM -> {
                applySystemExtParam(p)
                listOfNotNull(systemExtParam(T1.SYSTEM_NTFY_EXT_PARAM, type))
            }
            T1.GENERAL_SETTING_GET_CAPABILITY -> listOfNotNull(generalSettingCapability(type))
            T1.GENERAL_SETTING_GET_PARAM -> generalSettings[type]?.let {
                listOf(listOf(T1.GENERAL_SETTING_RET_PARAM, type, 0x00, onOff(it)))
            }.orEmpty()
            T1.GENERAL_SETTING_SET_PARAM -> {
                generalSettings[type] = isOn(p[3])
                listOf(listOf(T1.GENERAL_SETTING_NTFY_PARAM, type, 0x00, p[3]))
            }
            T1.PLAY_GET_PARAM -> listOf(listOf(T1.PLAY_RET_PARAM, Inquired.PLAY_MUSIC_VOLUME, volume))
            T1.PLAY_SET_PARAM -> {
                volume = p[2].coerceIn(0, 30)
                listOf(listOf(T1.PLAY_NTFY_PARAM, Inquired.PLAY_MUSIC_VOLUME, volume))
            }
            T1.PLAY_GET_STATUS -> listOf(playStatus(T1.PLAY_RET_STATUS))
            T1.PLAY_SET_STATUS -> {
                when (p[3]) {
                    0x01 -> playing = false
                    0x07 -> playing = true
                }
                listOf(playStatus(T1.PLAY_NTFY_STATUS))
            }
            else -> emptyList()
        }
    }

    private fun table2(p: List<Int>): List<List<Int>> = with(model) {
        val type = p.getOrElse(1) { -1 }
        when (p[0]) {
            T2.CONNECT_GET_SUPPORT_FUNCTION -> listOf(supportList(T2.CONNECT_RET_SUPPORT_FUNCTION, SimulatorModel.SUPPORT_TABLE2))
            T2.VOICE_GUIDANCE_GET_PARAM -> listOf(voiceGuidance(T2.VOICE_GUIDANCE_RET_PARAM, type))
            T2.VOICE_GUIDANCE_SET_PARAM -> {
                if (type == Inquired.VOICE_GUIDANCE_VOLUME) voiceGuidanceVolume = p[2].toByte().toInt()
                else voiceGuidance = isOn(p[2])
                listOf(voiceGuidance(T2.VOICE_GUIDANCE_NTFY_PARAM, type))
            }
            else -> emptyList()
        }
    }

    fun noiseControl(command: Int, changed: Boolean): List<Int> = with(model) {
        listOf(command, 0x17, if (changed) 1 else 0, if (ncEnabled) 1 else 0, if (ncAmbient) 1 else 0,
            if (focusOnVoice) 1 else 0, ambientLevel)
    }

    fun battery(command: Int, type: Int): List<Int>? = with(model) {
        when (type) {
            Inquired.POWER_LR_BATTERY_THRESHOLD -> listOf(command, type, leftBattery, 0, rightBattery, 0, 0x64, 0x64)
            Inquired.POWER_CRADLE_BATTERY_THRESHOLD -> listOf(command, type, caseBattery, if (caseCharging) 1 else 0, 0x1e)
            else -> null
        }
    }

    private fun SimulatorModel.deviceInfo(type: Int): List<Int>? = when (type) {
        Inquired.DEVICE_INFO_MODEL_NAME -> listOf(T1.CONNECT_RET_DEVICE_INFO, type) + text(modelName)
        Inquired.DEVICE_INFO_FW_VERSION -> listOf(T1.CONNECT_RET_DEVICE_INFO, type) + text(firmware)
        Inquired.DEVICE_INFO_SERIES_AND_COLOR -> listOf(T1.CONNECT_RET_DEVICE_INFO, type, 0x30, 0x03)
        else -> null
    }

    private fun supportList(command: Int, functions: List<Int>): List<Int> =
        listOf(command, 0x00, functions.size) + functions.flatMap { listOf(it, 0xFF) }

    private fun SimulatorModel.equalizer(command: Int): List<Int> {
        if (!EqPresets.isCustom(eqPreset)) eqSteps = presetForEqId(eqPreset).toMutableList()
        return listOf(command, Inquired.EQ_PRESET, eqPreset, eqSteps.size) + eqSteps
    }

    private fun eqCapability(): List<Int> = listOf(T1.EQEBB_RET_CAPABILITY, Inquired.EQ_PRESET, 6, 21) +
        listOf(SimulatorModel.EQ_PRESET_NAMES.size) +
        SimulatorModel.EQ_PRESET_NAMES.flatMap { (id, name) -> listOf(id) + text(name) }

    private fun touchCapability(): List<Int> {
        val presets = SimulatorModel.TOUCH_OPTIONS.flatMap { (preset, actions) ->
            val fixed = actions.filter { it.third.isEmpty() }
            val customizable = actions.filter { it.third.isNotEmpty() }
            listOf(preset, fixed.size, customizable.size) +
                fixed.flatMap { listOf(it.first, it.second) } +
                customizable.flatMap { listOf(it.first, it.second, it.third.size) + it.third }
        }
        val keys = listOf(0x00, 0x01).flatMap { key ->
            listOf(key, 0x00, TouchPresets.AMBIENT_SOUND_CONTROL, SimulatorModel.TOUCH_OPTIONS.size) + presets
        }
        return listOf(T1.SYSTEM_RET_CAPABILITY, Inquired.SYSTEM_ASSIGNABLE, 2) + keys
    }

    private fun SimulatorModel.systemParam(command: Int, type: Int): List<Int>? = when (type) {
        Inquired.SYSTEM_ASSIGNABLE -> listOf(command, type, touchPresets.size) + touchPresets
        Inquired.SYSTEM_SMART_TALKING -> listOf(command, type, onOff(speakToChat), onOff(false))
        Inquired.SYSTEM_PLAYBACK_BY_WEARING -> listOf(command, type, onOff(pauseWhenRemoved))
        Inquired.SYSTEM_HEAD_GESTURE -> listOf(command, type, onOff(headGestures))
        else -> null
    }

    private fun SimulatorModel.applySystemParam(p: List<Int>) {
        when (p[1]) {
            Inquired.SYSTEM_ASSIGNABLE -> touchPresets = p.drop(3).take(p[2]).toMutableList()
            Inquired.SYSTEM_SMART_TALKING -> speakToChat = isOn(p[2])
            Inquired.SYSTEM_PLAYBACK_BY_WEARING -> pauseWhenRemoved = isOn(p[2])
            Inquired.SYSTEM_HEAD_GESTURE -> headGestures = isOn(p[2])
        }
    }

    private fun SimulatorModel.systemExtParam(command: Int, type: Int): List<Int>? = when (type) {
        Inquired.SYSTEM_ASSIGNABLE -> {
            val ambient = touchPresets.filter(TouchPresets::controlsAmbientSound).distinct()
            listOf(command, type, ambient.size) +
                ambient.flatMap { listOf(it, 1, TouchActions.SINGLE_TAP, ambientCycle) }
        }
        Inquired.SYSTEM_SMART_TALKING -> listOf(command, type, speakToChatSensitivity, speakToChatTimeout)
        else -> null
    }

    private fun SimulatorModel.applySystemExtParam(p: List<Int>) {
        when (p[1]) {
            Inquired.SYSTEM_ASSIGNABLE -> if (p.size >= 7) ambientCycle = p[6]
            Inquired.SYSTEM_SMART_TALKING -> {
                speakToChatSensitivity = p[2]
                speakToChatTimeout = p[3]
            }
        }
    }

    private fun generalSettingCapability(id: Int): List<Int>? {
        val (title, summary) = SimulatorModel.GENERAL_SETTING_KEYS[id] ?: return null
        return listOf(T1.GENERAL_SETTING_RET_CAPABILITY, id, 0x00, 0x01) + text(title) + text(summary)
    }

    private fun SimulatorModel.playStatus(command: Int): List<Int> =
        listOf(command, Inquired.PLAY_CONTROL, 0x00, if (playing) 0x01 else 0x02, 0x00)

    private fun SimulatorModel.voiceGuidance(command: Int, type: Int): List<Int> =
        if (type == Inquired.VOICE_GUIDANCE_VOLUME) listOf(command, type, voiceGuidanceVolume and 0xFF)
        else listOf(command, type, onOff(voiceGuidance), 0x01)

    private fun text(value: String): List<Int> = listOf(value.length) + value.toByteArray().map { it.toInt() and 0xFF }
}
