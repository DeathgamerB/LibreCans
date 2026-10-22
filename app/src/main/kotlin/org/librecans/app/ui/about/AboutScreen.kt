package org.librecans.app.ui.about

import android.content.Intent
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.librecans.app.BuildConfig
import org.librecans.app.R
import org.librecans.app.data.settings.AppSettings
import org.librecans.app.data.settings.ThemeMode
import org.librecans.app.ui.common.appViewModel
import org.librecans.app.ui.common.rememberHaptics
import org.librecans.app.ui.components.AppScaffold
import org.librecans.app.ui.components.CardSurface
import org.librecans.app.ui.components.NavigationItem
import org.librecans.app.ui.components.SwitchItem
import org.librecans.app.ui.components.segmentedGroup

private const val SOURCE_URL = "https://github.com/DeathgamerB/LibreCans"

@Composable
fun AboutEntry(onBack: () -> Unit, onOpenDiagnostics: () -> Unit) {
    val viewModel = appViewModel { AboutViewModel(it.settings) }
    val settings by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    AboutScreen(
        settings = settings,
        onBack = onBack,
        actions = AboutActions(
            onDynamicColor = viewModel::setDynamicColor,
            onThemeMode = viewModel::setThemeMode,
            onAutoConnect = viewModel::setAutoConnect,
            onKeepConnected = viewModel::setKeepConnected,
            onDiagnostics = viewModel::setDiagnostics,
            onOpenDiagnostics = onOpenDiagnostics,
            onOpenSource = { context.startActivity(Intent(Intent.ACTION_VIEW, SOURCE_URL.toUri())) },
        ),
    )
}

data class AboutActions(
    val onDynamicColor: (Boolean) -> Unit = {},
    val onThemeMode: (ThemeMode) -> Unit = {},
    val onAutoConnect: (Boolean) -> Unit = {},
    val onKeepConnected: (Boolean) -> Unit = {},
    val onDiagnostics: (Boolean) -> Unit = {},
    val onOpenDiagnostics: () -> Unit = {},
    val onOpenSource: () -> Unit = {},
)

@Composable
fun AboutScreen(settings: AppSettings, onBack: () -> Unit, actions: AboutActions) {
    val appearance = stringResource(R.string.about_section_appearance)
    val behavior = stringResource(R.string.about_section_behavior)
    val diagnostics = stringResource(R.string.about_section_diagnostics)
    val info = stringResource(R.string.about_section_info)

    AppScaffold(title = stringResource(R.string.about_title), onBack = onBack) {
        item(key = "header") { AppHeader() }

        segmentedGroup("appearance", appearance) {
            row("theme") { _, modifier -> ThemeSelector(settings.themeMode, actions.onThemeMode, modifier) }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                row("dynamic") { segment, modifier ->
                    SwitchItem(
                        title = stringResource(R.string.about_dynamic_color),
                        summary = stringResource(R.string.about_dynamic_color_summary),
                        icon = R.drawable.ic_palette,
                        checked = settings.dynamicColor,
                        onCheckedChange = actions.onDynamicColor,
                        segment = segment,
                        modifier = modifier,
                    )
                }
            }
        }
        segmentedGroup("behavior", behavior) {
            row("auto_connect") { segment, modifier ->
                SwitchItem(
                    title = stringResource(R.string.about_auto_connect),
                    summary = stringResource(R.string.about_auto_connect_summary),
                    icon = R.drawable.ic_bluetooth_searching,
                    checked = settings.autoConnect,
                    onCheckedChange = actions.onAutoConnect,
                    segment = segment,
                    modifier = modifier.testTag("setting_auto_connect"),
                )
            }
            row("keep_connected") { segment, modifier ->
                SwitchItem(
                    title = stringResource(R.string.about_keep_connected),
                    summary = stringResource(R.string.about_keep_connected_summary),
                    icon = R.drawable.ic_bluetooth_connected,
                    checked = settings.keepConnectedInBackground,
                    onCheckedChange = actions.onKeepConnected,
                    segment = segment,
                    modifier = modifier,
                )
            }
        }
        segmentedGroup("diagnostics", diagnostics) {
            row("record") { segment, modifier ->
                SwitchItem(
                    title = stringResource(R.string.about_diagnostics),
                    summary = stringResource(R.string.about_diagnostics_summary),
                    icon = R.drawable.ic_bug_report,
                    checked = settings.diagnosticsEnabled,
                    onCheckedChange = actions.onDiagnostics,
                    segment = segment,
                    modifier = modifier.testTag("setting_diagnostics"),
                )
            }
            if (settings.diagnosticsEnabled) {
                row("view") { segment, modifier ->
                    NavigationItem(
                        title = stringResource(R.string.about_view_log),
                        icon = R.drawable.ic_code,
                        onClick = actions.onOpenDiagnostics,
                        segment = segment,
                        modifier = modifier.testTag("open_diagnostics"),
                    )
                }
            }
        }
        segmentedGroup("info", info) {
            row("credits") { segment, modifier ->
                NavigationItem(
                    title = stringResource(R.string.about_credits),
                    summary = stringResource(R.string.about_credits_body),
                    icon = R.drawable.ic_favorite,
                    onClick = actions.onOpenSource,
                    trailing = null,
                    segment = segment,
                    modifier = modifier,
                )
            }
            row("license") { segment, modifier ->
                NavigationItem(
                    title = stringResource(R.string.about_license),
                    summary = stringResource(R.string.about_license_body),
                    icon = R.drawable.ic_gavel,
                    onClick = actions.onOpenSource,
                    trailing = null,
                    segment = segment,
                    modifier = modifier,
                )
            }
            row("source") { segment, modifier ->
                NavigationItem(
                    title = stringResource(R.string.about_source),
                    summary = SOURCE_URL.removePrefix("https://"),
                    icon = R.drawable.ic_code,
                    onClick = actions.onOpenSource,
                    trailing = { Icon(painterResource(R.drawable.ic_open_in_new), null) },
                    segment = segment,
                    modifier = modifier,
                )
            }
        }
        item(key = "disclaimer") {
            CardSurface(modifier = Modifier.padding(top = 16.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                Text(stringResource(R.string.about_disclaimer_title), style = MaterialTheme.typography.titleSmallEmphasized)
                Text(
                    stringResource(R.string.about_disclaimer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun AppHeader() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier
                .size(112.dp)
                .background(colorResource(R.color.ic_launcher_background), MaterialShapes.Cookie12Sided.toShape()),
        )
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMediumEmphasized)
        Text(
            stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ThemeSelector(mode: ThemeMode, onSelect: (ThemeMode) -> Unit, modifier: Modifier = Modifier) {
    val options = listOf(
        Triple(ThemeMode.SYSTEM, R.string.theme_system, R.drawable.ic_brightness_auto),
        Triple(ThemeMode.LIGHT, R.string.theme_light, R.drawable.ic_light_mode),
        Triple(ThemeMode.DARK, R.string.theme_dark, R.drawable.ic_dark_mode),
    )
    val haptics = rememberHaptics()
    Row(
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        modifier = modifier.fillMaxWidth().padding(bottom = 8.dp).selectableGroup(),
    ) {
        options.forEachIndexed { index, (value, label, icon) ->
            ToggleButton(
                checked = mode == value,
                onCheckedChange = {
                    if (mode != value) haptics.select()
                    onSelect(value)
                },
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
                modifier = Modifier.weight(1f).semantics { role = Role.RadioButton }.testTag("theme_${value.name}"),
            ) {
                Icon(painterResource(icon), null, Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text(stringResource(label), maxLines = 1)
            }
        }
    }
}
