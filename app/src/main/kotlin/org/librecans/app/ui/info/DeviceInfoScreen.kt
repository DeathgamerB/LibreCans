package org.librecans.app.ui.info

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.librecans.app.R
import org.librecans.app.data.bluetooth.DeviceKind
import org.librecans.app.data.bluetooth.SonyDevices
import org.librecans.app.ui.about.SOURCE_URL
import org.librecans.app.ui.about.appInfoSection
import org.librecans.app.ui.common.appViewModel
import org.librecans.app.ui.common.codecLabel
import org.librecans.app.ui.common.label
import org.librecans.app.ui.components.AppScaffold
import org.librecans.app.ui.components.InfoItem
import org.librecans.app.ui.components.artworkBackdrop
import org.librecans.app.ui.components.segmentedGroup
import org.librecans.app.ui.devices.artwork

@Composable
fun DeviceInfoEntry(onBack: () -> Unit) {
    val viewModel = appViewModel { DeviceInfoViewModel(it.headphones) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    DeviceInfoScreen(state, onBack, onOpenSource = { context.startActivity(Intent(Intent.ACTION_VIEW, SOURCE_URL.toUri())) })
}

/** Everything known about the connected headphones first, then who made the app. */
@Composable
fun DeviceInfoScreen(state: DeviceInfoUiState, onBack: () -> Unit, onOpenSource: () -> Unit = {}) {
    val h = state.headphones
    val name = h.identity.modelName ?: state.target?.name.orEmpty()
    val kind = state.target?.kind ?: SonyDevices.kindOf(name).takeIf { name.isNotEmpty() } ?: DeviceKind.EARBUDS
    val headphonesTitle = stringResource(R.string.settings_section_device)
    val appTitle = stringResource(R.string.info_section_app)

    AppScaffold(title = stringResource(R.string.about_title), onBack = onBack) {
        item(key = "device") { DeviceHeader(kind, name, Modifier.animateItem()) }
        segmentedGroup("headphones", headphonesTitle) {
            if (name.isNotEmpty()) {
                row("model") { segment, modifier -> InfoItem(stringResource(R.string.settings_model), name, segment, modifier, R.drawable.ic_headphones) }
            }
            h.identity.firmware?.let { firmware ->
                row("firmware") { segment, modifier ->
                    InfoItem(stringResource(R.string.settings_firmware), firmware, segment, modifier.testTag("info_firmware"), R.drawable.ic_code)
                }
            }
            // The WF-1000XM5 reports its Bluetooth address here; only show IDs that add something.
            h.identity.uniqueId?.takeUnless { it.sameAddressAs(state.target?.address) }?.let { id ->
                row("device_id") { segment, modifier ->
                    InfoItem(stringResource(R.string.settings_device_id), id, segment, modifier.testTag("info_device_id"), R.drawable.ic_devices)
                }
            }
            state.target?.takeIf { !it.isDemo }?.let { target ->
                row("address") { segment, modifier ->
                    InfoItem(stringResource(R.string.settings_address), target.address, segment, modifier, R.drawable.ic_bluetooth)
                }
            }
            h.codec?.let { codec ->
                row("codec") { segment, modifier -> InfoItem(stringResource(R.string.settings_codec), codecLabel(codec), segment, modifier, R.drawable.ic_graphic_eq) }
            }
            if (h.upscaling != null) {
                row("upscaling") { segment, modifier ->
                    InfoItem(stringResource(R.string.settings_upscaling_type), stringResource(h.upscalingType.label), segment, modifier, R.drawable.ic_auto_awesome)
                }
            }
            h.protocolVersion?.let { version ->
                row("protocol") { segment, modifier ->
                    InfoItem(
                        stringResource(R.string.settings_protocol),
                        stringResource(R.string.settings_protocol_value, "%08X".format(version)),
                        segment,
                        modifier,
                        R.drawable.ic_info,
                    )
                }
            }
        }
        appInfoSection(appTitle, onOpenSource)
    }
}

private fun String.sameAddressAs(address: String?): Boolean =
    address != null && filter(Char::isLetterOrDigit).equals(address.filter(Char::isLetterOrDigit), ignoreCase = true)

@Composable
private fun DeviceHeader(kind: DeviceKind, name: String, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
    ) {
        Image(
            painter = painterResource(kind.artwork),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(144.dp)
                .background(artworkBackdrop(), MaterialShapes.Cookie9Sided.toShape())
                .padding(20.dp),
        )
        Text(name, style = MaterialTheme.typography.headlineSmallEmphasized, modifier = Modifier.padding(top = 8.dp))
    }
}
