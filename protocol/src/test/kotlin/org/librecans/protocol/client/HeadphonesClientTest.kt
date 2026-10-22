package org.librecans.protocol.client

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.librecans.protocol.mdr.Table
import org.librecans.protocol.model.AutoPowerOff
import org.librecans.protocol.model.EqLevels
import org.librecans.protocol.model.EqPresets
import org.librecans.protocol.model.Features
import org.librecans.protocol.model.NoiseMode
import org.librecans.protocol.model.PlaybackCommand
import org.librecans.protocol.model.PlaybackStatus
import org.librecans.protocol.model.SpeakToChatSensitivity
import org.librecans.protocol.model.SpeakToChatTimeout
import org.librecans.protocol.model.TouchFunctions
import org.librecans.protocol.model.TouchKeys
import org.librecans.protocol.model.TouchPresets
import org.librecans.protocol.session.MdrSession
import org.librecans.protocol.sim.InMemoryLink
import org.librecans.protocol.sim.SimulatedHeadphones

/** Runs the complete stack against the simulated WF-1000XM5. */
class HeadphonesClientTest {
    private class Rig(val client: HeadphonesClient, val device: SimulatedHeadphones, val session: MdrSession)

    private fun TestScope.connect(): Rig {
        val link = InMemoryLink()
        val device = SimulatedHeadphones(link.device).also { it.start(backgroundScope) }
        val session = MdrSession(link.app, backgroundScope)
        val client = HeadphonesClient(session, backgroundScope)
        session.start()
        return Rig(client, device, session)
    }

    private suspend fun TestScope.ready(): Rig = connect().also {
        assertThat(it.client.initialize()).isEqualTo(InitResult.READY)
        runCurrent()
    }

    @Test
    fun `initialization discovers identity features and current settings`() = runTest {
        val state = ready().client.state.value

        assertThat(state.identity.modelName).isEqualTo("WF-1000XM5")
        assertThat(state.identity.firmware).isEqualTo("6.1.0")
        assertThat(state.identity.uniqueId).isEqualTo("0000DEMO5A17E5")
        assertThat(state.supports(Features.NC_ASM_DUAL)).isTrue()
        assertThat(state.supports(Features.VOICE_GUIDANCE)).isTrue()
        assertThat(state.batteries.left?.percent).isEqualTo(80)
        assertThat(state.batteries.right?.percent).isEqualTo(76)
        assertThat(state.batteries.case?.percent).isEqualTo(53)
        assertThat(state.noiseControl?.mode).isEqualTo(NoiseMode.NOISE_CANCELLING)
        assertThat(state.equalizer?.presetId).isEqualTo(EqPresets.CUSTOM)
        assertThat(state.equalizer?.levels?.clearBass).isEqualTo(10)
        assertThat(state.equalizerCapability?.presets).hasSize(12)
        assertThat(state.upscaling).isTrue()
        assertThat(state.touchControls?.presets)
            .containsExactly(TouchPresets.AMBIENT_SOUND_CONTROL_QUICK_ACCESS, TouchPresets.PLAYBACK_CONTROL).inOrder()
        assertThat(state.touchControls?.keys).hasSize(2)
        assertThat(state.touchControls?.ambientCycle()).isEqualTo(TouchFunctions.NC_ASM_OFF)
        assertThat(state.speakToChat?.sensitivity).isEqualTo(SpeakToChatSensitivity.AUTO)
        assertThat(state.pauseWhenRemoved).isTrue()
        assertThat(state.headGestures).isFalse()
        assertThat(state.autoPowerOff?.current).isEqualTo(AutoPowerOff.WHEN_REMOVED)
        assertThat(state.autoPowerOffOptions).contains(AutoPowerOff.DISABLED)
        assertThat(state.generalSettings.map { it.title }).containsExactly("SIDETONE_SETTING", "MULTIPOINT_SETTING").inOrder()
        assertThat(state.generalSettings.map { it.enabled }).containsExactly(false, true).inOrder()
        assertThat(state.playback?.volume).isEqualTo(12)
        assertThat(state.voiceGuidance?.enabled).isTrue()
        assertThat(state.adaptiveVolume).isNull()
        assertThat(state.connectionPriority).isNull()
    }

    @Test
    fun `initialization only asks for what the headphones support`() = runTest {
        val requests = ready().device.received.filter { it.first == Table.ONE }.map { it.second }

        assertThat(requests).containsAtLeast(listOf(0x22, 0x09), listOf(0x22, 0x0A), listOf(0x66, 0x17))
        assertThat(requests).containsNoneOf(listOf(0x22, 0x01), listOf(0xE6, 0x00), listOf(0xF6, 0x0A), listOf(0xD0, 0xD3, 0x01))
    }

    @Test
    fun `noise control changes reach the headphones`() = runTest {
        val rig = ready()

        rig.client.setNoiseControl(NoiseMode.AMBIENT, ambientLevel = 7, focusOnVoice = true)
        runCurrent()

        with(rig.device.model) {
            assertThat(ncEnabled).isTrue()
            assertThat(ncAmbient).isTrue()
            assertThat(focusOnVoice).isTrue()
            assertThat(ambientLevel).isEqualTo(7)
        }
        assertThat(rig.client.state.value.noiseControl?.mode).isEqualTo(NoiseMode.AMBIENT)
    }

    @Test
    fun `a tap on the earbuds is reflected in the state`() = runTest {
        val rig = ready()

        rig.device.simulateAmbientTap()
        runCurrent()

        assertThat(rig.client.state.value.noiseControl?.mode).isEqualTo(NoiseMode.AMBIENT)
    }

    @Test
    fun `touch presets can be reassigned and disabled`() = runTest {
        val rig = ready()

        rig.client.setTouchPreset(TouchKeys.LEFT, TouchPresets.VOLUME_CONTROL)
        rig.client.setTouchPreset(TouchKeys.RIGHT, TouchPresets.NO_FUNCTION)
        runCurrent()

        assertThat(rig.device.model.touchPresets).containsExactly(TouchPresets.VOLUME_CONTROL, TouchPresets.NO_FUNCTION).inOrder()
        assertThat(rig.client.state.value.touchControls?.presets)
            .containsExactly(TouchPresets.VOLUME_CONTROL, TouchPresets.NO_FUNCTION).inOrder()
    }

    @Test
    fun `the ambient sound cycle can be narrowed`() = runTest {
        val rig = ready()

        rig.client.setAmbientCycle(TouchFunctions.NC_ASM)
        runCurrent()

        assertThat(rig.device.model.ambientCycle).isEqualTo(TouchFunctions.NC_ASM)
        assertThat(rig.client.state.value.touchControls?.ambientCycle()).isEqualTo(TouchFunctions.NC_ASM)
    }

    @Test
    fun `equalizer presets and custom bands`() = runTest {
        val rig = ready()

        rig.client.setEqualizerPreset(EqPresets.BASS)
        runCurrent()
        assertThat(rig.client.state.value.equalizer?.presetId).isEqualTo(EqPresets.BASS)
        assertThat(rig.client.state.value.equalizer?.steps).containsExactly(14, 14, 11, 10, 10, 10).inOrder()

        rig.client.setEqualizerLevels(EqLevels(clearBass = -3, bands = listOf(1, 2, 3, 4, 5)))
        runCurrent()
        assertThat(rig.device.model.eqPreset).isEqualTo(EqPresets.CUSTOM)
        assertThat(rig.device.model.eqSteps).containsExactly(7, 11, 12, 13, 14, 15).inOrder()
    }

    @Test
    fun `device settings round trip`() = runTest {
        val rig = ready()
        with(rig.client) {
            setSpeakToChat(true)
            setSpeakToChatConfig(SpeakToChatSensitivity.LOW, SpeakToChatTimeout.LONG)
            setPauseWhenRemoved(false)
            setHeadGestures(true)
            setUpscaling(false)
            setAutoPowerOff(AutoPowerOff.DISABLED)
            setGeneralSetting(0xD2, false)
            setVolume(20)
            setVoiceGuidance(false)
            setVoiceGuidanceVolume(-1)
            playback(PlaybackCommand.PLAY)
        }
        runCurrent()

        with(rig.device.model) {
            assertThat(speakToChat).isTrue()
            assertThat(speakToChatSensitivity).isEqualTo(SpeakToChatSensitivity.LOW.code)
            assertThat(speakToChatTimeout).isEqualTo(SpeakToChatTimeout.LONG.code)
            assertThat(pauseWhenRemoved).isFalse()
            assertThat(headGestures).isTrue()
            assertThat(upscaling).isFalse()
            assertThat(autoPowerOff).isEqualTo(AutoPowerOff.DISABLED.code)
            assertThat(generalSettings[0xD2]).isFalse()
            assertThat(volume).isEqualTo(20)
            assertThat(voiceGuidance).isFalse()
            assertThat(voiceGuidanceVolume).isEqualTo(-1)
            assertThat(playing).isTrue()
        }
        assertThat(rig.client.state.value.playback?.status).isEqualTo(PlaybackStatus.PLAYING)
    }

    @Test
    fun `battery notifications update the state`() = runTest {
        val rig = ready()

        rig.device.drainBattery(10)
        runCurrent()

        assertThat(rig.client.state.value.batteries.left?.percent).isEqualTo(70)
    }

    @Test
    fun `powering off ends the session`() = runTest {
        val rig = ready()

        rig.client.powerOff()

        assertThat(rig.session.closed.await()).isNull()
    }

    @Test
    fun `silent headphones fail initialization`() = runTest {
        val link = InMemoryLink()
        val session = MdrSession(link.app, backgroundScope)
        val client = HeadphonesClient(session, backgroundScope)
        session.start()

        assertThat(client.initialize()).isEqualTo(InitResult.NO_RESPONSE)
    }
}
