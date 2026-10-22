package org.librecans.app.ui.common

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.librecans.app.R
import org.librecans.protocol.model.TouchActions as A
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
fun touchActionLabel(action: Int): Int = when (action) {
    A.SINGLE_TAP -> R.string.touch_action_tap
    A.DOUBLE_TAP -> R.string.touch_action_double_tap
    A.TRIPLE_TAP -> R.string.touch_action_triple_tap
    A.REPEAT_TAP -> R.string.touch_action_repeat_tap
    A.TAP_AND_HOLD -> R.string.touch_action_hold
    A.DOUBLE_TAP_AND_HOLD -> R.string.touch_action_double_tap_hold
    else -> R.string.touch_action_long_press
}

@Composable
fun touchFunctionLabel(function: Int): String = when (function) {
    F.NO_FUNCTION -> stringResource(R.string.touch_function_none)
    F.NC_ASM_OFF -> stringResource(R.string.touch_function_nc_asm_off)
    F.NC_ASM -> stringResource(R.string.touch_function_nc_asm)
    F.NC_OFF -> stringResource(R.string.touch_function_nc_off)
    F.ASM_OFF -> stringResource(R.string.touch_function_asm_off)
    F.QUICK_ATTENTION -> stringResource(R.string.touch_function_quick_attention)
    F.NC_OPTIMIZER -> stringResource(R.string.touch_function_nc_optimizer)
    F.PLAY_PAUSE -> stringResource(R.string.touch_function_play_pause)
    F.NEXT_TRACK -> stringResource(R.string.touch_function_next)
    F.PREVIOUS_TRACK -> stringResource(R.string.touch_function_previous)
    F.VOLUME_UP -> stringResource(R.string.touch_function_volume_up)
    F.VOLUME_DOWN -> stringResource(R.string.touch_function_volume_down)
    F.VOICE_RECOGNITION -> stringResource(R.string.touch_function_voice_assistant)
    F.GET_YOUR_NOTIFICATION -> stringResource(R.string.touch_function_notifications)
    F.TALK_TO_GOOGLE_ASSISTANT, F.STOP_GOOGLE_ASSISTANT -> stringResource(R.string.touch_function_google)
    F.VOICE_INPUT_AMAZON_ALEXA -> stringResource(R.string.touch_function_alexa)
    F.SPOTIFY_TAP -> stringResource(R.string.touch_function_spotify)
    F.QUICK_ACCESS_1, F.QUICK_ACCESS_2 -> stringResource(R.string.touch_function_quick_access)
    else -> stringResource(R.string.touch_function_other, hex(function))
}

@StringRes
fun ambientCycleLabel(function: Int): Int = when (function) {
    F.NC_ASM -> R.string.cycle_nc_asm
    F.NC_OFF -> R.string.cycle_nc_off
    F.ASM_OFF -> R.string.cycle_asm_off
    else -> R.string.cycle_nc_asm_off
}

private fun hex(value: Int) = "0x%02X".format(value)
