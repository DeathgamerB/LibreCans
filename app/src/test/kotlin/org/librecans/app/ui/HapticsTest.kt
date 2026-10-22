package org.librecans.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.librecans.app.data.bluetooth.DeviceKind
import org.librecans.app.data.connection.ConnectionState
import org.librecans.app.data.connection.DeviceTarget
import org.librecans.app.ui.components.Segment
import org.librecans.app.ui.components.SwitchItem
import org.librecans.app.ui.dashboard.DashboardActions
import org.librecans.app.ui.dashboard.DashboardScreen
import org.librecans.app.ui.dashboard.DashboardUiState
import org.librecans.app.ui.theme.LibreCansTheme
import org.librecans.protocol.model.NoiseControl
import org.librecans.protocol.model.NoiseMode
import org.librecans.protocol.model.Playback
import org.librecans.protocol.state.HeadphoneState
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w412dp-h915dp-xxhdpi")
class HapticsTest {
    @get:Rule
    val compose = createComposeRule()

    private val performed = mutableListOf<HapticFeedbackType>()
    private val recorder = object : HapticFeedback {
        override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
            performed += hapticFeedbackType
        }
    }

    private val buds = DeviceTarget("AA:BB:CC:DD:EE:01", "WF-1000XM5", DeviceKind.EARBUDS)
    private val headphones = HeadphoneState(
        noiseControl = NoiseControl(NoiseMode.AMBIENT, 12, focusOnVoice = false, inquiredType = 0x17),
        playback = Playback(volume = 10),
    )

    private fun show(content: @Composable () -> Unit) = compose.setContent {
        CompositionLocalProvider(LocalHapticFeedback provides recorder) {
            LibreCansTheme(dynamicColor = false, content = content)
        }
    }

    private fun showDashboard() = show {
        DashboardScreen(DashboardUiState(ConnectionState.Connected(buds), headphones), DashboardActions())
    }

    @Test
    fun `choosing a new noise mode ticks once, the current one stays silent`() {
        showDashboard()
        compose.onNodeWithTag("screen_list").performScrollToNode(hasTestTag("noise_card"))
        compose.onNodeWithTag("mode_AMBIENT").performClick()
        assertThat(performed).isEmpty()
        compose.onNodeWithTag("mode_NOISE_CANCELLING").performClick()
        assertThat(performed).containsExactly(HapticFeedbackType.SegmentTick)
    }

    @Test
    fun `switches feel different turning on and off`() {
        show {
            Column {
                SwitchItem("On", checked = false, onCheckedChange = {}, segment = Segment(0, 2))
                SwitchItem("Off", checked = true, onCheckedChange = {}, segment = Segment(1, 2))
            }
        }
        compose.onNodeWithText("On").performClick()
        compose.onNodeWithText("Off").performClick()
        assertThat(performed).containsExactly(HapticFeedbackType.ToggleOn, HapticFeedbackType.ToggleOff).inOrder()
    }

    @Test
    fun `transport keys tap lightly and disconnecting lands heavily`() {
        showDashboard()
        compose.onNodeWithTag("screen_list").performScrollToNode(hasTestTag("play_pause"))
        compose.onNodeWithTag("play_pause").performClick()
        compose.onNodeWithTag("screen_list").performScrollToNode(hasTestTag("disconnect"))
        compose.onNodeWithTag("disconnect").performClick()
        assertThat(performed).containsExactly(HapticFeedbackType.ToggleOn, HapticFeedbackType.LongPress).inOrder()
    }

    @Test
    fun `dashboard header names the device and its link`() {
        showDashboard()
        compose.onNodeWithText("WF-1000XM5").assertExists()
        compose.onNodeWithText("Connected").assertExists()
    }
}
