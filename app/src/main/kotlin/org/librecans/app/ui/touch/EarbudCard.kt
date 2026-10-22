package org.librecans.app.ui.touch

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.librecans.app.R
import org.librecans.app.ui.common.rememberHaptics
import org.librecans.app.ui.common.touchActionLabel
import org.librecans.app.ui.common.touchFunctionLabel
import org.librecans.app.ui.common.touchPresetIcon
import org.librecans.app.ui.common.touchPresetLabel
import org.librecans.app.ui.components.CardSurface
import org.librecans.app.ui.components.ShapedIcon
import org.librecans.protocol.model.TouchKeys

/** One earbud: its assigned function and what each gesture does. */
@Composable
fun EarbudCard(side: TouchSide, onChange: () -> Unit, modifier: Modifier = Modifier) {
    val title = when (side.key) {
        TouchKeys.LEFT -> stringResource(R.string.touch_left)
        TouchKeys.RIGHT -> stringResource(R.string.touch_right)
        else -> stringResource(R.string.touch_key, side.key + 1)
    }
    val motion = MaterialTheme.motionScheme
    CardSurface(modifier = modifier.testTag("earbud_${side.key}")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AnimatedContent(
                targetState = side.preset,
                transitionSpec = { fadeIn(motion.defaultEffectsSpec()) togetherWith fadeOut(motion.fastEffectsSpec()) },
                label = "presetIcon",
            ) { preset ->
                ShapedIcon(
                    touchPresetIcon(preset),
                    size = 52.dp,
                    shape = if (side.key == TouchKeys.LEFT) MaterialShapes.Cookie7Sided else MaterialShapes.Clover4Leaf,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(touchPresetLabel(side.preset), style = MaterialTheme.typography.titleMediumEmphasized)
            }
            val haptics = rememberHaptics()
            FilledTonalButton(
                onClick = {
                    haptics.tap()
                    onChange()
                },
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.testTag("change_${side.key}"),
            ) {
                Text(stringResource(R.string.action_change))
            }
        }
        GestureList(side, Modifier.padding(top = 16.dp))
    }
}

@Composable
private fun GestureList(side: TouchSide, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (side.gestures.isEmpty()) {
            Text(stringResource(R.string.touch_no_gestures), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        side.gestures.forEach { gesture ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ) {
                    Text(
                        stringResource(touchActionLabel(gesture.action)),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(touchFunctionLabel(gesture.function), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
