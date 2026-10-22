package org.librecans.app.ui.devices

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.librecans.app.R
import org.librecans.app.data.bluetooth.DeviceKind
import org.librecans.app.data.bluetooth.PairedDevice
import org.librecans.app.data.connection.ConnectionState
import org.librecans.app.ui.components.Segment
import org.librecans.app.ui.components.shapes

@Composable
fun DeviceItem(
    device: PairedDevice,
    connection: ConnectionState,
    segment: Segment,
    onClick: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val connecting = connection as? ConnectionState.Connecting
    val isConnecting = connecting != null && connection.isFor(device.address)
    val isConnected = connection is ConnectionState.Connected && connection.isFor(device.address)
    val status = when {
        isConnecting && connecting.attempt > 1 -> stringResource(R.string.status_connecting_attempt, connecting.attempt)
        isConnecting -> stringResource(R.string.status_connecting)
        isConnected -> stringResource(R.string.status_connected)
        device.audioConnected -> stringResource(R.string.status_audio_connected)
        else -> stringResource(R.string.status_paired)
    }
    ConnectableItem(
        title = device.name,
        status = status,
        kind = device.kind,
        connecting = isConnecting,
        connected = isConnected,
        segment = segment,
        onClick = onClick,
        onCancel = onCancel,
        modifier = modifier.testTag("device_${device.address}"),
    )
}

@Composable
fun DemoItem(connection: ConnectionState, onClick: () -> Unit, onCancel: () -> Unit, modifier: Modifier = Modifier) {
    val isConnecting = connection is ConnectionState.Connecting && connection.isFor(demoAddress)
    val isConnected = connection is ConnectionState.Connected && connection.isFor(demoAddress)
    ConnectableItem(
        title = stringResource(R.string.devices_demo_title),
        status = when {
            isConnecting -> stringResource(R.string.status_connecting)
            isConnected -> stringResource(R.string.status_connected)
            else -> stringResource(R.string.devices_demo_body)
        },
        kind = DeviceKind.EARBUDS,
        connecting = isConnecting,
        connected = isConnected,
        segment = Segment.Single,
        onClick = onClick,
        onCancel = onCancel,
        modifier = modifier.testTag("demo_device"),
    )
}

@Composable
private fun ConnectableItem(
    title: String,
    status: String,
    kind: DeviceKind,
    connecting: Boolean,
    connected: Boolean,
    segment: Segment,
    onClick: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SegmentedListItem(
        onClick = onClick,
        shapes = segment.shapes(),
        modifier = modifier,
        leadingContent = { DeviceAvatar(kind, highlighted = connected || connecting) },
        supportingContent = { Text(status) },
        trailingContent = {
            when {
                connecting -> Row(verticalAlignment = Alignment.CenterVertically) {
                    LoadingIndicator(Modifier.size(36.dp).testTag("connecting_indicator"))
                    IconButton(onClick = onCancel) {
                        Icon(painterResource(R.drawable.ic_close), stringResource(R.string.action_cancel))
                    }
                }
                connected -> Icon(
                    painterResource(R.drawable.ic_check_circle_filled),
                    stringResource(R.string.status_connected),
                    tint = MaterialTheme.colorScheme.primary,
                )
                else -> Icon(painterResource(R.drawable.ic_chevron_right), null)
            }
        },
    ) { Text(title) }
}

@Composable
fun DeviceAvatar(kind: DeviceKind, highlighted: Boolean, modifier: Modifier = Modifier) {
    val container by animateColorAsState(
        if (highlighted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
        MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "avatar",
    )
    val shape = if (highlighted) MaterialShapes.Cookie9Sided else MaterialShapes.Cookie4Sided
    Box(
        modifier = modifier
            .size(56.dp)
            .background(container, shape.toShape()),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(kind.artwork),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.padding(8.dp),
        )
    }
}

val DeviceKind.artwork: Int
    get() = when (this) {
        DeviceKind.EARBUDS -> R.drawable.ic_device_earbuds
        DeviceKind.HEADPHONES -> R.drawable.ic_device_headphones
    }
