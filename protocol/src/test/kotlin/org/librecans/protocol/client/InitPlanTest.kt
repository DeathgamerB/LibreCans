package org.librecans.protocol.client

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.librecans.protocol.Xm5Capture
import org.librecans.protocol.event.DeviceEvent
import org.librecans.protocol.eventOf
import org.librecans.protocol.model.Features

class InitPlanTest {
    private val xm5 = (eventOf(Xm5Capture.SUPPORT_TABLE1) as DeviceEvent.SupportedFeatures).features +
        (eventOf(Xm5Capture.SUPPORT_TABLE2) as DeviceEvent.SupportedFeatures).features

    @Test
    fun `the WF-1000XM5 plan covers all of its controls`() {
        val labels = InitPlan.requests(xm5).map { it.label }

        assertThat(labels).containsAtLeast(
            "codec", "battery", "noiseControl", "eqCapability", "equalizer", "upscaling",
            "touchCapability", "touchPresets", "touchCustomizations", "volume", "speakToChat",
            "speakToChatConfig", "pauseWhenRemoved", "headGestures", "autoPowerOff",
            "generalSettingCapability", "voiceGuidance", "voiceGuidanceVolume",
        )
        assertThat(labels).containsNoneOf("adaptiveVolume", "connectionPriority")
        assertThat(labels.count { it == "battery" }).isEqualTo(2)
        assertThat(labels.count { it == "generalSettingCapability" }).isEqualTo(2)
    }

    @Test
    fun `nothing is requested without features`() {
        assertThat(InitPlan.requests(emptySet())).isEmpty()
        assertThat(InitPlan.noiseControlType(emptySet())).isNull()
        assertThat(InitPlan.autoPowerOffWearing(emptySet())).isNull()
    }

    @Test
    fun `noise control type follows the advertised mode`() {
        assertThat(InitPlan.noiseControlType(setOf(Features.NC_ASM_DUAL))).isEqualTo(0x17)
        assertThat(InitPlan.noiseControlType(setOf(Features.AMBIENT_SOUND_LEVEL))).isEqualTo(0x22)
        assertThat(InitPlan.noiseControlType(setOf(Features.NC_ASM_DUAL_AUTO))).isEqualTo(0x15)
    }

    @Test
    fun `single battery headphones ask for one battery`() {
        val requests = InitPlan.batteryRequests(setOf(Features.BATTERY_LEVEL))
        assertThat(requests.map { it.payload.toList() }).containsExactly(listOf<Byte>(0x22, 0x00))
    }
}
