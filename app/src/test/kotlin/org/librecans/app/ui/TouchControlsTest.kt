package org.librecans.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.librecans.app.ui.theme.LibreCansTheme
import org.librecans.app.ui.touch.TouchControlsScreen
import org.librecans.app.ui.touch.TouchModel
import org.librecans.protocol.model.GestureBinding
import org.librecans.protocol.model.PresetOption
import org.librecans.protocol.model.TouchActions
import org.librecans.protocol.model.TouchControls
import org.librecans.protocol.model.TouchFunctions
import org.librecans.protocol.model.TouchPresets
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w427dp-h952dp-xxhdpi")
class TouchControlsTest {
    @get:Rule
    val compose = createComposeRule()

    private var preset: Pair<Int, Int>? = null
    private var cycle: Int? = null

    private fun show(left: Int, right: Int, ambientCycle: Int = TouchFunctions.NC_ASM_OFF) {
        val cycleSetting = PresetOption(TouchPresets.AMBIENT_SOUND_CONTROL, listOf(GestureBinding(TouchActions.SINGLE_TAP, ambientCycle)))
        val touch = TouchControls(listOf(left, right), customizations = listOf(cycleSetting))
        compose.setContent {
            LibreCansTheme(dynamicColor = false) {
                TouchControlsScreen(TouchModel.uiState(touch), {}, { key, value -> preset = key to value }, { cycle = it })
            }
        }
    }

    @Test
    fun `each earbud is one row with its function and no gesture tables`() {
        show(TouchPresets.AMBIENT_SOUND_CONTROL, TouchPresets.PLAYBACK_CONTROL)
        compose.onNodeWithText("Left earbud").assertIsDisplayed()
        compose.onNodeWithText("Ambient sound control").assertIsDisplayed()
        compose.onNodeWithText("Right earbud").assertIsDisplayed()
        compose.onNodeWithText("Playback control").assertIsDisplayed()
        compose.onNodeWithText("Play / pause").assertDoesNotExist()
        compose.onNodeWithText("Double tap").assertDoesNotExist()
    }

    @Test
    fun `tapping an earbud offers its functions with one line each`() {
        show(TouchPresets.AMBIENT_SOUND_CONTROL, TouchPresets.PLAYBACK_CONTROL)
        compose.onNodeWithTag("earbud_0").performClick()
        compose.onNodeWithTag("options_sheet").assertIsDisplayed()
        compose.onNodeWithText("Tap for louder, hold for quieter").assertExists()
        compose.onNodeWithText("Volume control").performClick()
        assertThat(preset).isEqualTo(0 to TouchPresets.VOLUME_CONTROL)
    }

    @Test
    fun `the noise control cycle is a dropdown`() {
        show(TouchPresets.AMBIENT_SOUND_CONTROL, TouchPresets.PLAYBACK_CONTROL)
        compose.onNodeWithText("Noise canceling → Ambient → Off").assertIsDisplayed()
        compose.onNodeWithTag("cycle_${TouchFunctions.NC_OFF}").assertDoesNotExist()
        compose.onNodeWithTag("cycle_menu").performClick()
        compose.onNodeWithTag("cycle_${TouchFunctions.NC_OFF}").performClick()
        assertThat(cycle).isEqualTo(TouchFunctions.NC_OFF)
        compose.waitForIdle()
        compose.onNodeWithTag("cycle_${TouchFunctions.NC_OFF}").assertDoesNotExist()
    }

    @Test
    fun `the cycle is hidden while no earbud switches noise modes`() {
        show(TouchPresets.PLAYBACK_CONTROL, TouchPresets.VOLUME_CONTROL)
        compose.onNodeWithTag("cycle_menu").assertDoesNotExist()
    }
}
