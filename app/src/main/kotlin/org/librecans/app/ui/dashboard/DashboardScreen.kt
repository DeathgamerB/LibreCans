package org.librecans.app.ui.dashboard

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.librecans.app.R
import org.librecans.app.data.bluetooth.DeviceKind
import org.librecans.app.data.bluetooth.SonyDevices
import org.librecans.app.ui.common.appViewModel
import org.librecans.app.ui.common.codecLabel
import org.librecans.app.ui.common.eqPresetLabel
import org.librecans.app.ui.common.rememberHaptics
import org.librecans.app.ui.common.touchPresetLabel
import org.librecans.app.ui.components.AppScaffold
import org.librecans.app.ui.components.NavigationItem
import org.librecans.app.ui.components.Segment
import org.librecans.app.ui.components.ShapedIcon
import org.librecans.app.ui.components.section
import org.librecans.protocol.model.NoiseMode
import org.librecans.protocol.model.TouchKeys

@Composable
fun DashboardEntry(
    onOpenEqualizer: () -> Unit,
    onOpenTouchControls: () -> Unit,
    onOpenDeviceSettings: () -> Unit,
) {
    val viewModel = appViewModel { DashboardViewModel(it.headphones) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    DashboardScreen(
        state = state,
        actions = DashboardActions(
            onNoiseMode = viewModel::setNoiseMode,
            onAmbientLevel = viewModel::setAmbientLevel,
            onFocusOnVoice = viewModel::setFocusOnVoice,
            onTogglePlayback = viewModel::togglePlayback,
            onSkip = viewModel::skip,
            onVolume = viewModel::setVolume,
            onOpenEqualizer = onOpenEqualizer,
            onOpenTouchControls = onOpenTouchControls,
            onOpenDeviceSettings = onOpenDeviceSettings,
            onDisconnect = viewModel::disconnect,
            onSimulateTap = viewModel::simulateTap,
        ),
    )
}

data class DashboardActions(
    val onNoiseMode: (NoiseMode) -> Unit = {},
    val onAmbientLevel: (Int, Boolean) -> Unit = { _, _ -> },
    val onFocusOnVoice: (Boolean) -> Unit = {},
    val onTogglePlayback: () -> Unit = {},
    val onSkip: (Boolean) -> Unit = {},
    val onVolume: (Int) -> Unit = {},
    val onOpenEqualizer: () -> Unit = {},
    val onOpenTouchControls: () -> Unit = {},
    val onOpenDeviceSettings: () -> Unit = {},
    val onDisconnect: () -> Unit = {},
    val onSimulateTap: () -> Unit = {},
)

private val HeaderFadeDistance = 48.dp
private val HeroCollapseDistance = 320.dp

@Composable
fun DashboardScreen(state: DashboardUiState, actions: DashboardActions) {
    val headphones = state.headphones
    val target = state.connection.target
    val name = headphones.identity.modelName ?: target?.name.orEmpty()
    val kind = target?.kind ?: SonyDevices.kindOf(name).takeIf { name.isNotEmpty() } ?: DeviceKind.EARBUDS
    val status = listOfNotNull(
        stringResource(R.string.status_connected),
        headphones.codec?.let { codecLabel(it) },
        stringResource(R.string.dashboard_demo_badge).takeIf { target?.isDemo == true },
    ).joinToString(" · ")
    val customizeTitle = stringResource(R.string.dashboard_section_customize)
    val density = LocalDensity.current
    val listState = rememberLazyListState()
    // How far the hero has scrolled, read only while drawing so scrolling never recomposes.
    val scrolled = { if (listState.firstVisibleItemIndex > 0) Float.MAX_VALUE else listState.firstVisibleItemScrollOffset.toFloat() }
    val headerProgress = { (scrolled() / with(density) { HeaderFadeDistance.toPx() }).coerceIn(0f, 1f) }
    val heroCollapse = { (scrolled() / with(density) { HeroCollapseDistance.toPx() }).coerceIn(0f, 1f) }
    val haptics = rememberHaptics()

    AppScaffold(
        title = null,
        listState = listState,
        header = { DashboardHeader(name, status, headerProgress) },
    ) {
        item(key = "hero") {
            HeroCard(kind, headphones.batteries, headphones.noiseControl?.mode, Modifier.animateItem(), heroCollapse)
        }
        if (target?.isDemo == true) {
            item(key = "simulate") {
                AssistChip(
                    onClick = {
                        haptics.key()
                        actions.onSimulateTap()
                    },
                    label = { Text(stringResource(R.string.dashboard_simulate_tap)) },
                    leadingIcon = { Icon(painterResource(R.drawable.ic_touch_app), null, Modifier.size(AssistChipDefaults.IconSize)) },
                    modifier = Modifier.padding(top = 8.dp).testTag("simulate_tap"),
                )
            }
        }
        headphones.noiseControl?.let { control ->
            item(key = "noise") {
                NoiseControlCard(
                    control = control,
                    onModeChange = actions.onNoiseMode,
                    onLevelChange = actions.onAmbientLevel,
                    onFocusOnVoiceChange = actions.onFocusOnVoice,
                    modifier = Modifier.padding(top = 12.dp).animateItem(),
                )
            }
        }
        headphones.playback?.let { playback ->
            item(key = "playback") {
                PlaybackCard(
                    playback = playback,
                    onTogglePlayback = actions.onTogglePlayback,
                    onSkip = actions.onSkip,
                    onVolumeChange = actions.onVolume,
                    modifier = Modifier.padding(top = 12.dp).animateItem(),
                )
            }
        }

        val links = buildList<@Composable (Segment) -> Unit> {
            headphones.equalizer?.let { eq ->
                add { segment ->
                    NavigationItem(
                        title = stringResource(R.string.dashboard_equalizer),
                        summary = eqPresetLabel(eq.presetId, headphones.equalizerCapability?.presets?.firstOrNull { it.id == eq.presetId }?.name),
                        onClick = actions.onOpenEqualizer,
                        segment = segment,
                        leading = { ShapedIcon(R.drawable.ic_graphic_eq) },
                        modifier = Modifier.testTag("link_equalizer"),
                    )
                }
            }
            headphones.touchControls?.takeIf { it.presets.size >= 2 }?.let { touch ->
                add { segment ->
                    NavigationItem(
                        title = stringResource(R.string.dashboard_touch_controls),
                        summary = stringResource(
                            R.string.dashboard_touch_summary,
                            touchPresetLabel(touch.presets[TouchKeys.LEFT]),
                            touchPresetLabel(touch.presets[TouchKeys.RIGHT]),
                        ),
                        onClick = actions.onOpenTouchControls,
                        segment = segment,
                        leading = { ShapedIcon(R.drawable.ic_touch_app) },
                        modifier = Modifier.testTag("link_touch"),
                    )
                }
            }
            add { segment ->
                NavigationItem(
                    title = stringResource(R.string.dashboard_device_settings),
                    summary = stringResource(R.string.dashboard_device_settings_summary),
                    onClick = actions.onOpenDeviceSettings,
                    segment = segment,
                    leading = { ShapedIcon(R.drawable.ic_tune) },
                    modifier = Modifier.testTag("link_settings"),
                )
            }
        }
        section("customize", customizeTitle) {
            links.forEachIndexed { index, link ->
                item(key = "link_$index") { link(Segment(index, links.size)) }
            }
        }

        item(key = "disconnect") {
            OutlinedButton(
                onClick = {
                    haptics.heavy()
                    actions.onDisconnect()
                },
                shapes = ButtonDefaults.shapes(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp).testTag("disconnect"),
            ) {
                Icon(painterResource(R.drawable.ic_link_off), null)
                Text(stringResource(R.string.action_disconnect), Modifier.padding(start = 8.dp))
            }
        }
    }
}
