package org.librecans.app.ui.equalizer

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.librecans.app.R
import org.librecans.app.ui.common.appViewModel
import org.librecans.app.ui.components.AppScaffold
import org.librecans.app.ui.components.CardSurface
import org.librecans.app.ui.components.IntSlider
import org.librecans.app.ui.components.SectionHeader

@Composable
fun EqualizerEntry(onBack: () -> Unit) {
    val viewModel = appViewModel { EqualizerViewModel(it.headphones) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    EqualizerScreen(
        state = state,
        onBack = onBack,
        onSelectPreset = viewModel::selectPreset,
        onBandChange = viewModel::setBand,
        onClearBassChange = viewModel::setClearBass,
        onReset = viewModel::reset,
    )
}

@Composable
fun EqualizerScreen(
    state: EqualizerUiState,
    onBack: () -> Unit,
    onSelectPreset: (Int) -> Unit,
    onBandChange: (Int, Int) -> Unit,
    onClearBassChange: (Int) -> Unit,
    onReset: () -> Unit,
) {
    val available = state.equalizer?.available != false
    val levels = state.levels
    val presetsTitle = stringResource(R.string.eq_presets)
    val customTitle = stringResource(R.string.eq_custom_bands)

    AppScaffold(title = stringResource(R.string.eq_title), onBack = onBack) {
        if (!available) {
            item(key = "unavailable") {
                CardSurface(color = MaterialTheme.colorScheme.secondaryContainer) {
                    Text(stringResource(R.string.eq_unavailable), color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
        }
        item(key = "curve") {
            CardSurface(modifier = Modifier.padding(top = 8.dp)) {
                EqCurve(
                    levels = listOfNotNull(levels.clearBass) + levels.bands,
                    range = state.range,
                    modifier = Modifier.fillMaxWidth().height(120.dp).testTag("eq_curve"),
                )
            }
        }
        item(key = "presets_header") { SectionHeader(presetsTitle) }
        item(key = "presets") {
            CardSurface {
                PresetChips(
                    presets = state.presets,
                    selectedId = state.equalizer?.presetId,
                    onSelect = onSelectPreset,
                )
            }
        }
        item(key = "custom_header") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionHeader(customTitle, Modifier.weight(1f))
                TextButton(onClick = onReset, enabled = available, shapes = ButtonDefaults.shapes(), modifier = Modifier.testTag("eq_reset")) {
                    Icon(painterResource(R.drawable.ic_restart_alt), null)
                    Text(stringResource(R.string.action_reset), Modifier.padding(start = 8.dp))
                }
            }
        }
        item(key = "bands") {
            CardSurface {
                BandSliders(levels = levels.bands, range = state.range, onBandChange = onBandChange, enabled = available)
                levels.clearBass?.let { clearBass ->
                    Spacer(Modifier.height(24.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.eq_clear_bass), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        Text(
                            if (clearBass > 0) "+$clearBass" else "$clearBass",
                            style = MaterialTheme.typography.labelLargeEmphasized,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    IntSlider(
                        value = clearBass,
                        range = state.range,
                        onValueChange = onClearBassChange,
                        onValueChangeFinished = onClearBassChange,
                        enabled = available,
                        label = stringResource(R.string.eq_clear_bass),
                        modifier = Modifier.fillMaxWidth().testTag("clear_bass"),
                    )
                }
                Text(
                    stringResource(R.string.eq_custom_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
    }
}
