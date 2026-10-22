package org.librecans.app.ui.common

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.librecans.app.R
import org.librecans.app.data.connection.FailureReason
import org.librecans.protocol.model.AudioCodec
import org.librecans.protocol.model.AutoPowerOff
import org.librecans.protocol.model.ConnectionPriority
import org.librecans.protocol.model.EqPresets
import org.librecans.protocol.model.GeneralSetting
import org.librecans.protocol.model.SpeakToChatSensitivity
import org.librecans.protocol.model.SpeakToChatTimeout
import org.librecans.protocol.model.UpscalingType

@get:StringRes
val AutoPowerOff.label: Int
    get() = when (this) {
        AutoPowerOff.AFTER_5_MIN -> R.string.apo_5_min
        AutoPowerOff.AFTER_15_MIN -> R.string.apo_15_min
        AutoPowerOff.AFTER_30_MIN -> R.string.apo_30_min
        AutoPowerOff.AFTER_60_MIN -> R.string.apo_60_min
        AutoPowerOff.AFTER_180_MIN -> R.string.apo_180_min
        AutoPowerOff.WHEN_REMOVED -> R.string.apo_when_removed
        AutoPowerOff.DISABLED -> R.string.apo_disabled
    }

@get:StringRes
val ConnectionPriority.label: Int
    get() = when (this) {
        ConnectionPriority.SOUND_QUALITY -> R.string.priority_sound_quality
        ConnectionPriority.STABLE_CONNECTION -> R.string.priority_stable_connection
        ConnectionPriority.LOW_LATENCY -> R.string.priority_low_latency
    }

@get:StringRes
val SpeakToChatSensitivity.label: Int
    get() = when (this) {
        SpeakToChatSensitivity.AUTO -> R.string.sensitivity_auto
        SpeakToChatSensitivity.HIGH -> R.string.sensitivity_high
        SpeakToChatSensitivity.LOW -> R.string.sensitivity_low
    }

@get:StringRes
val SpeakToChatTimeout.label: Int
    get() = when (this) {
        SpeakToChatTimeout.SHORT -> R.string.timeout_short
        SpeakToChatTimeout.STANDARD -> R.string.timeout_standard
        SpeakToChatTimeout.LONG -> R.string.timeout_long
        SpeakToChatTimeout.NEVER -> R.string.timeout_never
    }

@get:StringRes
val UpscalingType?.label: Int
    get() = when (this) {
        UpscalingType.DSEE -> R.string.upscaling_dsee
        UpscalingType.DSEE_HX -> R.string.upscaling_dsee_hx
        UpscalingType.DSEE_HX_AI -> R.string.upscaling_dsee_extreme
        UpscalingType.DSEE_ULTIMATE -> R.string.upscaling_dsee_ultimate
        null -> R.string.upscaling_dsee
    }

@get:StringRes
val FailureReason.message: Int
    get() = when (this) {
        FailureReason.BLUETOOTH_UNAVAILABLE -> R.string.failure_bluetooth_unavailable
        FailureReason.BLUETOOTH_OFF -> R.string.failure_bluetooth_off
        FailureReason.NO_PERMISSION -> R.string.failure_no_permission
        FailureReason.NOT_FOUND -> R.string.failure_not_found
        FailureReason.CONNECTION_FAILED -> R.string.failure_connection_failed
        FailureReason.NO_RESPONSE -> R.string.failure_no_response
        FailureReason.UNSUPPORTED_PROTOCOL -> R.string.failure_unsupported_protocol
        FailureReason.CONNECTION_LOST -> R.string.failure_connection_lost
    }

@Composable
fun codecLabel(codec: AudioCodec?): String = when (codec) {
    AudioCodec.SBC -> stringResource(R.string.codec_sbc)
    AudioCodec.AAC -> stringResource(R.string.codec_aac)
    AudioCodec.LDAC -> stringResource(R.string.codec_ldac)
    AudioCodec.APTX -> stringResource(R.string.codec_aptx)
    AudioCodec.APTX_HD -> stringResource(R.string.codec_aptx_hd)
    AudioCodec.LC3 -> stringResource(R.string.codec_lc3)
    else -> stringResource(R.string.unknown)
}

/** Prefers the translated name of a known preset and falls back to the name the headphones send. */
@Composable
fun eqPresetLabel(id: Int, deviceName: String? = null): String = when (id) {
    EqPresets.OFF -> stringResource(R.string.eq_preset_off)
    EqPresets.BRIGHT -> stringResource(R.string.eq_preset_bright)
    EqPresets.EXCITED -> stringResource(R.string.eq_preset_excited)
    EqPresets.MELLOW -> stringResource(R.string.eq_preset_mellow)
    EqPresets.RELAXED -> stringResource(R.string.eq_preset_relaxed)
    EqPresets.VOCAL -> stringResource(R.string.eq_preset_vocal)
    EqPresets.TREBLE -> stringResource(R.string.eq_preset_treble)
    EqPresets.BASS -> stringResource(R.string.eq_preset_bass)
    EqPresets.SPEECH -> stringResource(R.string.eq_preset_speech)
    EqPresets.CUSTOM -> stringResource(R.string.eq_preset_custom)
    EqPresets.USER_1 -> stringResource(R.string.eq_preset_user_1)
    EqPresets.USER_2 -> stringResource(R.string.eq_preset_user_2)
    else -> deviceName ?: stringResource(R.string.eq_preset_unknown, "0x%02X".format(id))
}

/** Known identifiers are translated; unknown ones are turned into readable text. */
@Composable
fun generalSettingTitle(setting: GeneralSetting): String {
    if (!setting.titleIsKey) return setting.title
    return when (setting.title) {
        "MULTIPOINT_SETTING" -> stringResource(R.string.gs_multipoint)
        "SIDETONE_SETTING" -> stringResource(R.string.gs_sidetone)
        "TOUCH_PANEL_SETTING" -> stringResource(R.string.gs_touch_panel)
        else -> humanize(setting.title)
    }
}

@Composable
fun generalSettingSummary(setting: GeneralSetting): String? {
    val summary = setting.summary ?: return null
    if (!setting.titleIsKey) return summary
    return when (summary) {
        "MULTIPOINT_SETTING_SUMMARY" -> stringResource(R.string.gs_multipoint_summary)
        "MULTIPOINT_SETTING_SUMMARY_LDAC_AVAILABLE" -> stringResource(R.string.gs_multipoint_summary_ldac)
        "SIDETONE_SETTING_SUMMARY" -> stringResource(R.string.gs_sidetone_summary)
        "TOUCH_PANEL_SETTING_SUMMARY" -> stringResource(R.string.gs_touch_panel_summary)
        else -> null
    }
}

internal fun humanize(key: String): String = key.removeSuffix("_SETTING").split('_')
    .filter { it.isNotBlank() }
    .joinToString(" ") { it.lowercase() }
    .replaceFirstChar { it.uppercase() }
