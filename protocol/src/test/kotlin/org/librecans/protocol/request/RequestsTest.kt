package org.librecans.protocol.request

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.librecans.protocol.mdr.MdrRequest
import org.librecans.protocol.mdr.Table
import org.librecans.protocol.model.AutoPowerOff
import org.librecans.protocol.model.ConnectionPriority
import org.librecans.protocol.model.GestureBinding
import org.librecans.protocol.model.NoiseControl
import org.librecans.protocol.model.NoiseMode
import org.librecans.protocol.model.PlaybackCommand
import org.librecans.protocol.model.PresetOption
import org.librecans.protocol.model.SpeakToChatSensitivity
import org.librecans.protocol.model.SpeakToChatTimeout
import org.librecans.protocol.wire.toHex

/** Byte exact expectations, cross-checked against Gadgetbridge and libmdr. */
class RequestsTest {
    private fun MdrRequest.hex() = payload.toHex()

    @Test
    fun `connect`() {
        assertThat(ConnectRequests.protocolInfo().hex()).isEqualTo("00 00")
        assertThat(ConnectRequests.firmware().hex()).isEqualTo("04 02")
        assertThat(ConnectRequests.modelName().hex()).isEqualTo("04 01")
        assertThat(ConnectRequests.supportedFeatures().hex()).isEqualTo("06 00")
        assertThat(ConnectRequests.supportedFeaturesTable2().table).isEqualTo(Table.TWO)
    }

    @Test
    fun `noise control`() {
        val ambient = NoiseControl(NoiseMode.AMBIENT, 12, focusOnVoice = false, inquiredType = 0x17)
        assertThat(NoiseControlRequests.set(ambient, settled = true).hex()).isEqualTo("68 17 01 01 01 00 0c")
        assertThat(NoiseControlRequests.set(ambient, settled = false).hex()).isEqualTo("68 17 00 01 01 00 0c")

        val voice = ambient.copy(focusOnVoice = true, ambientLevel = 20)
        assertThat(NoiseControlRequests.set(voice, settled = true).hex()).isEqualTo("68 17 01 01 01 01 14")

        val nc = ambient.copy(mode = NoiseMode.NOISE_CANCELLING)
        assertThat(NoiseControlRequests.set(nc, settled = true).hex()).isEqualTo("68 17 01 01 00 00 0c")

        val off = ambient.copy(mode = NoiseMode.OFF)
        assertThat(NoiseControlRequests.set(off, settled = true).hex()).isEqualTo("68 17 01 00 01 00 0c")
    }

    @Test
    fun `noise control echoes model specific bytes from the template`() {
        val received = listOf(0x67, 0x19, 0x01, 0x01, 0x00, 0x00, 0x05, 0x01, 0x02)
        val control = NoiseControl(NoiseMode.AMBIENT, 7, focusOnVoice = true, inquiredType = 0x19, template = received)

        assertThat(NoiseControlRequests.set(control, settled = true).hex()).isEqualTo("68 19 01 01 01 01 07 01 02")
    }

    @Test
    fun `noise control for layouts with an NC value keeps it`() {
        val received = listOf(0x67, 0x15, 0x01, 0x01, 0x00, 0x03, 0x00, 0x05)
        val control = NoiseControl(NoiseMode.AMBIENT, 9, focusOnVoice = false, inquiredType = 0x15, template = received)

        assertThat(NoiseControlRequests.set(control, settled = true).hex()).isEqualTo("68 15 01 01 01 03 00 09")
    }

    @Test
    fun `equalizer`() {
        assertThat(EqualizerRequests.get().hex()).isEqualTo("56 00")
        assertThat(EqualizerRequests.setPreset(0x16).hex()).isEqualTo("58 00 16 00")
        assertThat(EqualizerRequests.setSteps(0xA0, listOf(12, 10, 10, 13, 9, 7)).hex())
            .isEqualTo("58 00 a0 06 0c 0a 0a 0d 09 07")
        assertThat(EqualizerRequests.capability().hex()).isEqualTo("50 00 01")
    }

    @Test
    fun `touch controls`() {
        assertThat(SystemRequests.touchPresets().hex()).isEqualTo("f6 03")
        assertThat(SystemRequests.setTouchPresets(listOf(0x00, 0x20)).hex()).isEqualTo("f8 03 02 00 20")
        assertThat(SystemRequests.setTouchPresets(listOf(0xFF, 0x10)).hex()).isEqualTo("f8 03 02 ff 10")
        assertThat(SystemRequests.touchCustomizations().hex()).isEqualTo("fa 03")
        val cycle = PresetOption(0x00, listOf(GestureBinding(0x00, 0x02)))
        assertThat(SystemRequests.setTouchCustomizations(listOf(cycle)).hex()).isEqualTo("fc 03 01 00 01 00 02")
        assertThat(SystemRequests.touchCapability().hex()).isEqualTo("f0 03")
    }

    @Test
    fun `system toggles use inverted on off bytes`() {
        assertThat(SystemRequests.setSpeakToChat(true).hex()).isEqualTo("f8 0c 00 01")
        assertThat(SystemRequests.setSpeakToChat(false).hex()).isEqualTo("f8 0c 01 01")
        assertThat(SystemRequests.setSpeakToChatConfig(SpeakToChatSensitivity.HIGH, SpeakToChatTimeout.LONG).hex())
            .isEqualTo("fc 0c 01 02")
        assertThat(SystemRequests.setPauseWhenRemoved(true).hex()).isEqualTo("f8 01 00")
        assertThat(SystemRequests.setHeadGestures(false).hex()).isEqualTo("f8 0f 01")
        assertThat(SystemRequests.setAdaptiveVolume(true).hex()).isEqualTo("f8 0a 00")
    }

    @Test
    fun `power`() {
        assertThat(PowerRequests.powerOff().hex()).isEqualTo("24 03 01")
        assertThat(PowerRequests.battery(0x09).hex()).isEqualTo("22 09")
        assertThat(PowerRequests.autoPowerOff(wearingDetection = true).hex()).isEqualTo("26 05")
        assertThat(PowerRequests.setAutoPowerOff(true, AutoPowerOff.WHEN_REMOVED, 0x00).hex()).isEqualTo("28 05 10 00")
        assertThat(PowerRequests.setAutoPowerOff(false, AutoPowerOff.DISABLED, 0x00).hex()).isEqualTo("28 04 11 00")
    }

    @Test
    fun `audio`() {
        assertThat(AudioRequests.codec().hex()).isEqualTo("12 02")
        assertThat(AudioRequests.setUpscaling(true).hex()).isEqualTo("e8 01 01")
        assertThat(AudioRequests.setUpscaling(false).hex()).isEqualTo("e8 01 00")
        assertThat(AudioRequests.setConnectionPriority(ConnectionPriority.STABLE_CONNECTION).hex()).isEqualTo("e8 00 01")
    }

    @Test
    fun `general settings playback and voice guidance`() {
        assertThat(GeneralSettingRequests.capability(0xD2).hex()).isEqualTo("d0 d2 01")
        assertThat(GeneralSettingRequests.set(0xD2, false).hex()).isEqualTo("d8 d2 00 01")
        assertThat(PlayRequests.setVolume(15).hex()).isEqualTo("a8 20 0f")
        assertThat(PlayRequests.control(PlaybackCommand.NEXT_TRACK).hex()).isEqualTo("a4 01 00 02")
        assertThat(VoiceGuidanceRequests.set(false).hex()).isEqualTo("48 01 01")
        assertThat(VoiceGuidanceRequests.setVolume(-2).hex()).isEqualTo("48 20 fe 00")
        assertThat(VoiceGuidanceRequests.get().table).isEqualTo(Table.TWO)
    }
}
