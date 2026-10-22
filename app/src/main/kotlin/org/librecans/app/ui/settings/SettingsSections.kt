package org.librecans.app.ui.settings

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import org.librecans.app.R
import org.librecans.app.ui.common.codecLabel
import org.librecans.app.ui.common.generalSettingSummary
import org.librecans.app.ui.common.generalSettingTitle
import org.librecans.app.ui.common.label
import org.librecans.app.ui.components.GroupBuilder
import org.librecans.app.ui.components.InfoItem
import org.librecans.app.ui.components.NavigationItem
import org.librecans.app.ui.components.SliderItem
import org.librecans.app.ui.components.SwitchItem
import org.librecans.app.ui.components.segmentedGroup
import org.librecans.protocol.model.GeneralSetting
import org.librecans.protocol.model.VoiceGuidance

/** The sidetone toggle belongs with the call settings; other general settings are connection related. */
private fun GeneralSetting.isConversation() = title == "SIDETONE_SETTING"

fun LazyListScope.settingsSections(
    state: DeviceSettingsUiState,
    titles: SectionTitles,
    actions: DeviceSettingsActions,
    onOpenSheet: (SettingsSheet) -> Unit,
) {
    val h = state.headphones
    val generalSettings = h.generalSettings.filter { it.title.isNotEmpty() && it.enabled != null }

    segmentedGroup("sound", titles.sound) {
        h.upscaling?.let { enabled ->
            row("upscaling") { segment, modifier ->
                SwitchItem(
                    title = stringResource(R.string.settings_upscaling),
                    summary = stringResource(R.string.settings_upscaling_summary, stringResource(h.upscalingType.label)),
                    icon = R.drawable.ic_auto_awesome,
                    checked = enabled,
                    onCheckedChange = actions.onUpscaling,
                    segment = segment,
                    modifier = modifier.testTag("setting_upscaling"),
                )
            }
        }
        h.connectionPriority?.let { priority ->
            row("priority") { segment, modifier ->
                NavigationItem(
                    title = stringResource(R.string.settings_connection_priority),
                    summary = stringResource(priority.label),
                    icon = R.drawable.ic_high_quality,
                    onClick = { onOpenSheet(SettingsSheet.PRIORITY) },
                    segment = segment,
                    modifier = modifier,
                )
            }
        }
        h.voiceGuidance?.enabled?.let { enabled ->
            row("voice_guidance") { segment, modifier ->
                SwitchItem(
                    title = stringResource(R.string.settings_voice_guidance),
                    summary = stringResource(R.string.settings_voice_guidance_summary),
                    icon = R.drawable.ic_campaign,
                    checked = enabled,
                    onCheckedChange = actions.onVoiceGuidance,
                    segment = segment,
                    modifier = modifier.testTag("setting_voice_guidance"),
                )
            }
            h.voiceGuidance?.volume?.takeIf { enabled }?.let { volume ->
                row("voice_guidance_volume") { segment, modifier ->
                    SliderItem(
                        title = stringResource(R.string.settings_voice_guidance_volume),
                        value = volume,
                        range = VoiceGuidance.VOLUME_RANGE,
                        onValueChange = {},
                        onValueChangeFinished = actions.onVoiceGuidanceVolume,
                        valueText = { if (it > 0) "+$it" else "$it" },
                        segment = segment,
                        modifier = modifier,
                    )
                }
            }
        }
    }

    segmentedGroup("conversation", titles.conversation) {
        h.speakToChat?.let { stc ->
            row("speak_to_chat") { segment, modifier ->
                SwitchItem(
                    title = stringResource(R.string.settings_speak_to_chat),
                    summary = stringResource(R.string.settings_speak_to_chat_summary),
                    icon = R.drawable.ic_record_voice_over,
                    checked = stc.enabled,
                    onCheckedChange = actions.onSpeakToChat,
                    segment = segment,
                    modifier = modifier.testTag("setting_speak_to_chat"),
                )
            }
            stc.sensitivity?.let { sensitivity ->
                row("stc_sensitivity") { segment, modifier ->
                    NavigationItem(
                        title = stringResource(R.string.settings_speak_to_chat_sensitivity),
                        summary = stringResource(sensitivity.label),
                        icon = R.drawable.ic_mic,
                        enabled = stc.enabled,
                        onClick = { onOpenSheet(SettingsSheet.SENSITIVITY) },
                        segment = segment,
                        modifier = modifier,
                    )
                }
            }
            stc.timeout?.let { timeout ->
                row("stc_timeout") { segment, modifier ->
                    NavigationItem(
                        title = stringResource(R.string.settings_speak_to_chat_timeout),
                        summary = stringResource(timeout.label),
                        icon = R.drawable.ic_timer,
                        enabled = stc.enabled,
                        onClick = { onOpenSheet(SettingsSheet.TIMEOUT) },
                        segment = segment,
                        modifier = modifier,
                    )
                }
            }
        }
        h.headGestures?.let { enabled ->
            row("head_gestures") { segment, modifier ->
                SwitchItem(
                    title = stringResource(R.string.settings_head_gestures),
                    summary = stringResource(R.string.settings_head_gestures_summary),
                    icon = R.drawable.ic_gesture,
                    checked = enabled,
                    onCheckedChange = actions.onHeadGestures,
                    segment = segment,
                    modifier = modifier.testTag("setting_head_gestures"),
                )
            }
        }
        generalSettings.filter { it.isConversation() }.forEach { setting -> generalSettingRow(setting, actions) }
    }

    segmentedGroup("wearing", titles.wearing) {
        h.pauseWhenRemoved?.let { enabled ->
            row("pause_when_removed") { segment, modifier ->
                SwitchItem(
                    title = stringResource(R.string.settings_pause_when_removed),
                    summary = stringResource(R.string.settings_pause_when_removed_summary),
                    icon = R.drawable.ic_sensors,
                    checked = enabled,
                    onCheckedChange = actions.onPauseWhenRemoved,
                    segment = segment,
                    modifier = modifier.testTag("setting_pause_when_removed"),
                )
            }
        }
        h.adaptiveVolume?.let { enabled ->
            row("adaptive_volume") { segment, modifier ->
                SwitchItem(
                    title = stringResource(R.string.settings_adaptive_volume),
                    summary = stringResource(R.string.settings_adaptive_volume_summary),
                    icon = R.drawable.ic_volume_up,
                    checked = enabled,
                    onCheckedChange = actions.onAdaptiveVolume,
                    segment = segment,
                    modifier = modifier,
                )
            }
        }
        h.autoPowerOff?.let { setting ->
            row("auto_power_off") { segment, modifier ->
                NavigationItem(
                    title = stringResource(R.string.settings_auto_power_off),
                    summary = stringResource(setting.current.label),
                    icon = R.drawable.ic_power_settings_new,
                    onClick = { onOpenSheet(SettingsSheet.AUTO_POWER_OFF) },
                    segment = segment,
                    modifier = modifier.testTag("setting_auto_power_off"),
                )
            }
        }
    }

    segmentedGroup("connection", titles.connection) {
        generalSettings.filterNot { it.isConversation() }.forEach { setting -> generalSettingRow(setting, actions) }
    }

    segmentedGroup("device", titles.device) {
        h.identity.modelName?.let { name ->
            row("model") { segment, modifier -> InfoItem(stringResource(R.string.settings_model), name, segment, modifier, R.drawable.ic_headphones) }
        }
        h.identity.firmware?.let { firmware ->
            row("firmware") { segment, modifier -> InfoItem(stringResource(R.string.settings_firmware), firmware, segment, modifier, R.drawable.ic_code) }
        }
        h.codec?.let { codec ->
            row("codec") { segment, modifier -> InfoItem(stringResource(R.string.settings_codec), codecLabel(codec), segment, modifier, R.drawable.ic_graphic_eq) }
        }
        state.target?.takeIf { !it.isDemo }?.let { target ->
            row("address") { segment, modifier -> InfoItem(stringResource(R.string.settings_address), target.address, segment, modifier, R.drawable.ic_bluetooth) }
        }
        h.protocolVersion?.let { version ->
            row("protocol") { segment, modifier ->
                InfoItem(
                    stringResource(R.string.settings_protocol),
                    stringResource(R.string.settings_protocol_value, "%08X".format(version)),
                    segment,
                    modifier,
                    R.drawable.ic_info,
                )
            }
        }
    }
}

private fun GroupBuilder.generalSettingRow(setting: GeneralSetting, actions: DeviceSettingsActions) {
    row("gs_${setting.id}") { segment, modifier ->
        SwitchItem(
            title = generalSettingTitle(setting),
            summary = generalSettingSummary(setting),
            icon = if (setting.isConversation()) R.drawable.ic_settings_voice else R.drawable.ic_devices,
            checked = setting.enabled == true,
            onCheckedChange = { actions.onGeneralSetting(setting.id, it) },
            segment = segment,
            modifier = modifier.testTag("setting_gs_${setting.id}"),
        )
    }
}
