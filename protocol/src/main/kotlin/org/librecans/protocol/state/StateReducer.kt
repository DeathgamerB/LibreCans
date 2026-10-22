package org.librecans.protocol.state

import org.librecans.protocol.event.DeviceEvent
import org.librecans.protocol.model.AutoPowerOffSetting
import org.librecans.protocol.model.Equalizer
import org.librecans.protocol.model.GeneralSetting
import org.librecans.protocol.model.Playback
import org.librecans.protocol.model.SpeakToChat
import org.librecans.protocol.model.TouchControls
import org.librecans.protocol.model.VoiceGuidance

fun HeadphoneState.reduce(event: DeviceEvent): HeadphoneState = when (event) {
    is DeviceEvent.ProtocolInfo -> copy(protocolVersion = event.version)
    is DeviceEvent.ModelName -> copy(identity = identity.copy(modelName = event.name))
    is DeviceEvent.FirmwareVersion -> copy(identity = identity.copy(firmware = event.version))
    is DeviceEvent.UniqueId -> copy(identity = identity.copy(uniqueId = event.id))
    is DeviceEvent.SeriesAndColor -> copy(identity = identity.copy(series = event.series, color = event.color))
    is DeviceEvent.SupportedFeatures ->
        copy(features = features.filterNot { it.table == event.table }.toSet() + event.features)

    is DeviceEvent.Battery ->
        copy(batteries = event.levels.entries.fold(batteries) { acc, (slot, level) -> acc.with(slot, level) })
    is DeviceEvent.Codec -> copy(codec = event.codec)
    is DeviceEvent.NoiseControlChanged -> copy(noiseControl = event.control)

    is DeviceEvent.EqualizerCapabilityChanged -> copy(equalizerCapability = event.capability)
    is DeviceEvent.EqualizerAvailability ->
        copy(equalizer = (equalizer ?: Equalizer(0, emptyList())).copy(available = event.available))
    is DeviceEvent.EqualizerChanged -> copy(
        equalizer = Equalizer(
            presetId = event.presetId,
            steps = event.steps.ifEmpty { equalizer?.steps.orEmpty() },
            available = equalizer?.available ?: true,
        ),
    )

    is DeviceEvent.UpscalingCapabilityChanged -> copy(upscalingType = event.type)
    is DeviceEvent.UpscalingChanged -> copy(upscaling = event.enabled)
    is DeviceEvent.ConnectionPriorityChanged -> copy(connectionPriority = event.priority)

    is DeviceEvent.AutoPowerOffOptions -> copy(autoPowerOffOptions = event.options)
    is DeviceEvent.AutoPowerOffChanged ->
        copy(autoPowerOff = AutoPowerOffSetting(event.current, event.lastSelected, event.withWearingDetection))

    is DeviceEvent.TouchPresetsChanged -> copy(touchControls = touch().copy(presets = event.presets))
    is DeviceEvent.TouchCapability -> copy(touchControls = touch().copy(keys = event.keys))
    is DeviceEvent.TouchCustomizations -> copy(touchControls = touch().copy(customizations = event.options))

    is DeviceEvent.SpeakToChatChanged ->
        copy(speakToChat = speakToChat?.copy(enabled = event.enabled) ?: SpeakToChat(event.enabled))
    is DeviceEvent.SpeakToChatConfigChanged -> copy(
        speakToChat = (speakToChat ?: SpeakToChat(enabled = false))
            .copy(sensitivity = event.sensitivity, timeout = event.timeout),
    )

    is DeviceEvent.PauseWhenRemovedChanged -> copy(pauseWhenRemoved = event.enabled)
    is DeviceEvent.HeadGesturesChanged -> copy(headGestures = event.enabled)
    is DeviceEvent.AdaptiveVolumeChanged -> copy(adaptiveVolume = event.enabled)

    is DeviceEvent.GeneralSettingDescribed -> copy(
        generalSettings = upsertSetting(event.setting.id) { it?.let { old -> event.setting.copy(enabled = old.enabled) } ?: event.setting },
    )
    is DeviceEvent.GeneralSettingChanged -> copy(
        generalSettings = upsertSetting(event.id) {
            it?.copy(enabled = event.enabled) ?: GeneralSetting(event.id, "", null, false, event.enabled)
        },
    )

    is DeviceEvent.VolumeChanged -> copy(playback = (playback ?: Playback()).copy(volume = event.volume))
    is DeviceEvent.PlaybackStatusChanged -> copy(playback = (playback ?: Playback()).copy(status = event.status))

    is DeviceEvent.VoiceGuidanceChanged ->
        copy(voiceGuidance = (voiceGuidance ?: VoiceGuidance()).copy(enabled = event.enabled))
    is DeviceEvent.VoiceGuidanceVolumeChanged ->
        copy(voiceGuidance = (voiceGuidance ?: VoiceGuidance()).copy(volume = event.volume))
}

private fun HeadphoneState.touch(): TouchControls = touchControls ?: TouchControls()

private fun HeadphoneState.upsertSetting(id: Int, update: (GeneralSetting?) -> GeneralSetting): List<GeneralSetting> {
    val existing = generalSettings.firstOrNull { it.id == id }
    return (generalSettings.filterNot { it.id == id } + update(existing)).sortedBy { it.id }
}
