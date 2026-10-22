package org.librecans.protocol.state

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.librecans.protocol.event.DeviceEvent
import org.librecans.protocol.mdr.Table
import org.librecans.protocol.model.BatteryLevel
import org.librecans.protocol.model.BatterySlot
import org.librecans.protocol.model.ChargingState
import org.librecans.protocol.model.Feature
import org.librecans.protocol.model.GeneralSetting
import org.librecans.protocol.model.SpeakToChatSensitivity
import org.librecans.protocol.model.SpeakToChatTimeout

class StateReducerTest {
    private fun HeadphoneState.after(vararg events: DeviceEvent) = events.fold(this) { s, e -> s.reduce(e) }

    @Test
    fun `battery events only touch their slots`() {
        val charging = BatteryLevel(20, ChargingState.CHARGING)
        val state = HeadphoneState().after(
            DeviceEvent.Battery(mapOf(BatterySlot.LEFT to charging, BatterySlot.RIGHT to charging)),
            DeviceEvent.Battery(mapOf(BatterySlot.CASE to BatteryLevel(70, ChargingState.NOT_CHARGING))),
            DeviceEvent.Battery(mapOf(BatterySlot.LEFT to null, BatterySlot.RIGHT to charging)),
        )

        assertThat(state.batteries.left).isNull()
        assertThat(state.batteries.right).isEqualTo(charging)
        assertThat(state.batteries.case?.percent).isEqualTo(70)
    }

    @Test
    fun `features are replaced per table`() {
        val state = HeadphoneState().after(
            DeviceEvent.SupportedFeatures(Table.ONE, setOf(Feature(Table.ONE, 1), Feature(Table.ONE, 2))),
            DeviceEvent.SupportedFeatures(Table.TWO, setOf(Feature(Table.TWO, 9))),
            DeviceEvent.SupportedFeatures(Table.ONE, setOf(Feature(Table.ONE, 3))),
        )

        assertThat(state.features).containsExactly(Feature(Table.ONE, 3), Feature(Table.TWO, 9))
    }

    @Test
    fun `a preset change without steps keeps the previous curve until the device sends it`() {
        val state = HeadphoneState().after(
            DeviceEvent.EqualizerChanged(0xA0, listOf(10, 10, 10, 10, 10, 10)),
            DeviceEvent.EqualizerAvailability(false),
            DeviceEvent.EqualizerChanged(0x16, emptyList()),
        )

        assertThat(state.equalizer?.presetId).isEqualTo(0x16)
        assertThat(state.equalizer?.steps).hasSize(6)
        assertThat(state.equalizer?.available).isFalse()
    }

    @Test
    fun `general setting values and descriptions merge in any order`() {
        val state = HeadphoneState().after(
            DeviceEvent.GeneralSettingChanged(0xD2, true),
            DeviceEvent.GeneralSettingDescribed(GeneralSetting(0xD2, "MULTIPOINT_SETTING", null, true)),
            DeviceEvent.GeneralSettingDescribed(GeneralSetting(0xD1, "SIDETONE_SETTING", null, true)),
        )

        assertThat(state.generalSettings.map { it.id }).containsExactly(0xD1, 0xD2).inOrder()
        assertThat(state.generalSettings.last().enabled).isTrue()
        assertThat(state.generalSettings.last().title).isEqualTo("MULTIPOINT_SETTING")
    }

    @Test
    fun `speak to chat config and switch merge`() {
        val state = HeadphoneState().after(
            DeviceEvent.SpeakToChatConfigChanged(SpeakToChatSensitivity.HIGH, SpeakToChatTimeout.NEVER),
            DeviceEvent.SpeakToChatChanged(true),
        )

        assertThat(state.speakToChat?.enabled).isTrue()
        assertThat(state.speakToChat?.sensitivity).isEqualTo(SpeakToChatSensitivity.HIGH)
        assertThat(state.speakToChat?.timeout).isEqualTo(SpeakToChatTimeout.NEVER)
    }

    @Test
    fun `touch capability and presets build one model`() {
        val state = HeadphoneState().after(
            DeviceEvent.TouchPresetsChanged(listOf(0x00, 0x20)),
            DeviceEvent.TouchCapability(emptyList()),
        )

        assertThat(state.touchControls?.presets).containsExactly(0x00, 0x20).inOrder()
    }
}
