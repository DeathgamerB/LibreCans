package org.librecans.app.ui.touch

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedListItem
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
import org.librecans.app.ui.common.ambientCycleLabel
import org.librecans.app.ui.common.appViewModel
import org.librecans.app.ui.common.touchActionLabel
import org.librecans.app.ui.common.touchFunctionLabel
import org.librecans.app.ui.common.touchPresetIcon
import org.librecans.app.ui.common.touchPresetLabel
import org.librecans.app.ui.components.AppScaffold
import org.librecans.app.ui.components.OptionsSheet
import org.librecans.app.ui.components.SectionHeader
import org.librecans.app.ui.components.ShapedIcon

@Composable
fun TouchControlsEntry(onBack: () -> Unit) {
    val viewModel = appViewModel { TouchControlsViewModel(it.headphones) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    TouchControlsScreen(state, onBack, viewModel::setPreset, viewModel::setAmbientCycle)
}

@Composable
fun TouchControlsScreen(
    state: TouchControlsUiState,
    onBack: () -> Unit,
    onSetPreset: (key: Int, preset: Int) -> Unit,
    onSetAmbientCycle: (Int) -> Unit,
) {
    var editingKey by rememberSaveable { mutableStateOf<Int?>(null) }
    val cycleTitle = stringResource(R.string.touch_cycle_title)

    AppScaffold(title = stringResource(R.string.touch_title), onBack = onBack) {
        item(key = "intro") {
            Text(
                stringResource(if (state.available) R.string.touch_intro else R.string.touch_unavailable),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            )
        }
        state.sides.forEach { side ->
            item(key = "side_${side.key}") {
                EarbudCard(side, onChange = { editingKey = side.key }, modifier = Modifier.padding(vertical = 6.dp).animateItem())
            }
        }
        if (state.showCycle) {
            item(key = "cycle_header") { SectionHeader(cycleTitle, Modifier.animateItem()) }
            item(key = "cycle_summary") {
                Text(
                    stringResource(R.string.touch_cycle_summary),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp).animateItem(),
                )
            }
            state.cycleOptions.forEachIndexed { index, function ->
                item(key = "cycle_$function") {
                    SegmentedListItem(
                        selected = state.ambientCycle == function,
                        onClick = { onSetAmbientCycle(function) },
                        shapes = ListItemDefaults.segmentedShapes(index, state.cycleOptions.size),
                        trailingContent = { RadioButton(selected = state.ambientCycle == function, onClick = null) },
                        modifier = Modifier.animateItem().testTag("cycle_$function"),
                    ) { Text(stringResource(ambientCycleLabel(function))) }
                }
            }
        }
    }

    val side = state.sides.firstOrNull { it.key == editingKey }
    if (side != null) {
        OptionsSheet(
            title = stringResource(R.string.touch_choose),
            options = side.options.map { it.preset },
            selected = side.preset,
            label = { touchPresetLabel(it) },
            summary = { preset ->
                val gestures = side.options.firstOrNull { it.preset == preset }?.gestures.orEmpty()
                if (gestures.isEmpty()) {
                    stringResource(R.string.touch_no_gestures)
                } else {
                    gestures.map { stringResource(R.string.touch_gesture_format, stringResource(touchActionLabel(it.action)), touchFunctionLabel(it.function)) }
                        .joinToString("\n")
                }
            },
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
