package org.librecans.protocol.decode

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.librecans.protocol.Xm5Capture
import org.librecans.protocol.event.DeviceEvent
import org.librecans.protocol.eventOf
import org.librecans.protocol.mdr.Table
import org.librecans.protocol.model.AudioCodec
import org.librecans.protocol.model.AutoPowerOff
import org.librecans.protocol.model.BatteryLevel
import org.librecans.protocol.model.BatterySlot
import org.librecans.protocol.model.ChargingState
import org.librecans.protocol.model.Features
import org.librecans.protocol.model.NoiseMode
import org.librecans.protocol.model.PlaybackStatus
import org.librecans.protocol.model.SpeakToChatSensitivity
import org.librecans.protocol.model.SpeakToChatTimeout
import org.librecans.protocol.model.TouchPresets
import org.librecans.protocol.model.UpscalingType

/** Decodes real WF-1000XM5 replies. */
class CapturedPayloadsTest {
    @Test
    fun `protocol info announces both tables`() {
        val info = eventOf(Xm5Capture.PROTOCOL_INFO) as DeviceEvent.ProtocolInfo
        assertThat(info.table1).isTrue()
        assertThat(info.table2).isTrue()
        assertThat(info.version).isEqualTo(0x03003018L)
    }

    @Test
    fun `device info`() {
        assertThat(eventOf(Xm5Capture.MODEL_NAME)).isEqualTo(DeviceEvent.ModelName("WF-1000XM5"))
        assertThat(eventOf(Xm5Capture.FIRMWARE)).isEqualTo(DeviceEvent.FirmwareVersion("6.1.0"))
        assertThat(eventOf(Xm5Capture.SERIES_AND_COLOR)).isEqualTo(DeviceEvent.SeriesAndColor(0x30, 0x03))
    }

    @Test
    fun `support functions of table one`() {
        val event = eventOf(Xm5Capture.SUPPORT_TABLE1) as DeviceEvent.SupportedFeatures
        assertThat(event.table).isEqualTo(Table.ONE)
        assertThat(event.features).hasSize(46)
        assertThat(event.features).containsAtLeast(
            Features.NC_ASM_DUAL,
            Features.LR_BATTERY_THRESHOLD,
            Features.CRADLE_BATTERY_THRESHOLD,
            Features.PRESET_EQ,
            Features.UPSCALING,
            Features.ASSIGNABLE_SETTING,
            Features.SMART_TALKING_TYPE2,
            Features.AUTO_POWER_OFF_WEARING,
            Features.PLAYBACK_BY_WEARING,
            Features.HEAD_GESTURE,
            Features.GENERAL_SETTING_1,
            Features.GENERAL_SETTING_2,
            Features.PLAYBACK_CONTROLLER,
        )
        assertThat(event.features).doesNotContain(Features.AUTO_VOLUME)
    }

    @Test
    fun `support functions of table two include voice guidance`() {
        val event = eventOf(Xm5Capture.SUPPORT_TABLE2) as DeviceEvent.SupportedFeatures
        assertThat(event.table).isEqualTo(Table.TWO)
        assertThat(event.features).contains(Features.VOICE_GUIDANCE)
        assertThat(event.features).hasSize(11)
    }

    @Test
    fun `noise control reports ambient sound off at level 20`() {
        val event = eventOf(Xm5Capture.NOISE_CONTROL) as DeviceEvent.NoiseControlChanged
        assertThat(event.control.mode).isEqualTo(NoiseMode.OFF)
        assertThat(event.control.ambientLevel).isEqualTo(20)
        assertThat(event.control.focusOnVoice).isFalse()
        assertThat(event.control.inquiredType).isEqualTo(0x17)
        assertThat(event.settled).isTrue()
    }

    @Test
    fun `batteries`() {
        val pair = eventOf(Xm5Capture.LR_BATTERY) as DeviceEvent.Battery
        assertThat(pair.levels).containsExactly(
            BatterySlot.LEFT, null,
            BatterySlot.RIGHT, BatteryLevel(100, ChargingState.NOT_CHARGING),
        )
        val case = eventOf(Xm5Capture.CASE_BATTERY) as DeviceEvent.Battery
        assertThat(case.levels).containsExactly(BatterySlot.CASE, BatteryLevel(53, ChargingState.NOT_CHARGING))
    }

    @Test
    fun `equalizer`() {
        assertThat(eventOf(Xm5Capture.EQ_STATUS)).isEqualTo(DeviceEvent.EqualizerAvailability(true))
        val eq = eventOf(Xm5Capture.EQ_PARAM) as DeviceEvent.EqualizerChanged
        assertThat(eq.presetId).isEqualTo(0xA0)
        assertThat(eq.steps).containsExactly(0x14, 0x0b, 0x0a, 0x0d, 0x09, 0x0c).inOrder()
    }

    @Test
    fun `touch presets are ambient sound with quick access on the left and playback on the right`() {
        assertThat(eventOf(Xm5Capture.TOUCH_PRESETS)).isEqualTo(
            DeviceEvent.TouchPresetsChanged(
                listOf(TouchPresets.AMBIENT_SOUND_CONTROL_QUICK_ACCESS, TouchPresets.PLAYBACK_CONTROL),
            ),
        )
    }

    @Test
    fun `speak to chat`() {
        assertThat(eventOf(Xm5Capture.SPEAK_TO_CHAT)).isEqualTo(DeviceEvent.SpeakToChatChanged(false))
        assertThat(eventOf(Xm5Capture.SPEAK_TO_CHAT_CONFIG)).isEqualTo(
            DeviceEvent.SpeakToChatConfigChanged(SpeakToChatSensitivity.AUTO, SpeakToChatTimeout.SHORT),
        )
    }

    @Test
    fun `system toggles`() {
        assertThat(eventOf(Xm5Capture.HEAD_GESTURE)).isEqualTo(DeviceEvent.HeadGesturesChanged(false))
        assertThat(eventOf(Xm5Capture.PAUSE_WHEN_REMOVED)).isEqualTo(DeviceEvent.PauseWhenRemovedChanged(true))
    }

    @Test
    fun `auto power off when removed from ears`() {
        assertThat(eventOf(Xm5Capture.AUTO_POWER_OFF)).isEqualTo(
            DeviceEvent.AutoPowerOffChanged(AutoPowerOff.WHEN_REMOVED, lastSelected = 0x00, withWearingDetection = true),
        )
    }

    @Test
    fun `audio`() {
        assertThat(eventOf(Xm5Capture.CODEC)).isEqualTo(DeviceEvent.Codec(AudioCodec.AAC))
        assertThat(eventOf(Xm5Capture.UPSCALING)).isEqualTo(DeviceEvent.UpscalingChanged(true))
        assertThat(eventOf(Xm5Capture.UPSCALING_CAPABILITY))
            .isEqualTo(DeviceEvent.UpscalingCapabilityChanged(UpscalingType.DSEE_HX_AI))
    }

    @Test
    fun `playback`() {
        assertThat(eventOf(Xm5Capture.VOLUME)).isEqualTo(DeviceEvent.VolumeChanged(12))
        assertThat(eventOf(Xm5Capture.PLAY_STATUS)).isEqualTo(DeviceEvent.PlaybackStatusChanged(PlaybackStatus.PAUSED))
    }

    @Test
    fun `general settings carry translatable names`() {
        val sidetone = (eventOf(Xm5Capture.SIDETONE_CAPABILITY) as DeviceEvent.GeneralSettingDescribed).setting
        assertThat(sidetone.id).isEqualTo(0xD1)
        assertThat(sidetone.title).isEqualTo("SIDETONE_SETTING")
        assertThat(sidetone.summary).isEqualTo("SIDETONE_SETTING_SUMMARY")
        assertThat(sidetone.titleIsKey).isTrue()
        assertThat(eventOf(Xm5Capture.SIDETONE_PARAM)).isEqualTo(DeviceEvent.GeneralSettingChanged(0xD1, false))

        val multipoint = (eventOf(Xm5Capture.MULTIPOINT_CAPABILITY) as DeviceEvent.GeneralSettingDescribed).setting
        assertThat(multipoint.title).isEqualTo("MULTIPOINT_SETTING")
        assertThat(multipoint.summary).isEqualTo("MULTIPOINT_SETTING_SUMMARY_LDAC_AVAILABLE")
        assertThat(eventOf(Xm5Capture.MULTIPOINT_PARAM)).isEqualTo(DeviceEvent.GeneralSettingChanged(0xD2, true))
    }

    @Test
    fun `voice guidance`() {
        assertThat(eventOf(Xm5Capture.VOICE_GUIDANCE)).isEqualTo(DeviceEvent.VoiceGuidanceChanged(true))
        assertThat(eventOf(Xm5Capture.VOICE_GUIDANCE_VOLUME)).isEqualTo(DeviceEvent.VoiceGuidanceVolumeChanged(0))
    }
}
