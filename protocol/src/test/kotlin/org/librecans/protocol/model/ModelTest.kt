package org.librecans.protocol.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ModelTest {
    @Test
    fun `six step equalizers split off clear bass`() {
        val levels = EqualizerMath.levels(listOf(0x14, 0x0b, 0x0a, 0x0d, 0x09, 0x0c))
        assertThat(levels.clearBass).isEqualTo(10)
        assertThat(levels.bands).containsExactly(1, 0, 3, -1, 2).inOrder()
    }

    @Test
    fun `ten band equalizers are centred on six and have no clear bass`() {
        val levels = EqualizerMath.levels(List(10) { 6 + it % 3 })
        assertThat(levels.clearBass).isNull()
        assertThat(levels.bands.take(3)).containsExactly(0, 1, 2).inOrder()
    }

    @Test
    fun `levels convert back to steps and are clamped`() {
        assertThat(EqualizerMath.steps(EqLevels(10, listOf(1, 0, 3, -1, 2))))
            .containsExactly(0x14, 0x0b, 0x0a, 0x0d, 0x09, 0x0c).inOrder()
        assertThat(EqualizerMath.steps(EqLevels(15, listOf(-12, 0, 0, 0, 0)))).containsExactly(20, 0, 10, 10, 10, 10).inOrder()
    }

    @Test
    fun `custom presets`() {
        assertThat(EqPresets.isCustom(EqPresets.CUSTOM)).isTrue()
        assertThat(EqPresets.isCustom(EqPresets.USER_2)).isTrue()
        assertThat(EqPresets.isCustom(EqPresets.BASS)).isFalse()
    }

    @Test
    fun `worn battery is the lower earbud`() {
        val batteries = Batteries(
            left = BatteryLevel(40, ChargingState.NOT_CHARGING),
            right = BatteryLevel(65, ChargingState.NOT_CHARGING),
            case = BatteryLevel(90, ChargingState.CHARGING),
        )
        assertThat(batteries.worn?.percent).isEqualTo(40)
        assertThat(Batteries().isEmpty).isTrue()
    }

    @Test
    fun `touch controls`() {
        val controls = TouchControls(
            presets = listOf(TouchPresets.AMBIENT_SOUND_CONTROL, TouchPresets.PLAYBACK_CONTROL),
            customizations = listOf(
                PresetOption(TouchPresets.AMBIENT_SOUND_CONTROL, listOf(GestureBinding(TouchActions.SINGLE_TAP, TouchFunctions.NC_OFF))),
            ),
        )
        assertThat(controls.ambientCycle()).isEqualTo(TouchFunctions.NC_OFF)
        assertThat(controls.withPreset(TouchKeys.RIGHT, TouchPresets.NO_FUNCTION).presetFor(TouchKeys.RIGHT))
            .isEqualTo(TouchPresets.NO_FUNCTION)
        assertThat(controls.withPreset(5, TouchPresets.NO_FUNCTION)).isEqualTo(controls)
    }
}
