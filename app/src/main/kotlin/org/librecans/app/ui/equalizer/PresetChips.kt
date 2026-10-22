package org.librecans.app.ui.equalizer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import org.librecans.app.R
import org.librecans.app.ui.common.eqPresetLabel
import org.librecans.protocol.model.EqPreset

@Composable
fun PresetChips(presets: List<EqPreset>, selectedId: Int?, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        presets.forEach { preset ->
            val selected = preset.id == selectedId
            FilterChip(
                selected = selected,
                onClick = { onSelect(preset.id) },
                label = { Text(eqPresetLabel(preset.id, preset.name)) },
                leadingIcon = {
                    AnimatedVisibility(
                        visible = selected,
                        enter = expandHorizontally(MaterialTheme.motionScheme.fastSpatialSpec()),
                        exit = shrinkHorizontally(MaterialTheme.motionScheme.fastSpatialSpec()),
                    ) {
                        Icon(painterResource(R.drawable.ic_check), null, Modifier.size(FilterChipDefaults.IconSize))
                    }
                },
                modifier = Modifier.testTag("preset_${preset.id}"),
            )
        }
    }
}
