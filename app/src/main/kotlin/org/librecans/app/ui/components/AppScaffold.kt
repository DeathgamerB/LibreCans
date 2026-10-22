package org.librecans.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.librecans.app.R
import org.librecans.app.ui.common.rememberHaptics

private val HeaderHeight = 56.dp

/**
 * Shared screen frame: a collapsing large flexible top app bar over a list of sections. Without a
 * title the list runs edge to edge under the status bar, optionally below a pinned [header].
 */
@Composable
fun AppScaffold(
    title: String?,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    snackbarHostState: SnackbarHostState? = null,
    listState: LazyListState = rememberLazyListState(),
    header: (@Composable () -> Unit)? = null,
    content: LazyListScope.() -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val background = MaterialTheme.colorScheme.surfaceContainer
    Scaffold(
        // Without a bar the collapse limit is never measured and the behavior would eat every scroll.
        modifier = if (title != null) modifier.nestedScroll(scrollBehavior.nestedScrollConnection) else modifier,
        containerColor = background,
        snackbarHost = { snackbarHostState?.let { SnackbarHost(it) } },
        topBar = {
            if (title != null) {
                LargeFlexibleTopAppBar(
                    title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    subtitle = subtitle?.let { { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) } },
                    navigationIcon = {
                        if (onBack != null) {
                            val haptics = rememberHaptics()
                            IconButton(
                                onClick = {
                                    haptics.tap()
                                    onBack()
                                },
                                modifier = Modifier.testTag("back"),
                            ) {
                                Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.action_back))
                            }
                        }
                    },
                    actions = actions,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = background,
                        scrolledContainerColor = background,
                    ),
                    scrollBehavior = scrollBehavior,
                )
            }
        },
    ) { padding ->
        val direction = LocalLayoutDirection.current
        Box {
            val start = padding.calculateStartPadding(direction) + 16.dp
            val end = padding.calculateEndPadding(direction) + 16.dp
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().testTag("screen_list"),
                contentPadding = PaddingValues(
                    start = start,
                    end = end,
                    top = padding.calculateTopPadding() + 8.dp + if (header != null) HeaderHeight else 0.dp,
                    bottom = padding.calculateBottomPadding() + 32.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
                content = content,
            )
            if (header != null) {
                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier = Modifier
                        .padding(start = start, end = end, top = padding.calculateTopPadding())
                        .fillMaxWidth()
                        .height(HeaderHeight),
                ) { header() }
            }
        }
    }
}
