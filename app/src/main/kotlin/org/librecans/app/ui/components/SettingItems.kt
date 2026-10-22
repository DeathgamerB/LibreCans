package org.librecans.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import org.librecans.app.R

/** Position of an item inside a group of segmented list items. */
data class Segment(val index: Int, val count: Int) {
    companion object {
        val Single = Segment(0, 1)
    }
}

@Composable
fun SwitchItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    segment: Segment,
    modifier: Modifier = Modifier,
    summary: String? = null,
    @DrawableRes icon: Int? = null,
    enabled: Boolean = true,
) {
    SegmentedListItem(
        checked = checked,
        onCheckedChange = onCheckedChange,
        shapes = ListItemDefaults.segmentedShapes(segment.index, segment.count),
        modifier = modifier,
        enabled = enabled,
        colors = toggleColors(),
        leadingContent = icon?.let { { Icon(painterResource(it), contentDescription = null) } },
        supportingContent = summary?.let { { Text(it) } },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = null,
                enabled = enabled,
                thumbContent = if (checked) {
                    { Icon(painterResource(R.drawable.ic_check), null, Modifier.size(SwitchDefaults.IconSize)) }
                } else {
                    null
                },
            )
        },
    ) { Text(title) }
}

@Composable
fun NavigationItem(
    title: String,
    onClick: () -> Unit,
    segment: Segment,
    modifier: Modifier = Modifier,
    summary: String? = null,
    @DrawableRes icon: Int? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = { Icon(painterResource(R.drawable.ic_chevron_right), null) },
    enabled: Boolean = true,
) {
    SegmentedListItem(
        onClick = onClick,
        shapes = ListItemDefaults.segmentedShapes(segment.index, segment.count),
        modifier = modifier,
        enabled = enabled,
        leadingContent = leading ?: icon?.let { { Icon(painterResource(it), contentDescription = null) } },
        supportingContent = summary?.let { { Text(it) } },
        trailingContent = trailing,
    ) { Text(title) }
}

@Composable
fun InfoItem(title: String, value: String, segment: Segment, modifier: Modifier = Modifier, @DrawableRes icon: Int? = null) {
    SegmentedListItem(
        shapes = ListItemDefaults.segmentedShapes(segment.index, segment.count),
        modifier = modifier,
        leadingContent = icon?.let { { Icon(painterResource(it), contentDescription = null) } },
        supportingContent = { Text(value) },
    ) { Text(title) }
}

/** Switch rows show their state through the switch, not a highlighted container. */
@Composable
private fun toggleColors() = ListItemDefaults.segmentedColors(
    selectedContainerColor = MaterialTheme.colorScheme.surface,
    selectedContentColor = MaterialTheme.colorScheme.onSurface,
    selectedLeadingContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    selectedTrailingContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    selectedSupportingContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
)
