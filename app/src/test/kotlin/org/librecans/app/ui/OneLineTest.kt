package org.librecans.app.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.librecans.app.R
import org.librecans.app.data.bluetooth.BluetoothSnapshot
import org.librecans.app.data.bluetooth.DeviceKind
import org.librecans.app.data.bluetooth.PairedDevice
import org.librecans.app.data.connection.ConnectionState
import org.librecans.app.data.settings.AppSettings
import org.librecans.app.fakes.SimulatedState
import org.librecans.app.ui.about.AboutActions
import org.librecans.app.ui.about.AboutScreen
import org.librecans.app.ui.dashboard.DashboardActions
import org.librecans.app.ui.dashboard.DashboardScreen
import org.librecans.app.ui.dashboard.DashboardUiState
import org.librecans.app.ui.devices.DevicesActions
import org.librecans.app.ui.devices.DevicesScreen
import org.librecans.app.ui.devices.DevicesUiState
import org.librecans.app.ui.equalizer.EqualizerScreen
import org.librecans.app.ui.equalizer.EqualizerUiState
import org.librecans.app.ui.info.DeviceInfoScreen
import org.librecans.app.ui.info.DeviceInfoUiState
import org.librecans.app.ui.settings.DeviceSettingsActions
import org.librecans.app.ui.settings.DeviceSettingsScreen
import org.librecans.app.ui.settings.DeviceSettingsUiState
import org.librecans.app.ui.theme.LibreCansTheme
import org.librecans.app.ui.touch.TouchControlsScreen
import org.librecans.app.ui.touch.TouchModel
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Lays every screen out at the Pixel 8 Pro's width with real text measurement and checks that row
 * titles and summaries fit on one line, so list icons stay centred and nothing gets cut off.
 * The screen is tall enough for every list item to be composed at once.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w427dp-h3200dp-xxhdpi")
class OneLineTest {
    @get:Rule
    val compose = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val state = SimulatedState.xm5
    private val buds = SimulatedState.buds

    /** Paragraphs that are meant to wrap. */
    private val paragraphs = listOf(R.string.about_credits_body, R.string.about_disclaimer, R.string.eq_custom_hint)
        .map(context::getString)

    private fun show(content: @Composable () -> Unit) = compose.setContent { LibreCansTheme(dynamicColor = false, content = content) }

    private fun assertNothingWraps() {
        compose.waitForIdle()
        val wrapped = compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult), useUnmergedTree = true)
            .fetchSemanticsNodes()
            .mapNotNull { node ->
                val layouts = mutableListOf<TextLayoutResult>()
                node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
                layouts.firstOrNull()?.takeIf { it.lineCount > 1 }?.layoutInput?.text?.text
            }
            .filterNot { it in paragraphs }
        assertWithMessage("Texts that wrap at 427dp").that(wrapped).isEmpty()
    }

    @Test
    fun dashboard() {
        show { DashboardScreen(DashboardUiState(ConnectionState.Connected(buds), state), DashboardActions()) }
        assertNothingWraps()
    }

    @Test
    fun deviceSettings() {
        show { DeviceSettingsScreen(DeviceSettingsUiState(buds, state), onBack = {}, actions = DeviceSettingsActions()) }
        assertNothingWraps()
    }

    @Test
    fun touchControls() {
        show { TouchControlsScreen(TouchModel.uiState(state.touchControls), {}, { _, _ -> }, {}) }
        assertNothingWraps()
    }

    @Test
    fun equalizer() {
        show {
            EqualizerScreen(
                EqualizerUiState(state.equalizer),
                onBack = {},
                onSelectPreset = {},
                onBandChange = { _, _ -> },
                onClearBassChange = {},
                onReset = {},
            )
        }
        assertNothingWraps()
    }

    @Test
    fun deviceInfo() {
        show { DeviceInfoScreen(DeviceInfoUiState(buds, state), onBack = {}) }
        assertNothingWraps()
    }

    @Test
    fun about() {
        show { AboutScreen(AppSettings(), onBack = {}, actions = AboutActions()) }
        assertNothingWraps()
    }

    @Test
    fun devices() {
        val paired = PairedDevice(buds.address, buds.name, DeviceKind.EARBUDS, audioConnected = true)
        show {
            DevicesScreen(
                DevicesUiState(bluetooth = BluetoothSnapshot(true, true, true, listOf(paired)), connection = ConnectionState.Idle, showDemo = true),
                DevicesActions(),
            )
        }
        assertNothingWraps()
    }
}
