package org.librecans.protocol.event

import org.librecans.protocol.mdr.Table
import org.librecans.protocol.model.AudioCodec
import org.librecans.protocol.model.AutoPowerOff
import org.librecans.protocol.model.BatteryLevel
import org.librecans.protocol.model.BatterySlot
import org.librecans.protocol.model.ConnectionPriority
import org.librecans.protocol.model.EqualizerCapability
import org.librecans.protocol.model.Feature
import org.librecans.protocol.model.GeneralSetting
import org.librecans.protocol.model.NoiseControl
import org.librecans.protocol.model.PlaybackStatus
import org.librecans.protocol.model.PresetOption
import org.librecans.protocol.model.SpeakToChatSensitivity
import org.librecans.protocol.model.SpeakToChatTimeout
import org.librecans.protocol.model.TouchKey
import org.librecans.protocol.model.UpscalingType

/** A decoded RET or NTFY payload. */
sealed interface DeviceEvent {
    data class ProtocolInfo(val version: Long, val table1: Boolean, val table2: Boolean) : DeviceEvent
    data class ModelName(val name: String) : DeviceEvent
    data class FirmwareVersion(val version: String) : DeviceEvent
    data class SeriesAndColor(val series: Int, val color: Int) : DeviceEvent
    data class SupportedFeatures(val table: Table, val features: Set<Feature>) : DeviceEvent

    data class Battery(val levels: Map<BatterySlot, BatteryLevel?>) : DeviceEvent
    data class Codec(val codec: AudioCodec) : DeviceEvent

    data class NoiseControlChanged(val control: NoiseControl, val settled: Boolean) : DeviceEvent

    data class EqualizerCapabilityChanged(val capability: EqualizerCapability) : DeviceEvent
    data class EqualizerAvailability(val available: Boolean) : DeviceEvent
    data class EqualizerChanged(val presetId: Int, val steps: List<Int>) : DeviceEvent

    data class UpscalingCapabilityChanged(val type: UpscalingType?) : DeviceEvent
    data class UpscalingChanged(val enabled: Boolean) : DeviceEvent
    data class ConnectionPriorityChanged(val priority: ConnectionPriority) : DeviceEvent

    data class AutoPowerOffOptions(val options: List<AutoPowerOff>) : DeviceEvent
    data class AutoPowerOffChanged(
        val current: AutoPowerOff,
        val lastSelected: Int,
        val withWearingDetection: Boolean,
    ) : DeviceEvent

    data class TouchPresetsChanged(val presets: List<Int>) : DeviceEvent
    data class TouchCapability(val keys: List<TouchKey>) : DeviceEvent
    data class TouchCustomizations(val options: List<PresetOption>) : DeviceEvent

    data class SpeakToChatChanged(val enabled: Boolean) : DeviceEvent
    data class SpeakToChatConfigChanged(
        val sensitivity: SpeakToChatSensitivity?,
        val timeout: SpeakToChatTimeout?,
    ) : DeviceEvent

    data class PauseWhenRemovedChanged(val enabled: Boolean) : DeviceEvent
    data class HeadGesturesChanged(val enabled: Boolean) : DeviceEvent
    data class AdaptiveVolumeChanged(val enabled: Boolean) : DeviceEvent

    data class GeneralSettingDescribed(val setting: GeneralSetting) : DeviceEvent
    data class GeneralSettingChanged(val id: Int, val enabled: Boolean) : DeviceEvent

    data class VolumeChanged(val volume: Int) : DeviceEvent
    data class PlaybackStatusChanged(val status: PlaybackStatus) : DeviceEvent

    data class VoiceGuidanceChanged(val enabled: Boolean) : DeviceEvent
    data class VoiceGuidanceVolumeChanged(val volume: Int) : DeviceEvent
}
