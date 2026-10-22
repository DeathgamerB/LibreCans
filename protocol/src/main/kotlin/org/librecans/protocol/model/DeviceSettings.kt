package org.librecans.protocol.model

enum class SpeakToChatSensitivity(val code: Int) {
    AUTO(0x00), HIGH(0x01), LOW(0x02);

    companion object {
        fun fromCode(code: Int): SpeakToChatSensitivity? = entries.firstOrNull { it.code == code }
    }
}

/** How long Speak-to-Chat stays active after you stop talking. */
enum class SpeakToChatTimeout(val code: Int) {
    SHORT(0x00), STANDARD(0x01), LONG(0x02), NEVER(0x03);

    companion object {
        fun fromCode(code: Int): SpeakToChatTimeout? = entries.firstOrNull { it.code == code }
    }
}

data class SpeakToChat(
    val enabled: Boolean,
    val sensitivity: SpeakToChatSensitivity? = null,
    val timeout: SpeakToChatTimeout? = null,
)

enum class AutoPowerOff(val code: Int) {
    AFTER_5_MIN(0x00),
    AFTER_30_MIN(0x01),
    AFTER_60_MIN(0x02),
    AFTER_180_MIN(0x03),
    AFTER_15_MIN(0x04),
    WHEN_REMOVED(0x10),
    DISABLED(0x11);

    companion object {
        fun fromCode(code: Int): AutoPowerOff? = entries.firstOrNull { it.code == code }
    }
}

data class AutoPowerOffSetting(
    val current: AutoPowerOff,
    val lastSelected: Int,
    val withWearingDetection: Boolean,
    val options: List<AutoPowerOff> = emptyList(),
)

enum class ConnectionPriority(val code: Int) {
    SOUND_QUALITY(0x00), STABLE_CONNECTION(0x01), LOW_LATENCY(0x02);

    companion object {
        fun fromCode(code: Int): ConnectionPriority? = entries.firstOrNull { it.code == code }
    }
}

enum class AudioCodec(val code: Int) {
    UNSETTLED(0x00), SBC(0x01), AAC(0x02), LDAC(0x10), APTX(0x20), APTX_HD(0x21), LC3(0x30), OTHER(0xFF);

    companion object {
        fun fromCode(code: Int): AudioCodec = entries.firstOrNull { it.code == code } ?: OTHER
    }
}

enum class UpscalingType(val code: Int) {
    DSEE_HX(0x00), DSEE(0x01), DSEE_HX_AI(0x02), DSEE_ULTIMATE(0x03);

    companion object {
        fun fromCode(code: Int): UpscalingType? = entries.firstOrNull { it.code == code }
    }
}

enum class PlaybackStatus(val code: Int) {
    UNSETTLED(0x00), PLAYING(0x01), PAUSED(0x02), STOPPED(0x03);

    companion object {
        fun fromCode(code: Int): PlaybackStatus = entries.firstOrNull { it.code == code } ?: UNSETTLED
    }
}

enum class PlaybackCommand(val code: Int) {
    PAUSE(0x01), NEXT_TRACK(0x02), PREVIOUS_TRACK(0x03), PLAY(0x07)
}

data class Playback(val volume: Int? = null, val status: PlaybackStatus = PlaybackStatus.UNSETTLED) {
    companion object {
        const val MAX_VOLUME = 30
    }
}

/**
 * A device driven toggle ("General setting"). Names come from the headphones either as raw text
 * or as an identifier such as MULTIPOINT_SETTING that the app translates.
 */
data class GeneralSetting(
    val id: Int,
    val title: String,
    val summary: String?,
    val titleIsKey: Boolean,
    val enabled: Boolean? = null,
)

data class VoiceGuidance(val enabled: Boolean? = null, val volume: Int? = null) {
    companion object {
        val VOLUME_RANGE = -2..2
    }
}

data class DeviceIdentity(
    val modelName: String? = null,
    val firmware: String? = null,
    val series: Int? = null,
    val color: Int? = null,
)
