package org.librecans.app.ui.components

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

typealias GroupRow = @Composable (segment: Segment, modifier: Modifier) -> Unit

/** Collects optional rows so the segmented shapes are computed from the rows actually shown. */
class GroupBuilder {
    internal val rows = mutableListOf<Pair<String, GroupRow>>()

    fun row(key: String, content: GroupRow) {
        rows += key to content
    }
}

fun LazyListScope.segmentedGroup(key: String, title: String?, build: GroupBuilder.() -> Unit) {
    val rows = GroupBuilder().apply(build).rows
    if (rows.isEmpty()) return
    section(key, title) {
        rows.forEachIndexed { index, (rowKey, content) ->
            item(key = "${key}_$rowKey") { content(Segment(index, rows.size), Modifier.animateItem()) }
        }
    }
}
