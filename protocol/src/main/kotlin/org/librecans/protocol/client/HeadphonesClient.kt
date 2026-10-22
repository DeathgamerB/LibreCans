package org.librecans.protocol.client

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.librecans.protocol.event.DeviceEvent
import org.librecans.protocol.mdr.Table
import org.librecans.protocol.model.AutoPowerOff
import org.librecans.protocol.model.ConnectionPriority
import org.librecans.protocol.model.EqLevels
import org.librecans.protocol.model.EqPresets
import org.librecans.protocol.model.EqualizerMath
import org.librecans.protocol.model.GestureBinding
import org.librecans.protocol.model.NoiseControl
import org.librecans.protocol.model.NoiseMode
import org.librecans.protocol.model.PlaybackCommand
import org.librecans.protocol.model.PlaybackStatus
import org.librecans.protocol.model.PresetOption
import org.librecans.protocol.model.SpeakToChatSensitivity
import org.librecans.protocol.model.SpeakToChatTimeout
import org.librecans.protocol.model.TouchActions
import org.librecans.protocol.model.TouchPresets
import org.librecans.protocol.request.AudioRequests
import org.librecans.protocol.request.ConnectRequests
import org.librecans.protocol.request.EqualizerRequests
import org.librecans.protocol.request.GeneralSettingRequests
import org.librecans.protocol.request.NoiseControlRequests
import org.librecans.protocol.request.PlayRequests
import org.librecans.protocol.request.PowerRequests
import org.librecans.protocol.request.SystemRequests
import org.librecans.protocol.request.VoiceGuidanceRequests
import org.librecans.protocol.session.MdrSession
import org.librecans.protocol.state.HeadphoneState
import org.librecans.protocol.state.reduce

enum class InitResult { READY, UNSUPPORTED_PROTOCOL, NO_RESPONSE }

/**
 * High level API over an [MdrSession]. Setters update [state] optimistically; the headphones
 * confirm with a notification which the reducer applies on top.
 */
class HeadphonesClient(
    private val session: MdrSession,
    scope: CoroutineScope,
    private val replyTimeoutMillis: Long = 3_000,
) {
    private val _state = MutableStateFlow(HeadphoneState())
    val state: StateFlow<HeadphoneState> = _state.asStateFlow()

    init {
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            session.events.collect(::apply)
        }
    }

    suspend fun initialize(): InitResult {
        val info = session.query(ConnectRequests.protocolInfo(), replyTimeoutMillis) {
            it as? DeviceEvent.ProtocolInfo
        } ?: return InitResult.NO_RESPONSE
        if (!info.table1) return InitResult.UNSUPPORTED_PROTOCOL

        session.send(ConnectRequests.modelName())
        session.send(ConnectRequests.firmware())
        session.send(ConnectRequests.seriesAndColor())

        val table1 = session.query(ConnectRequests.supportedFeatures(), replyTimeoutMillis) {
            (it as? DeviceEvent.SupportedFeatures)?.takeIf { e -> e.table == Table.ONE }
        } ?: return InitResult.NO_RESPONSE
        val table2 = if (info.table2) {
            session.query(ConnectRequests.supportedFeaturesTable2(), replyTimeoutMillis) {
                (it as? DeviceEvent.SupportedFeatures)?.takeIf { e -> e.table == Table.TWO }
            }
        } else {
            null
        }

        InitPlan.requests(table1.features + table2?.features.orEmpty()).forEach { session.send(it) }
        return InitResult.READY
    }

    fun refresh() = InitPlan.requests(features).forEach { session.post(it) }

    fun refreshBattery() = InitPlan.batteryRequests(features).forEach { session.post(it) }

    fun setNoiseControl(mode: NoiseMode, ambientLevel: Int, focusOnVoice: Boolean, settled: Boolean = true) {
        val current = state.value.noiseControl
            ?: InitPlan.noiseControlType(features)?.let { NoiseControl(mode, ambientLevel, focusOnVoice, it) }
            ?: return
        val updated = current.copy(
            mode = mode,
            ambientLevel = ambientLevel.coerceIn(NoiseControl.MIN_AMBIENT_LEVEL, NoiseControl.MAX_AMBIENT_LEVEL),
            focusOnVoice = focusOnVoice,
        )
        apply(DeviceEvent.NoiseControlChanged(updated, settled))
        session.post(NoiseControlRequests.set(updated, settled), coalesceKey = "noiseControl")
    }

    /** Changes part of the noise control based on the latest local state, so rapid edits compose. */
    fun updateNoiseControl(settled: Boolean = true, transform: (NoiseControl) -> NoiseControl) {
        val next = transform(state.value.noiseControl ?: return)
        setNoiseControl(next.mode, next.ambientLevel, next.focusOnVoice, settled)
    }

    /** Changes some bands based on the latest local state, so edits to different bands compose. */
    fun updateEqualizerLevels(transform: (EqLevels) -> EqLevels) {
        val equalizer = state.value.equalizer?.takeIf { it.steps.isNotEmpty() } ?: return
        setEqualizerLevels(transform(equalizer.levels))
    }

    fun setEqualizerPreset(presetId: Int) {
        apply(DeviceEvent.EqualizerChanged(presetId, emptyList()))
        session.post(EqualizerRequests.setPreset(presetId), coalesceKey = "equalizer")
        session.post(EqualizerRequests.get())
    }

    fun setEqualizerLevels(levels: EqLevels) {
        val presetId = state.value.equalizer?.presetId?.takeIf(EqPresets::isCustom) ?: EqPresets.CUSTOM
        val steps = EqualizerMath.steps(levels)
        apply(DeviceEvent.EqualizerChanged(presetId, steps))
        session.post(EqualizerRequests.setSteps(presetId, steps), coalesceKey = "equalizer")
    }

    fun setUpscaling(enabled: Boolean) {
        apply(DeviceEvent.UpscalingChanged(enabled))
        session.post(AudioRequests.setUpscaling(enabled))
    }

    fun setConnectionPriority(priority: ConnectionPriority) {
        apply(DeviceEvent.ConnectionPriorityChanged(priority))
        session.post(AudioRequests.setConnectionPriority(priority))
    }

    fun setTouchPreset(key: Int, preset: Int) {
        val touch = state.value.touchControls ?: return
        val updated = touch.withPreset(key, preset)
        if (updated == touch) return
        apply(DeviceEvent.TouchPresetsChanged(updated.presets))
        session.post(SystemRequests.setTouchPresets(updated.presets))
        session.post(SystemRequests.touchCustomizations())
    }

    /** Chooses which modes the ambient sound gesture cycles through, e.g. NC ↔ Ambient. */
    fun setAmbientCycle(function: Int) {
        val touch = state.value.touchControls ?: return
        val existing = touch.customizations.filter { TouchPresets.controlsAmbientSound(it.preset) }
        val targets = existing.ifEmpty {
            val preset = touch.presets.firstOrNull(TouchPresets::controlsAmbientSound) ?: return
            listOf(PresetOption(preset, listOf(GestureBinding(TouchActions.SINGLE_TAP, function))))
        }
        val updated = targets.map { option ->
            option.copy(
                gestures = option.gestures.map {
                    if (it.action == TouchActions.SINGLE_TAP) it.copy(function = function) else it
                },
            )
        }
        val others = touch.customizations.filterNot { TouchPresets.controlsAmbientSound(it.preset) }
        apply(DeviceEvent.TouchCustomizations(others + updated))
        session.post(SystemRequests.setTouchCustomizations(updated))
    }

    fun setSpeakToChat(enabled: Boolean) {
        apply(DeviceEvent.SpeakToChatChanged(enabled))
        session.post(SystemRequests.setSpeakToChat(enabled))
    }

    fun setSpeakToChatConfig(sensitivity: SpeakToChatSensitivity, timeout: SpeakToChatTimeout) {
        apply(DeviceEvent.SpeakToChatConfigChanged(sensitivity, timeout))
        session.post(SystemRequests.setSpeakToChatConfig(sensitivity, timeout))
    }

    fun setPauseWhenRemoved(enabled: Boolean) {
        apply(DeviceEvent.PauseWhenRemovedChanged(enabled))
        session.post(SystemRequests.setPauseWhenRemoved(enabled))
    }

    fun setHeadGestures(enabled: Boolean) {
        apply(DeviceEvent.HeadGesturesChanged(enabled))
        session.post(SystemRequests.setHeadGestures(enabled))
    }

    fun setAdaptiveVolume(enabled: Boolean) {
        apply(DeviceEvent.AdaptiveVolumeChanged(enabled))
        session.post(SystemRequests.setAdaptiveVolume(enabled))
    }

    fun setAutoPowerOff(value: AutoPowerOff) {
        val wearing = state.value.autoPowerOff?.withWearingDetection ?: InitPlan.autoPowerOffWearing(features) ?: return
        val last = if (value == AutoPowerOff.WHEN_REMOVED || value == AutoPowerOff.DISABLED) {
            state.value.autoPowerOff?.lastSelected ?: AutoPowerOff.AFTER_5_MIN.code
        } else {
            value.code
        }
        apply(DeviceEvent.AutoPowerOffChanged(value, last, wearing))
        session.post(PowerRequests.setAutoPowerOff(wearing, value, last))
    }

    fun setGeneralSetting(id: Int, enabled: Boolean) {
        apply(DeviceEvent.GeneralSettingChanged(id, enabled))
        session.post(GeneralSettingRequests.set(id, enabled))
    }

    fun setVolume(volume: Int) {
        apply(DeviceEvent.VolumeChanged(volume))
        session.post(PlayRequests.setVolume(volume), coalesceKey = "volume")
    }

    fun playback(command: PlaybackCommand) {
        when (command) {
            PlaybackCommand.PLAY -> apply(DeviceEvent.PlaybackStatusChanged(PlaybackStatus.PLAYING))
            PlaybackCommand.PAUSE -> apply(DeviceEvent.PlaybackStatusChanged(PlaybackStatus.PAUSED))
            else -> Unit
        }
        session.post(PlayRequests.control(command))
    }

    fun setVoiceGuidance(enabled: Boolean) {
        apply(DeviceEvent.VoiceGuidanceChanged(enabled))
        session.post(VoiceGuidanceRequests.set(enabled))
    }

    fun setVoiceGuidanceVolume(volume: Int) {
        apply(DeviceEvent.VoiceGuidanceVolumeChanged(volume))
        session.post(VoiceGuidanceRequests.setVolume(volume), coalesceKey = "voiceGuidanceVolume")
    }

    suspend fun powerOff(): Boolean = session.send(PowerRequests.powerOff())

    private val features get() = state.value.features

    private fun apply(event: DeviceEvent) = _state.update { it.reduce(event) }
}
