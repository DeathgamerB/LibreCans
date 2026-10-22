package org.librecans.app.ui.dashboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import org.librecans.app.R
import org.librecans.app.ui.common.icon
import org.librecans.app.ui.common.label
import org.librecans.app.ui.common.rememberHaptics
import org.librecans.app.ui.common.shortLabel
import org.librecans.app.ui.components.CardSurface
import org.librecans.app.ui.components.IntSlider
import org.librecans.app.ui.components.ShapedIcon
import org.librecans.protocol.mdr.Inquired
import org.librecans.protocol.model.NoiseControl
import org.librecans.protocol.model.NoiseMode

@Composable
fun NoiseControlCard(
    control: NoiseControl,
    onModeChange: (NoiseMode) -> Unit,
    onLevelChange: (level: Int, settled: Boolean) -> Unit,
    onFocusOnVoiceChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val modes = if (control.inquiredType == Inquired.NCASM_ASM_SEAMLESS) {
        listOf(NoiseMode.AMBIENT, NoiseMode.OFF)
    } else {
        NoiseMode.entries
    }
    val motion = MaterialTheme.motionScheme
    CardSurface(modifier = modifier.testTag("noise_card")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AnimatedContent(
                targetState = control.mode,
                transitionSpec = { fadeIn(motion.defaultEffectsSpec()) togetherWith fadeOut(motion.fastEffectsSpec()) },
                label = "modeIcon",
            ) { ShapedIcon(it.icon, size = 44.dp) }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.dashboard_noise_title), style = MaterialTheme.typography.titleMediumEmphasized)
                Text(
                    stringResource(control.mode.label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        val haptics = rememberHaptics()
        Row(
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp).selectableGroup(),
        ) {
            modes.forEachIndexed { index, mode ->
                val selected = control.mode == mode
                ToggleButton(
                    checked = selected,
                    onCheckedChange = {
                        if (!selected) haptics.select()
                        onModeChange(mode)
                    },
                    shapes = when (index) {
                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        modes.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("mode_${mode.name}")
                        .semantics {
                            role = Role.RadioButton
                            stateDescription = if (selected) "selected" else "not selected"
                        },
                ) {
                    Icon(painterResource(mode.icon), contentDescription = null, Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text(stringResource(mode.shortLabel), maxLines = 1)
                }
            }
        }

        AnimatedVisibility(
            visible = control.mode == NoiseMode.AMBIENT,
            enter = expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) + fadeIn(),
            exit = shrinkVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) + fadeOut(),
        ) {
            AmbientSettings(control, onLevelChange, onFocusOnVoiceChange)
        }
    }
}

@Composable
private fun AmbientSettings(
    control: NoiseControl,
    onLevelChange: (level: Int, settled: Boolean) -> Unit,
    onFocusOnVoiceChange: (Boolean) -> Unit,
) {
    val haptics = rememberHaptics()
    Column(Modifier.padding(top = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.dashboard_ambient_level), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(
                control.ambientLevel.toString(),
                style = MaterialTheme.typography.titleMediumEmphasized,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        IntSlider(
            value = control.ambientLevel.coerceAtLeast(NoiseControl.MIN_AMBIENT_LEVEL),
            range = NoiseControl.MIN_AMBIENT_LEVEL..NoiseControl.MAX_AMBIENT_LEVEL,
            onValueChange = { onLevelChange(it, false) },
            onValueChangeFinished = { onLevelChange(it, true) },
            label = stringResource(R.string.dashboard_ambient_level),
            modifier = Modifier.fillMaxWidth().testTag("ambient_slider"),
        )
        val focusAvailable = control.ambientLevel >= NoiseControl.MIN_FOCUS_ON_VOICE_LEVEL
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.dashboard_focus_on_voice), style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(R.string.dashboard_focus_on_voice_summary),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = control.focusOnVoice,
                onCheckedChange = {
                    haptics.toggle(it)
                    onFocusOnVoiceChange(it)
                },
                enabled = focusAvailable,
                modifier = Modifier.testTag("focus_on_voice"),
            )
        }
    }
}
