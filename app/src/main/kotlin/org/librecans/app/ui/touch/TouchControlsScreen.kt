package org.librecans.app.ui.touch

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.librecans.app.R
import org.librecans.app.ui.common.appViewModel
import org.librecans.app.ui.common.touchPresetDescription
import org.librecans.app.ui.common.touchPresetIcon
import org.librecans.app.ui.common.touchPresetLabel
import org.librecans.app.ui.components.AppScaffold
import org.librecans.app.ui.components.NavigationItem
import org.librecans.app.ui.components.OptionsSheet
import org.librecans.app.ui.components.ShapedIcon
import org.librecans.app.ui.components.segmentedGroup
import org.librecans.protocol.model.TouchKeys

@Composable
fun TouchControlsEntry(onBack: () -> Unit) {
    val viewModel = appViewModel { TouchControlsViewModel(it.headphones) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    TouchControlsScreen(state, onBack, viewModel::setPreset, viewModel::setAmbientCycle)
}

/** One row per earbud showing its function; tapping a row picks another one. */
@Composable
fun TouchControlsScreen(
    state: TouchControlsUiState,
    onBack: () -> Unit,
    onSetPreset: (key: Int, preset: Int) -> Unit,
    onSetAmbientCycle: (Int) -> Unit,
) {
    var editingKey by rememberSaveable { mutableStateOf<Int?>(null) }
    val earbudsTitle = stringResource(R.string.touch_section_earbuds)
    val sides = state.sides.map { it to sideTitle(it.key) }

    AppScaffold(title = stringResource(R.string.touch_title), onBack = onBack) {
        if (!state.available) {
            item(key = "unavailable") {
                Text(
                    stringResource(R.string.touch_unavailable),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(8.dp),
                )
            }
        }
        segmentedGroup("earbuds", earbudsTitle.takeIf { state.available }) {
            sides.forEach { (side, title) ->
                row("side_${side.key}") { segment, modifier ->
                    NavigationItem(
                        title = title,
                        summary = touchPresetLabel(side.preset),
                        leading = { ShapedIcon(touchPresetIcon(side.preset)) },
                        onClick = { editingKey = side.key },
                        segment = segment,
                        modifier = modifier.testTag("earbud_${side.key}"),
                    )
                }
            }
        }
        if (state.showCycle) {
            segmentedGroup("cycle", null) {
                row("cycle") { segment, modifier ->
                    AmbientCycleMenu(state.ambientCycle, state.cycleOptions, onSetAmbientCycle, segment, modifier.padding(top = 12.dp))
                }
            }
        }
    }

    val side = state.sides.firstOrNull { it.key == editingKey }
    if (side != null) {
        OptionsSheet(
            title = sides.first { it.first.key == side.key }.second,
            options = side.options.map { it.preset },
            selected = side.preset,
            label = { touchPresetLabel(it) },
            summary = { touchPresetDescription(it) },
            leading = {
                ShapedIcon(
                    touchPresetIcon(it),
                    size = 36.dp,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            },
            onSelect = { onSetPreset(side.key, it) },
            onDismiss = { editingKey = null },
        )
    }
}

@Composable
private fun sideTitle(key: Int): String = when (key) {
    TouchKeys.LEFT -> stringResource(R.string.touch_left)
    TouchKeys.RIGHT -> stringResource(R.string.touch_right)
    else -> stringResource(R.string.touch_key, key + 1)
}
