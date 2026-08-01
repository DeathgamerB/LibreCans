package org.librecans.app

import org.librecans.protocol.EqPreset
import org.librecans.protocol.SonyProtocolVersion

data class BluetoothDeviceState(
    val connected: Boolean = false,
    val connecting: Boolean = false,
    val deviceName: String = "",
    val deviceMac: String = "",
    val protocolVersion: SonyProtocolVersion? = null,
    val errorMessage: String? = null,

    // Ambient Sound Control
    val ambientSoundControl: Boolean = false,
    val asmLevel: Int = 10,
    val focusOnVoice: Boolean = false,
    val focusOnVoiceAvailable: Boolean = false,

    // Battery Levels
    val batteryLevel: Int = -1,
    val batteryCharging: Boolean = false,
    val hasDualBattery: Boolean = false,
    val batteryLeft: Int = -1,
    val batteryRight: Int = -1,
    val batteryCase: Int = -1,

    // Equalizer
    val eqPreset: EqPreset = EqPreset.OFF,
    val eqClearBass: Int = 0,
    val eqBands: List<Int> = listOf(0, 0, 0, 0, 0),

    // DSEE
    val dsee: Boolean = false,

    // Capability-gated Extras
    val hasAutoPowerOff: Boolean = false,
    val autoPowerOff: Int = 0, // index 0..5
    val hasFirmware: Boolean = false,
    val firmware: String = "",
    val hasCodec: Boolean = false,
    val codec: String = "",
    val hasSpeakToChat: Boolean = false,
    val speakToChat: Boolean = false,
    val hasAdaptiveVolume: Boolean = false,
    val adaptiveVolume: Boolean = false
) {
    val isFocusOnVoiceAvailable: Boolean
        get() = ambientSoundControl && asmLevel > 2
}
