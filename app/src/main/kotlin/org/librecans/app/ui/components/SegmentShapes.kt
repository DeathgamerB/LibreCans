package org.librecans.app.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * Segmented list shapes rounded like the cards around them: the group's outer corners match the
 * cards and inner corners stay soft, so a group reads as one block. Checked or selected rows keep
 * their place in that block; only a press briefly rounds a row off.
 */
@Composable
fun segmentShapes(index: Int, count: Int): ListItemShapes {
    val outer = MaterialTheme.shapes.extraLarge
    val inner = MaterialTheme.shapes.medium
    val first = index == 0
    val last = index == count - 1
    val shape = remember(first, last, outer, inner) {
        RoundedCornerShape(
            topStart = if (first) outer.topStart else inner.topStart,
            topEnd = if (first) outer.topEnd else inner.topEnd,
            bottomEnd = if (last) outer.bottomEnd else inner.bottomEnd,
            bottomStart = if (last) outer.bottomStart else inner.bottomStart,
        )
    }
    return ListItemDefaults.shapes(
        shape = shape,
        selectedShape = shape,
        pressedShape = outer,
        focusedShape = shape,
        hoveredShape = shape,
        draggedShape = shape,
    )
}

@Composable
fun Segment.shapes(): ListItemShapes = segmentShapes(index, count)
