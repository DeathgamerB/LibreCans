package org.librecans.app.ui.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.librecans.app.R
import org.librecans.app.ui.common.appViewModel
import org.librecans.app.ui.common.label
import org.librecans.app.ui.common.rememberHaptics
import org.librecans.app.ui.components.AppScaffold
import org.librecans.app.ui.components.OptionsSheet
import org.librecans.protocol.model.AutoPowerOff
import org.librecans.protocol.model.ConnectionPriority
import org.librecans.protocol.model.Features
import org.librecans.protocol.model.SpeakToChatSensitivity
import org.librecans.protocol.model.SpeakToChatTimeout

enum class SettingsSheet { PRIORITY, SENSITIVITY, TIMEOUT, AUTO_POWER_OFF }

@Composable
fun DeviceSettingsEntry(onBack: () -> Unit) {
    val viewModel = appViewModel { DeviceSettingsViewModel(it.headphones) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    DeviceSettingsScreen(
        state = state,
        onBack = onBack,
        actions = DeviceSettingsActions(
            onUpscaling = viewModel::setUpscaling,
            onSpeakToChat = viewModel::setSpeakToChat,
            onHeadGestures = viewModel::setHeadGestures,
            onAdaptiveVolume = viewModel::setAdaptiveVolume,
            onPauseWhenRemoved = viewModel::setPauseWhenRemoved,
            onVoiceGuidance = viewModel::setVoiceGuidance,
            onVoiceGuidanceVolume = viewModel::setVoiceGuidanceVolume,
            onGeneralSetting = viewModel::setGeneralSetting,
            onPriority = viewModel::setConnectionPriority,
            onSensitivity = viewModel::setSpeakToChatSensitivity,
            onTimeout = viewModel::setSpeakToChatTimeout,
            onAutoPowerOff = viewModel::setAutoPowerOff,
            onPowerOff = viewModel::powerOff,
        ),
    )
}

data class DeviceSettingsActions(
    val onUpscaling: (Boolean) -> Unit = {},
    val onSpeakToChat: (Boolean) -> Unit = {},
    val onHeadGestures: (Boolean) -> Unit = {},
    val onAdaptiveVolume: (Boolean) -> Unit = {},
    val onPauseWhenRemoved: (Boolean) -> Unit = {},
    val onVoiceGuidance: (Boolean) -> Unit = {},
    val onVoiceGuidanceVolume: (Int) -> Unit = {},
    val onGeneralSetting: (Int, Boolean) -> Unit = { _, _ -> },
    val onPriority: (ConnectionPriority) -> Unit = {},
    val onSensitivity: (SpeakToChatSensitivity) -> Unit = {},
    val onTimeout: (SpeakToChatTimeout) -> Unit = {},
    val onAutoPowerOff: (AutoPowerOff) -> Unit = {},
    val onPowerOff: () -> Unit = {},
)

@Composable
fun DeviceSettingsScreen(state: DeviceSettingsUiState, onBack: () -> Unit, actions: DeviceSettingsActions) {
    var sheet by rememberSaveable { mutableStateOf<SettingsSheet?>(null) }
    var confirmPowerOff by rememberSaveable { mutableStateOf(false) }
    val haptics = rememberHaptics()
    val headphones = state.headphones
    val titles = SectionTitles(
        sound = stringResource(R.string.settings_section_sound),
        conversation = stringResource(R.string.settings_section_conversation),
        wearing = stringResource(R.string.settings_section_wearing),
        connection = stringResource(R.string.settings_section_connection),
        device = stringResource(R.string.settings_section_device),
    )

    AppScaffold(title = stringResource(R.string.settings_title), onBack = onBack) {
        settingsSections(state, titles, actions, onOpenSheet = { sheet = it })
        if (headphones.supports(Features.POWER_OFF)) {
            item(key = "power_off") {
                Button(
                    onClick = {
                        haptics.tap()
                        confirmPowerOff = true
                    },
                    shapes = ButtonDefaults.shapes(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp).testTag("power_off"),
                ) {
                    Icon(painterResource(R.drawable.ic_power_settings_new), null)
                    Text(stringResource(R.string.settings_power_off), Modifier.padding(start = 8.dp))
                }
            }
        }
    }

    if (confirmPowerOff) {
        AlertDialog(
            onDismissRequest = { confirmPowerOff = false },
            icon = { Icon(painterResource(R.drawable.ic_power_settings_new), null) },
            title = { Text(stringResource(R.string.settings_power_off_confirm_title)) },
            text = { Text(stringResource(R.string.settings_power_off_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    haptics.heavy()
                    confirmPowerOff = false
                    actions.onPowerOff()
                }) { Text(stringResource(R.string.settings_power_off)) }
            },
            dismissButton = { TextButton(onClick = { confirmPowerOff = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }

    when (sheet) {
        SettingsSheet.PRIORITY -> OptionsSheet(
            title = stringResource(R.string.settings_connection_priority),
            options = ConnectionPriority.entries.filter { it != ConnectionPriority.LOW_LATENCY },
            selected = headphones.connectionPriority,
            label = { stringResource(it.label) },
            onSelect = actions.onPriority,
            onDismiss = { sheet = null },
        )
        SettingsSheet.SENSITIVITY -> OptionsSheet(
            title = stringResource(R.string.settings_speak_to_chat_sensitivity),
            options = SpeakToChatSensitivity.entries,
            selected = headphones.speakToChat?.sensitivity,
            label = { stringResource(it.label) },
            onSelect = actions.onSensitivity,
            onDismiss = { sheet = null },
        )
        SettingsSheet.TIMEOUT -> OptionsSheet(
            title = stringResource(R.string.settings_speak_to_chat_timeout),
            options = SpeakToChatTimeout.entries,
            selected = headphones.speakToChat?.timeout,
            label = { stringResource(it.label) },
            onSelect = actions.onTimeout,
            onDismiss = { sheet = null },
        )
        SettingsSheet.AUTO_POWER_OFF -> OptionsSheet(
            title = stringResource(R.string.settings_auto_power_off),
            options = autoPowerOffOptions(state),
            selected = headphones.autoPowerOff?.current,
            label = { stringResource(it.label) },
            onSelect = actions.onAutoPowerOff,
            onDismiss = { sheet = null },
        )
        null -> Unit
    }
}

data class SectionTitles(val sound: String, val conversation: String, val wearing: String, val connection: String, val device: String)

fun autoPowerOffOptions(state: DeviceSettingsUiState): List<AutoPowerOff> {
    val reported = state.headphones.autoPowerOffOptions
    if (reported.isNotEmpty()) return reported.sortedBy { AutoPowerOffOrder.indexOf(it) }
    val wearing = state.headphones.autoPowerOff?.withWearingDetection == true
    return AutoPowerOffOrder.filter { wearing || it != AutoPowerOff.WHEN_REMOVED }.filter { it != AutoPowerOff.AFTER_15_MIN }
}

private val AutoPowerOffOrder = listOf(
    AutoPowerOff.WHEN_REMOVED,
    AutoPowerOff.AFTER_5_MIN,
    AutoPowerOff.AFTER_15_MIN,
    AutoPowerOff.AFTER_30_MIN,
    AutoPowerOff.AFTER_60_MIN,
    AutoPowerOff.AFTER_180_MIN,
    AutoPowerOff.DISABLED,
)
