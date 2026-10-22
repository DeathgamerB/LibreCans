package org.librecans.app.ui.touch

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import org.librecans.app.R
import org.librecans.app.ui.common.ambientCycleLabel
import org.librecans.app.ui.common.rememberHaptics
import org.librecans.app.ui.components.Segment
import org.librecans.app.ui.components.ShapedIcon
import org.librecans.app.ui.components.shapes

/** Which noise modes a tap on the noise control earbud steps through, picked from a menu. */
@Composable
fun AmbientCycleMenu(
    selected: Int?,
    options: List<Int>,
    onSelect: (Int) -> Unit,
    segment: Segment,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val haptics = rememberHaptics()
    val arrow by animateFloatAsState(
        targetValue = if (expanded) 270f else 90f,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "arrow",
    )
    Box(modifier) {
        SegmentedListItem(
            onClick = {
                haptics.tap()
                expanded = true
            },
            shapes = segment.shapes(),
            leadingContent = { ShapedIcon(R.drawable.ic_noise_control_on) },
            supportingContent = { selected?.let { Text(stringResource(ambientCycleLabel(it))) } },
            trailingContent = { Icon(painterResource(R.drawable.ic_chevron_right), null, Modifier.rotate(arrow)) },
            modifier = Modifier.testTag("cycle_menu"),
        ) { Text(stringResource(R.string.touch_cycle_title)) }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = MaterialTheme.shapes.large,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            options.forEach { function ->
                DropdownMenuItem(
                    text = { Text(stringResource(ambientCycleLabel(function))) },
                    onClick = {
                        if (function != selected) haptics.select()
                        expanded = false
                        onSelect(function)
                    },
                    trailingIcon = if (function == selected) {
                        { Icon(painterResource(R.drawable.ic_check), null) }
                    } else {
                        null
                    },
                    modifier = Modifier.semantics { role = Role.RadioButton }.testTag("cycle_$function"),
                )
            }
        }
    }
}
