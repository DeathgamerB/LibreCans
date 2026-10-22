package org.librecans.app.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.RoundedPolygon
import org.librecans.app.R
import org.librecans.app.data.bluetooth.DeviceKind
import org.librecans.app.ui.components.BatteryRing
import org.librecans.app.ui.components.CardSurface
import org.librecans.app.ui.components.MorphingShape
import org.librecans.app.ui.devices.artwork
import org.librecans.protocol.model.Batteries
import org.librecans.protocol.model.NoiseMode

/**
 * Device artwork on a shape that morphs with the noise mode, above the battery rings. The artwork
 * shrinks towards the rings to half its size as [collapse] goes from 0 to 1.
 */
@Composable
fun HeroCard(
    kind: DeviceKind,
    batteries: Batteries,
    noiseMode: NoiseMode?,
    modifier: Modifier = Modifier,
    collapse: () -> Float = { 0f },
) {
    CardSurface(modifier = modifier.testTag("hero_card")) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(232.dp)
                .graphicsLayer {
                    val scale = 1f - 0.5f * collapse()
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = TransformOrigin(0.5f, 1f)
                },
            contentAlignment = Alignment.Center,
        ) {
            MorphingShape(
                shape = noiseMode.heroShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(216.dp),
            )
            Image(
                painter = painterResource(kind.artwork),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(176.dp),
            )
        }
        AnimatedVisibility(visible = batteries.isEmpty) {
            Text(
                stringResource(R.string.dashboard_battery_unknown),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            batteries.single?.let { BatteryRing(stringResource(R.string.dashboard_battery), it) }
            if (batteries.single == null && !batteries.isEmpty) {
                BatteryRing(stringResource(R.string.dashboard_battery_left), batteries.left)
                BatteryRing(stringResource(R.string.dashboard_battery_right), batteries.right)
            }
            batteries.case?.let { BatteryRing(stringResource(R.string.dashboard_battery_case), it) }
        }
    }
}

private val NoiseMode?.heroShape: RoundedPolygon
    get() = when (this) {
        NoiseMode.NOISE_CANCELLING -> MaterialShapes.Cookie12Sided
        NoiseMode.AMBIENT -> MaterialShapes.SoftBurst
        NoiseMode.OFF -> MaterialShapes.Circle
        null -> MaterialShapes.Cookie9Sided
    }
