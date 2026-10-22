package org.librecans.app.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Device name and status pinned above the dashboard. As the list scrolls ([progress] 0 to 1) the
 * two halves fade out while drifting apart and shrinking, leaving a clean edge-to-edge page.
 */
@Composable
fun DashboardHeader(name: String, status: String, progress: () -> Float, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp).testTag("dashboard_header"),
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f, fill = false)
                .graphicsLayer { fadeAway(progress(), toEnd = false) },
        )
        Text(
            text = status,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier
                .padding(start = 16.dp)
                .graphicsLayer { fadeAway(progress(), toEnd = true) },
        )
    }
}

private fun GraphicsLayerScope.fadeAway(progress: Float, toEnd: Boolean) {
    val drift = 48.dp.toPx() * progress
    translationX = if (toEnd) drift else -drift
    val scale = 1f - 0.2f * progress
    scaleX = scale
    scaleY = scale
    alpha = 1f - progress
    transformOrigin = TransformOrigin(if (toEnd) 1f else 0f, 0.5f)
}
