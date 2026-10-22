package org.librecans.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.librecans.app.data.bluetooth.BluetoothSnapshot
import org.librecans.app.data.bluetooth.DeviceKind
import org.librecans.app.data.bluetooth.PairedDevice
import org.librecans.app.data.connection.ConnectionState
import org.librecans.app.data.connection.DeviceTarget
import org.librecans.app.data.connection.FailureReason
import org.librecans.app.data.settings.AppSettings
import org.librecans.app.data.settings.ThemeMode
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
import org.librecans.app.ui.settings.DeviceSettingsActions
import org.librecans.app.ui.settings.DeviceSettingsScreen
import org.librecans.app.ui.settings.DeviceSettingsUiState
import org.librecans.app.ui.theme.LibreCansTheme
import org.librecans.app.ui.touch.TouchControlsScreen
import org.librecans.app.ui.touch.TouchModel
import org.librecans.protocol.model.BatteryLevel
import org.librecans.protocol.model.Batteries
import org.librecans.protocol.model.ChargingState
import org.librecans.protocol.model.Equalizer
import org.librecans.protocol.model.EqPresets
import org.librecans.protocol.model.GeneralSetting
import org.librecans.protocol.model.NoiseControl
import org.librecans.protocol.model.NoiseMode
import org.librecans.protocol.model.Playback
import org.librecans.protocol.model.SpeakToChat
import org.librecans.protocol.model.TouchControls
import org.librecans.protocol.model.TouchPresets
import org.librecans.protocol.state.HeadphoneState

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w412dp-h915dp-xxhdpi")
class ScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val buds = DeviceTarget("AA:BB:CC:DD:EE:01", "WF-1000XM5", DeviceKind.EARBUDS)
    private val paired = PairedDevice(buds.address, buds.name, DeviceKind.EARBUDS, audioConnected = true)

    private fun show(content: @Composable () -> Unit) = compose.setContent {
        LibreCansTheme(dynamicColor = false, content = content)
    }

    @Test
    fun `devices asks for the permission first`() {
        var asked = false
        show {
            DevicesScreen(
                DevicesUiState(bluetooth = BluetoothSnapshot(available = true, enabled = true, permitted = false, devices = emptyList())),
                DevicesActions(onGrantPermission = { asked = true }),
            )
        }
        compose.onNodeWithTag("permission_card").assertIsDisplayed()
        compose.onNodeWithText("Allow").performClick()
        assertThat(asked).isTrue()
    }

    @Test
    fun `devices lists paired headphones and connects on tap`() {
        var connected: PairedDevice? = null
        show {
            DevicesScreen(
                DevicesUiState(bluetooth = BluetoothSnapshot(true, true, true, listOf(paired))),
                DevicesActions(onConnect = { connected = it }),
            )
        }
        compose.onNodeWithText("Connected for audio").assertIsDisplayed()
        compose.onNodeWithTag("device_${buds.address}").performClick()
        assertThat(connected).isEqualTo(paired)
    }

    @Test
    fun `devices shows the empty state, failures and the demo`() {
        var retried = false
        show {
            DevicesScreen(
                DevicesUiState(
                    bluetooth = BluetoothSnapshot(true, true, true, emptyList()),
                    connection = ConnectionState.Failed(buds, FailureReason.CONNECTION_FAILED),
                    showDemo = true,
                ),
                DevicesActions(onRetry = { retried = true }),
            )
        }
        compose.onNodeWithTag("empty_state").assertIsDisplayed()
        compose.onNodeWithTag("screen_list").performScrollToNode(hasTestTag("demo_device"))
        compose.onNodeWithTag("demo_device").assertIsDisplayed()
        compose.onNodeWithText("Try again").performClick()
        assertThat(retried).isTrue()
    }

    @Test
    fun `devices shows progress while connecting`() {
        show {
            DevicesScreen(
                DevicesUiState(bluetooth = BluetoothSnapshot(true, true, true, listOf(paired)), connection = ConnectionState.Connecting(buds, 2)),
                DevicesActions(),
            )
        }
        compose.onNodeWithText("Connecting… (attempt 2)").assertIsDisplayed()
        compose.onNodeWithTag("connecting_indicator").assertExists()
    }

    private val headphones = HeadphoneState(
        batteries = Batteries(
            left = BatteryLevel(80, ChargingState.NOT_CHARGING),
            right = BatteryLevel(76, ChargingState.CHARGING),
            case = BatteryLevel(53, ChargingState.NOT_CHARGING),
        ),
        noiseControl = NoiseControl(NoiseMode.AMBIENT, 12, focusOnVoice = false, inquiredType = 0x17),
        equalizer = Equalizer(EqPresets.BASS, List(6) { 10 }),
        touchControls = TouchControls(listOf(TouchPresets.AMBIENT_SOUND_CONTROL, TouchPresets.PLAYBACK_CONTROL)),
        playback = Playback(volume = 10),
    )

    @Test
    fun `dashboard shows batteries and switches noise mode`() {
        var mode: NoiseMode? = null
        show {
            DashboardScreen(
                DashboardUiState(ConnectionState.Connected(buds), headphones),
                DashboardActions(onNoiseMode = { mode = it }),
            )
        }
        compose.onNodeWithContentDescription("Left battery 80%").assertExists()
        compose.onNodeWithContentDescription("Right battery 76%, Charging").assertExists()
        compose.onNodeWithContentDescription("Case battery 53%").assertExists()
        compose.onNodeWithTag("screen_list").performScrollToNode(hasTestTag("noise_card"))
        compose.onNodeWithText("Ambient sound level").assertIsDisplayed()
        compose.onNodeWithTag("mode_NOISE_CANCELLING").performClick()
        assertThat(mode).isEqualTo(NoiseMode.NOISE_CANCELLING)
    }

    @Test
    fun `dashboard links to the customization screens`() {
        var opened = ""
        show {
            DashboardScreen(
                DashboardUiState(ConnectionState.Connected(buds), headphones),
                DashboardActions(onOpenTouchControls = { opened = "touch" }, onDisconnect = { opened = "disconnect" }),
            )
        }
        compose.onNodeWithTag("screen_list").performScrollToNode(hasTestTag("link_touch"))
        compose.onNodeWithText("Left: Ambient sound control · Right: Playback control").assertIsDisplayed()
        compose.onNodeWithTag("link_touch").performClick()
        assertThat(opened).isEqualTo("touch")
        compose.onNodeWithTag("screen_list").performScrollToNode(hasTestTag("disconnect"))
        compose.onNodeWithTag("disconnect").performClick()
        assertThat(opened).isEqualTo("disconnect")
    }

    @Test
    fun `touch controls can disable an earbud`() {
        var change: Pair<Int, Int>? = null
        val state = TouchModel.uiState(TouchControls(listOf(TouchPresets.AMBIENT_SOUND_CONTROL, TouchPresets.PLAYBACK_CONTROL)))
        show { TouchControlsScreen(state, onBack = {}, onSetPreset = { key, preset -> change = key to preset }, onSetAmbientCycle = {}) }

        compose.onNodeWithTag("screen_list").performScrollToNode(hasTestTag("earbud_1"))
        compose.onNodeWithText("Play / pause").assertIsDisplayed()
        compose.onNodeWithTag("change_1").performClick()
        compose.onNodeWithTag("options_sheet").assertIsDisplayed()
        compose.onNodeWithText("Not assigned").performClick()
        assertThat(change).isEqualTo(1 to TouchPresets.NO_FUNCTION)
    }

    @Test
    fun `equalizer selects presets`() {
        var preset: Int? = null
        show {
            EqualizerScreen(
                EqualizerUiState(Equalizer(EqPresets.OFF, List(6) { 10 })),
                onBack = {},
                onSelectPreset = { preset = it },
                onBandChange = { _, _ -> },
                onClearBassChange = {},
                onReset = {},
            )
        }
        compose.onNodeWithTag("preset_${EqPresets.VOCAL}").performClick()
        assertThat(preset).isEqualTo(EqPresets.VOCAL)
    }

    @Test
    fun `device settings toggle and translate general settings`() {
        var speakToChat: Boolean? = null
        var general: Pair<Int, Boolean>? = null
        val state = DeviceSettingsUiState(
            target = buds,
            headphones = HeadphoneState(
                speakToChat = SpeakToChat(enabled = false),
                generalSettings = listOf(GeneralSetting(0xD2, "MULTIPOINT_SETTING", "MULTIPOINT_SETTING_SUMMARY", titleIsKey = true, enabled = true)),
            ),
        )
        show {
            DeviceSettingsScreen(
                state,
                onBack = {},
                actions = DeviceSettingsActions(onSpeakToChat = { speakToChat = it }, onGeneralSetting = { id, on -> general = id to on }),
            )
        }
        compose.onNodeWithTag("setting_speak_to_chat").assertIsOff().performClick()
        assertThat(speakToChat).isTrue()
        compose.onNodeWithTag("screen_list").performScrollToNode(hasText("Connect to 2 devices at once"))
        compose.onNodeWithTag("setting_gs_210").assertIsOn().performClick()
        assertThat(general).isEqualTo(0xD2 to false)
    }

    @Test
    fun `about changes the theme`() {
        var theme: ThemeMode? = null
        show { AboutScreen(AppSettings(), onBack = {}, actions = AboutActions(onThemeMode = { theme = it })) }
        compose.onNodeWithTag("theme_DARK").performClick()
        assertThat(theme).isEqualTo(ThemeMode.DARK)
    }
}
