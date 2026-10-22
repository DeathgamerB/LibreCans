package org.librecans.app.ui

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.librecans.app.ui.touch.TouchModel
import org.librecans.protocol.model.GestureBinding
import org.librecans.protocol.model.PresetOption
import org.librecans.protocol.model.TouchControls
import org.librecans.protocol.model.TouchFunctions
import org.librecans.protocol.model.TouchKey
import org.librecans.protocol.model.TouchPresets

class TouchModelTest {
    @Test
    fun `without a capability the documented presets are offered plus not assigned`() {
        val touch = TouchControls(presets = listOf(TouchPresets.AMBIENT_SOUND_CONTROL, TouchPresets.PLAYBACK_CONTROL))

        val options = TouchModel.optionsFor(touch, key = 0, current = TouchPresets.AMBIENT_SOUND_CONTROL).map { it.preset }

        assertThat(options).containsExactly(
            TouchPresets.AMBIENT_SOUND_CONTROL,
            TouchPresets.PLAYBACK_CONTROL,
            TouchPresets.VOLUME_CONTROL,
            TouchPresets.NO_FUNCTION,
        ).inOrder()
    }

    @Test
    fun `quick access is offered when it is already in use`() {
        val touch = TouchControls(presets = listOf(TouchPresets.AMBIENT_SOUND_CONTROL_QUICK_ACCESS, TouchPresets.PLAYBACK_CONTROL))

        val options = TouchModel.optionsFor(touch, key = 0, current = TouchPresets.AMBIENT_SOUND_CONTROL_QUICK_ACCESS)

        assertThat(options.map { it.preset }).contains(TouchPresets.AMBIENT_SOUND_CONTROL_QUICK_ACCESS)
    }

    @Test
    fun `reported capabilities win over the fallback and unknown presets are kept`() {
        val reported = listOf(PresetOption(0x47, listOf(GestureBinding(0x00, TouchFunctions.PLAY_PAUSE))))
        val touch = TouchControls(presets = listOf(0x47, 0x99), keys = listOf(TouchKey(0, 0, 0x47, reported), TouchKey(1, 0, 0x47, reported)))

        assertThat(TouchModel.optionsFor(touch, 0, 0x47).map { it.preset }).containsExactly(0x47, TouchPresets.NO_FUNCTION).inOrder()
        assertThat(TouchModel.optionsFor(touch, 1, 0x99).map { it.preset }).containsExactly(0x47, 0x99, TouchPresets.NO_FUNCTION).inOrder()
    }

    @Test
    fun `the cycle section shows only when an earbud controls ambient sound`() {
        val customization = PresetOption(TouchPresets.AMBIENT_SOUND_CONTROL, listOf(GestureBinding(0x00, TouchFunctions.NC_OFF)))
        val withAmbient = TouchModel.uiState(
            TouchControls(listOf(TouchPresets.AMBIENT_SOUND_CONTROL, TouchPresets.PLAYBACK_CONTROL), customizations = listOf(customization)),
        )
        val withoutAmbient = TouchModel.uiState(
            TouchControls(listOf(TouchPresets.VOLUME_CONTROL, TouchPresets.PLAYBACK_CONTROL), customizations = listOf(customization)),
        )

        assertThat(withAmbient.showCycle).isTrue()
        assertThat(withAmbient.ambientCycle).isEqualTo(TouchFunctions.NC_OFF)
        assertThat(withoutAmbient.showCycle).isFalse()
        assertThat(TouchModel.uiState(null).available).isFalse()
    }

    @Test
    fun `gestures of the current preset are listed`() {
        val state = TouchModel.uiState(TouchControls(listOf(TouchPresets.PLAYBACK_CONTROL, TouchPresets.NO_FUNCTION)))

        assertThat(state.sides[0].gestures.map { it.function })
            .containsExactly(TouchFunctions.PLAY_PAUSE, TouchFunctions.NEXT_TRACK, TouchFunctions.PREVIOUS_TRACK, TouchFunctions.VOICE_RECOGNITION)
        assertThat(state.sides[1].gestures).isEmpty()
    }
}
