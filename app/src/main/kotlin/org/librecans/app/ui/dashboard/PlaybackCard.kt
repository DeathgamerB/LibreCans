package org.librecans.app.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.librecans.app.R
import org.librecans.app.ui.common.rememberHaptics
import org.librecans.app.ui.components.CardSurface
import org.librecans.app.ui.components.IntSlider
import org.librecans.protocol.model.Playback
import org.librecans.protocol.model.PlaybackStatus

@Composable
fun PlaybackCard(
    playback: Playback,
    onTogglePlayback: () -> Unit,
    onSkip: (next: Boolean) -> Unit,
    onVolumeChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val playing = playback.status == PlaybackStatus.PLAYING
    val haptics = rememberHaptics()
    CardSurface(modifier = modifier.testTag("playback_card")) {
        Text(stringResource(R.string.dashboard_playback_title), style = MaterialTheme.typography.titleMediumEmphasized)
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledTonalIconButton(
                onClick = {
                    haptics.key()
                    onSkip(false)
                },
                shapes = IconButtonDefaults.shapes(),
                modifier = Modifier.size(IconButtonDefaults.mediumContainerSize()),
            ) {
                Icon(painterResource(R.drawable.ic_skip_previous_filled), stringResource(R.string.dashboard_previous))
            }
            FilledIconButton(
                onClick = {
                    haptics.toggle(!playing)
                    onTogglePlayback()
                },
                shapes = IconButtonDefaults.shapes(),
                modifier = Modifier
                    .size(IconButtonDefaults.mediumContainerSize(IconButtonDefaults.IconButtonWidthOption.Wide))
                    .testTag("play_pause"),
            ) {
                Icon(
                    painterResource(if (playing) R.drawable.ic_pause_filled else R.drawable.ic_play_arrow_filled),
                    stringResource(if (playing) R.string.dashboard_pause else R.string.dashboard_play),
                    Modifier.size(IconButtonDefaults.mediumIconSize),
                )
            }
            FilledTonalIconButton(
                onClick = {
                    haptics.key()
                    onSkip(true)
                },
                shapes = IconButtonDefaults.shapes(),
                modifier = Modifier.size(IconButtonDefaults.mediumContainerSize()),
            ) {
                Icon(painterResource(R.drawable.ic_skip_next_filled), stringResource(R.string.dashboard_next))
            }
        }
        playback.volume?.let { volume ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(R.drawable.ic_volume_down), null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                IntSlider(
                    value = volume,
                    range = 0..Playback.MAX_VOLUME,
                    onValueChange = onVolumeChange,
                    onValueChangeFinished = onVolumeChange,
                    label = stringResource(R.string.dashboard_volume),
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp).testTag("volume_slider"),
                )
                Icon(painterResource(R.drawable.ic_volume_up), null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
