package org.librecans.protocol.decode

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.librecans.protocol.event.DeviceEvent
import org.librecans.protocol.mdr.Table
import org.librecans.protocol.model.GestureBinding
import org.librecans.protocol.model.NoiseMode
import org.librecans.protocol.model.PresetOption
import org.librecans.protocol.model.TouchActions
import org.librecans.protocol.model.TouchFunctions
import org.librecans.protocol.model.TouchKey
import org.librecans.protocol.model.TouchPresets
import org.librecans.protocol.payloadOf

class DecoderTest {
    private fun decode(vararg bytes: Int, table: Table = Table.ONE) = MdrDecoder.decode(table, payloadOf(*bytes))

    @Test
    fun `assignable capability lists fixed and customizable gestures per key`() {
        val event = decode(
            0xF1, 0x03, 0x01,
            0x01, 0x00, 0x00, 0x02,
            0x00, 0x01, 0x01, 0x10, 0x10, 0x00, 0x01, 0x04, 0x01, 0x02, 0x03, 0x04,
            0x20, 0x02, 0x00, 0x00, 0x20, 0x01, 0x21,
        ) as DeviceEvent.TouchCapability

        assertThat(event.keys).containsExactly(
            TouchKey(
                key = 0x01,
                type = 0x00,
                defaultPreset = TouchPresets.AMBIENT_SOUND_CONTROL,
                options = listOf(
                    PresetOption(
                        TouchPresets.AMBIENT_SOUND_CONTROL,
                        listOf(
                            GestureBinding(TouchActions.TAP_AND_HOLD, TouchFunctions.QUICK_ATTENTION),
                            GestureBinding(TouchActions.SINGLE_TAP, TouchFunctions.NC_ASM_OFF),
                        ),
                    ),
                    PresetOption(
                        TouchPresets.PLAYBACK_CONTROL,
                        listOf(
                            GestureBinding(TouchActions.SINGLE_TAP, TouchFunctions.PLAY_PAUSE),
                            GestureBinding(TouchActions.DOUBLE_TAP, TouchFunctions.NEXT_TRACK),
                        ),
                    ),
                ),
            ),
        )
    }

    @Test
    fun `assignable customizations carry the ambient sound cycle`() {
        val event = decode(0xFB, 0x03, 0x01, 0x35, 0x01, 0x00, 0x02) as DeviceEvent.TouchCustomizations
        assertThat(event.options).containsExactly(
            PresetOption(0x35, listOf(GestureBinding(TouchActions.SINGLE_TAP, TouchFunctions.NC_ASM))),
        )
    }

    @Test
    fun `noise cancelling on`() {
        val event = decode(0x69, 0x17, 0x01, 0x01, 0x00, 0x00, 0x05) as DeviceEvent.NoiseControlChanged
        assertThat(event.control.mode).isEqualTo(NoiseMode.NOISE_CANCELLING)
    }

    @Test
    fun `ambient sound with focus on voice while the slider is dragged`() {
        val event = decode(0x69, 0x17, 0x00, 0x01, 0x01, 0x01, 0x0c) as DeviceEvent.NoiseControlChanged
        assertThat(event.control.mode).isEqualTo(NoiseMode.AMBIENT)
        assertThat(event.control.focusOnVoice).isTrue()
        assertThat(event.control.ambientLevel).isEqualTo(12)
        assertThat(event.settled).isFalse()
    }

    @Test
    fun `layouts with an NC value byte read the trailing fields`() {
        val event = decode(0x67, 0x15, 0x01, 0x01, 0x01, 0x03, 0x01, 0x09) as DeviceEvent.NoiseControlChanged
        assertThat(event.control.mode).isEqualTo(NoiseMode.AMBIENT)
        assertThat(event.control.focusOnVoice).isTrue()
        assertThat(event.control.ambientLevel).isEqualTo(9)
    }

    @Test
    fun `ambient only devices have no mode byte`() {
        val event = decode(0x67, 0x22, 0x01, 0x01, 0x00, 0x0e) as DeviceEvent.NoiseControlChanged
        assertThat(event.control.mode).isEqualTo(NoiseMode.AMBIENT)
        assertThat(event.control.ambientLevel).isEqualTo(14)
    }

    @Test
    fun `battery notifications decode like replies`() {
        val event = decode(0x25, 0x09, 0x32, 0x01, 0x33, 0x00, 0x64, 0x64) as DeviceEvent.Battery
        assertThat(event.levels.values.map { it?.percent }).containsExactly(50, 51)
        assertThat(event.levels.values.first()?.isCharging).isTrue()
    }

    @Test
    fun `equalizer capability lists named presets`() {
        val event = decode(
            0x51, 0x00, 0x06, 0x15, 0x02,
            0x00, 0x03, 'O'.code, 'f'.code, 'f'.code,
            0x10, 0x00,
        ) as DeviceEvent.EqualizerCapabilityChanged
        assertThat(event.capability.presets.map { it.id to it.name }).containsExactly(0x00 to "Off", 0x10 to null)
    }

    @Test
    fun `truncated payloads are ignored instead of crashing`() {
        listOf(
            payloadOf(0x23, 0x09, 0x32),
            payloadOf(0x67, 0x17, 0x01),
            payloadOf(0x05, 0x02, 0x09, 'v'.code),
            payloadOf(0xF1, 0x03, 0x01, 0x00),
            payloadOf(0x57, 0x00, 0xA0, 0x06, 0x01),
            payloadOf(0xD1, 0xD1, 0x00, 0x01, 0x10),
        ).forEach { assertThat(MdrDecoder.decode(Table.ONE, it)).isNull() }
    }

    @Test
    fun `unknown commands and empty payloads yield nothing`() {
        assertThat(decode(0x99, 0x01)).isNull()
        assertThat(MdrDecoder.decode(Table.ONE, ByteArray(0))).isNull()
        assertThat(decode(0x47, 0x77, 0x00, table = Table.TWO)).isNull()
    }

    @Test
    fun `list type general settings are skipped`() {
        assertThat(decode(0xD7, 0xD3, 0x01, 0x02)).isNull()
    }
}
