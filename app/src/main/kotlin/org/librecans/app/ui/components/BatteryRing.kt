package org.librecans.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import org.librecans.app.R
import org.librecans.protocol.model.BatteryLevel

/** Battery level as a ring that turns wavy while charging. */
@Composable
fun BatteryRing(label: String, level: BatteryLevel?, modifier: Modifier = Modifier) {
    val percent = level?.percent ?: 0
    val progress by animateFloatAsState(
        targetValue = percent / 100f,
        animationSpec = MaterialTheme.motionScheme.slowSpatialSpec(),
        label = "battery",
    )
    val charging = level?.isCharging == true
    val low = level != null && percent <= 15 && !charging
    val color = when {
        level == null -> MaterialTheme.colorScheme.outlineVariant
        low -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }
    val description = if (level == null) {
        stringResource(R.string.dashboard_battery_unknown)
    } else {
        stringResource(R.string.dashboard_battery_content_description, label, percent) +
            if (charging) ", " + stringResource(R.string.dashboard_charging) else ""
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(76.dp)) {
            CircularWavyProgressIndicator(
                progress = { progress },
                color = color,
                amplitude = remember(charging) { { if (charging) 1f else 0f } },
                modifier = Modifier.size(76.dp),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (charging) {
                    Icon(painterResource(R.drawable.ic_bolt), null, Modifier.size(16.dp), tint = color)
                }
                Text(
                    text = if (level == null) "–" else "$percent%",
                    style = MaterialTheme.typography.titleMediumEmphasized,
                )
            }
        }
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
