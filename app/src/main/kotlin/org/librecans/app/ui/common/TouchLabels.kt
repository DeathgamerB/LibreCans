package org.librecans.app.ui.common

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.librecans.app.R
import org.librecans.protocol.model.TouchFunctions as F
import org.librecans.protocol.model.TouchPresets as P

@Composable
fun touchPresetLabel(preset: Int): String = when (preset) {
    P.AMBIENT_SOUND_CONTROL -> stringResource(R.string.touch_preset_ambient)
    P.AMBIENT_SOUND_CONTROL_QUICK_ACCESS -> stringResource(R.string.touch_preset_ambient_quick_access)
    P.PLAYBACK_CONTROL, P.PLAYBACK_CONTROL_VOICE_ASSISTANT_LIMITATION -> stringResource(R.string.touch_preset_playback)
    P.TRACK_CONTROL -> stringResource(R.string.touch_preset_track)
    P.VOLUME_CONTROL -> stringResource(R.string.touch_preset_volume)
    P.VOICE_RECOGNITION -> stringResource(R.string.touch_preset_voice_assistant)
    P.GOOGLE_ASSISTANT -> stringResource(R.string.touch_preset_google)
    P.AMAZON_ALEXA -> stringResource(R.string.touch_preset_alexa)
    P.QUICK_ACCESS -> stringResource(R.string.touch_preset_quick_access)
    P.NO_FUNCTION -> stringResource(R.string.touch_preset_none)
    else -> stringResource(R.string.touch_preset_unknown, hex(preset))
}

/** A word or two for tight spots such as the dashboard summary. */
@Composable
fun touchPresetShortLabel(preset: Int): String = stringResource(
    when (preset) {
        P.AMBIENT_SOUND_CONTROL, P.AMBIENT_SOUND_CONTROL_QUICK_ACCESS -> R.string.touch_short_noise
        P.PLAYBACK_CONTROL, P.PLAYBACK_CONTROL_VOICE_ASSISTANT_LIMITATION -> R.string.touch_short_playback
        P.TRACK_CONTROL -> R.string.touch_short_track
        P.VOLUME_CONTROL -> R.string.touch_short_volume
        P.VOICE_RECOGNITION, P.GOOGLE_ASSISTANT, P.AMAZON_ALEXA -> R.string.touch_short_assistant
        P.QUICK_ACCESS -> R.string.touch_preset_quick_access
        P.NO_FUNCTION -> R.string.touch_short_none
        else -> R.string.touch_short_custom
    },
)

/** One line on what a preset does, shown while choosing it. */
@Composable
fun touchPresetDescription(preset: Int): String = stringResource(
    when (preset) {
        P.AMBIENT_SOUND_CONTROL -> R.string.touch_desc_ambient
        P.AMBIENT_SOUND_CONTROL_QUICK_ACCESS -> R.string.touch_desc_ambient_quick_access
        P.PLAYBACK_CONTROL, P.PLAYBACK_CONTROL_VOICE_ASSISTANT_LIMITATION -> R.string.touch_desc_playback
        P.TRACK_CONTROL -> R.string.touch_desc_track
        P.VOLUME_CONTROL -> R.string.touch_desc_volume
        P.VOICE_RECOGNITION, P.GOOGLE_ASSISTANT, P.AMAZON_ALEXA -> R.string.touch_desc_assistant
        P.QUICK_ACCESS -> R.string.touch_desc_quick_access
        P.NO_FUNCTION -> R.string.touch_no_gestures
        else -> R.string.touch_desc_custom
    },
)

@DrawableRes
fun touchPresetIcon(preset: Int): Int = when (preset) {
    P.AMBIENT_SOUND_CONTROL, P.AMBIENT_SOUND_CONTROL_QUICK_ACCESS -> R.drawable.ic_noise_control_on
    P.PLAYBACK_CONTROL, P.PLAYBACK_CONTROL_VOICE_ASSISTANT_LIMITATION, P.TRACK_CONTROL -> R.drawable.ic_music_note
    P.VOLUME_CONTROL -> R.drawable.ic_volume_up
    P.VOICE_RECOGNITION, P.GOOGLE_ASSISTANT, P.AMAZON_ALEXA -> R.drawable.ic_mic
    P.QUICK_ACCESS -> R.drawable.ic_touch_app
    P.NO_FUNCTION -> R.drawable.ic_do_not_disturb_on
    else -> R.drawable.ic_gesture
}

@StringRes
fun ambientCycleLabel(function: Int): Int = when (function) {
    F.NC_ASM -> R.string.cycle_nc_asm
    F.NC_OFF -> R.string.cycle_nc_off
    F.ASM_OFF -> R.string.cycle_asm_off
    else -> R.string.cycle_nc_asm_off
}

private fun hex(value: Int) = "0x%02X".format(value)
