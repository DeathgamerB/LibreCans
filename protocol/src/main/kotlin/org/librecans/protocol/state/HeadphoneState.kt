package org.librecans.protocol.state

import org.librecans.protocol.model.AudioCodec
import org.librecans.protocol.model.AutoPowerOff
import org.librecans.protocol.model.AutoPowerOffSetting
import org.librecans.protocol.model.Batteries
import org.librecans.protocol.model.ConnectionPriority
import org.librecans.protocol.model.DeviceIdentity
import org.librecans.protocol.model.Equalizer
import org.librecans.protocol.model.EqualizerCapability
import org.librecans.protocol.model.Feature
import org.librecans.protocol.model.GeneralSetting
import org.librecans.protocol.model.NoiseControl
import org.librecans.protocol.model.Playback
import org.librecans.protocol.model.SpeakToChat
import org.librecans.protocol.model.TouchControls
import org.librecans.protocol.model.UpscalingType
import org.librecans.protocol.model.VoiceGuidance

/** Everything known about the connected headphones. Null means unknown or unsupported. */
data class HeadphoneState(
    val protocolVersion: Long? = null,
    val identity: DeviceIdentity = DeviceIdentity(),
    val features: Set<Feature> = emptySet(),
    val batteries: Batteries = Batteries(),
    val codec: AudioCodec? = null,
    val noiseControl: NoiseControl? = null,
    val equalizer: Equalizer? = null,
    val equalizerCapability: EqualizerCapability? = null,
    val upscaling: Boolean? = null,
    val upscalingType: UpscalingType? = null,
    val connectionPriority: ConnectionPriority? = null,
    val autoPowerOff: AutoPowerOffSetting? = null,
    val autoPowerOffOptions: List<AutoPowerOff> = emptyList(),
    val touchControls: TouchControls? = null,
    val speakToChat: SpeakToChat? = null,
    val pauseWhenRemoved: Boolean? = null,
    val headGestures: Boolean? = null,
    val adaptiveVolume: Boolean? = null,
    val generalSettings: List<GeneralSetting> = emptyList(),
    val playback: Playback? = null,
    val voiceGuidance: VoiceGuidance? = null,
) {
    fun supports(feature: Feature): Boolean = feature in features
}
