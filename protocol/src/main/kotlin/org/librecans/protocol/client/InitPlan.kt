package org.librecans.protocol.client

import org.librecans.protocol.mdr.Inquired
import org.librecans.protocol.mdr.MdrRequest
import org.librecans.protocol.model.Feature
import org.librecans.protocol.model.Features
import org.librecans.protocol.request.AudioRequests
import org.librecans.protocol.request.EqualizerRequests
import org.librecans.protocol.request.GeneralSettingRequests
import org.librecans.protocol.request.NoiseControlRequests
import org.librecans.protocol.request.PlayRequests
import org.librecans.protocol.request.PowerRequests
import org.librecans.protocol.request.SystemRequests
import org.librecans.protocol.request.VoiceGuidanceRequests

/** Decides which queries to send based on the advertised support functions. */
object InitPlan {
    fun requests(features: Set<Feature>): List<MdrRequest> = buildList {
        fun has(feature: Feature) = feature in features

        if (has(Features.CODEC_INDICATOR)) add(AudioRequests.codec())
        addAll(batteryRequests(features))
        noiseControlType(features)?.let { add(NoiseControlRequests.get(it)) }

        if (has(Features.PRESET_EQ) || has(Features.PRESET_EQ_NON_CUSTOMIZABLE)) {
            add(EqualizerRequests.capability())
            add(EqualizerRequests.status())
            add(EqualizerRequests.get())
        }
        if (has(Features.UPSCALING)) {
            add(AudioRequests.upscalingCapability())
            add(AudioRequests.upscaling())
        }
        if (has(Features.CONNECTION_MODE)) add(AudioRequests.connectionPriority())

        if (has(Features.ASSIGNABLE_SETTING)) {
            add(SystemRequests.touchCapability())
            add(SystemRequests.touchPresets())
            add(SystemRequests.touchCustomizations())
        }
        if (has(Features.PLAYBACK_CONTROLLER) || has(Features.PLAYBACK_CONTROLLER_MUTE)) {
            add(PlayRequests.volume())
            add(PlayRequests.status())
        }
        if (has(Features.SMART_TALKING_TYPE2)) {
            add(SystemRequests.speakToChat())
            add(SystemRequests.speakToChatConfig())
        }
        if (has(Features.PLAYBACK_BY_WEARING)) add(SystemRequests.pauseWhenRemoved())
        if (has(Features.HEAD_GESTURE)) add(SystemRequests.headGestures())
        if (has(Features.AUTO_VOLUME)) add(SystemRequests.adaptiveVolume())

        autoPowerOffWearing(features)?.let { wearing ->
            add(PowerRequests.autoPowerOffOptions(wearing))
            add(PowerRequests.autoPowerOff(wearing))
        }
        Features.GENERAL_SETTINGS.filter(::has).forEach {
            add(GeneralSettingRequests.capability(it.code))
            add(GeneralSettingRequests.get(it.code))
        }
        if (has(Features.VOICE_GUIDANCE)) {
            add(VoiceGuidanceRequests.get())
            add(VoiceGuidanceRequests.volume())
        }
    }

    fun batteryRequests(features: Set<Feature>): List<MdrRequest> = buildList {
        when {
            Features.BATTERY_LEVEL in features -> add(PowerRequests.battery(Inquired.POWER_BATTERY))
            Features.BATTERY_THRESHOLD in features -> add(PowerRequests.battery(Inquired.POWER_BATTERY_THRESHOLD))
        }
        when {
            Features.LEFT_RIGHT_BATTERY in features -> add(PowerRequests.battery(Inquired.POWER_LEFT_RIGHT_BATTERY))
            Features.LR_BATTERY_THRESHOLD in features ->
                add(PowerRequests.battery(Inquired.POWER_LR_BATTERY_THRESHOLD))
        }
        when {
            Features.CRADLE_BATTERY in features -> add(PowerRequests.battery(Inquired.POWER_CRADLE_BATTERY))
            Features.CRADLE_BATTERY_THRESHOLD in features ->
                add(PowerRequests.battery(Inquired.POWER_CRADLE_BATTERY_THRESHOLD))
        }
    }

    fun noiseControlType(features: Set<Feature>): Int? = when {
        Features.NC_ASM_DUAL in features -> 0x17
        Features.NC_ASM_NOISE_ADAPTATION in features -> 0x19
        Features.NC_ASM_DUAL_SINGLE in features -> 0x16
        Features.NC_ASM_DUAL_AUTO in features -> 0x15
        Features.AMBIENT_SOUND_LEVEL in features -> Inquired.NCASM_ASM_SEAMLESS
        else -> null
    }

    /** Null when auto power off is not supported at all. */
    fun autoPowerOffWearing(features: Set<Feature>): Boolean? = when {
        Features.AUTO_POWER_OFF_WEARING in features -> true
        Features.AUTO_POWER_OFF in features -> false
        else -> null
    }
}
