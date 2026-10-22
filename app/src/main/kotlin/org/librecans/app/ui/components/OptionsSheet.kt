package org.librecans.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/** A bottom sheet with a single-choice list, closing itself after a pick. */
@Composable
fun <T> OptionsSheet(
    title: String,
    options: List<T>,
    selected: T?,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
    summary: (@Composable (T) -> String?)? = null,
    leading: (@Composable (T) -> Unit)? = null,
) {
    val state = rememberBottomSheetState(initialValue = SheetValue.Hidden, enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded))
    val scope = rememberCoroutineScope()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state, modifier = Modifier.testTag("options_sheet")) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
                .navigationBarsPadding()
                .selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLargeEmphasized,
                modifier = Modifier.padding(start = 8.dp, bottom = 16.dp),
            )
            options.forEachIndexed { index, option ->
                val isSelected = option == selected
                SegmentedListItem(
                    selected = isSelected,
                    onClick = {
                        onSelect(option)
                        scope.launch { state.hide() }.invokeOnCompletion { onDismiss() }
                    },
                    shapes = ListItemDefaults.segmentedShapes(index, options.size),
                    leadingContent = leading?.let { { it(option) } },
                    supportingContent = summary?.let { s -> s(option)?.let { { Text(it) } } },
                    trailingContent = { RadioButton(selected = isSelected, onClick = null) },
                ) { Text(label(option)) }
            }
        }
    }
}
