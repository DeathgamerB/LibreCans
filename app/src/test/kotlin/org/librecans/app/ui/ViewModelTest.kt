package org.librecans.app.ui

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.librecans.app.data.bluetooth.BluetoothSnapshot
import org.librecans.app.data.bluetooth.DeviceKind
import org.librecans.app.data.bluetooth.PairedDevice
import org.librecans.app.data.connection.ConnectionState
import org.librecans.app.data.connection.DefaultHeadphonesRepository
import org.librecans.app.data.connection.DeviceTarget
import org.librecans.app.data.diagnostics.DiagnosticsLog
import org.librecans.app.data.settings.AppSettings
import org.librecans.app.data.settings.SavedDevice
import org.librecans.app.data.settings.ThemeMode
import org.librecans.app.fakes.FakeSettingsRepository
import org.librecans.app.fakes.MainDispatcherRule
import org.librecans.app.fakes.SimulatorOpener
import org.librecans.app.ui.about.AboutViewModel
import org.librecans.app.ui.dashboard.DashboardViewModel
import org.librecans.app.ui.devices.DevicesViewModel
import org.librecans.app.ui.equalizer.EqualizerViewModel
import org.librecans.app.ui.settings.DeviceSettingsUiState
import org.librecans.app.ui.settings.DeviceSettingsViewModel
import org.librecans.app.ui.settings.autoPowerOffOptions
import org.librecans.app.ui.touch.TouchControlsViewModel
import org.librecans.protocol.model.AutoPowerOff
import org.librecans.protocol.model.EqPresets
import org.librecans.protocol.model.NoiseMode
import org.librecans.protocol.model.PlaybackStatus
import org.librecans.protocol.model.SpeakToChatTimeout
import org.librecans.protocol.model.TouchFunctions
import org.librecans.protocol.model.TouchPresets
import org.librecans.protocol.state.HeadphoneState

class ViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val buds = DeviceTarget("AA:BB:CC:DD:EE:01", "WF-1000XM5", DeviceKind.EARBUDS)
    private val opener = SimulatorOpener()
    private val settings = FakeSettingsRepository()

    private suspend fun TestScope.connected(): DefaultHeadphonesRepository {
        val repository = DefaultHeadphonesRepository(backgroundScope, opener, settings, DiagnosticsLog())
        repository.connect(buds)
        repository.connection.first { it is ConnectionState.Connected }
        runCurrent()
        return repository
    }

    private val device get() = opener.devices.single().model

    @Test
    fun `dashboard switches modes and adjusts ambient sound`() = runTest(main.dispatcher) {
        val viewModel = DashboardViewModel(connected())

        viewModel.setNoiseMode(NoiseMode.AMBIENT)
        runCurrent()
        assertThat(device.ncAmbient).isTrue()

        viewModel.setAmbientLevel(1, settled = true)
        viewModel.setFocusOnVoice(true)
        runCurrent()
        assertThat(device.focusOnVoice).isTrue()
        assertThat(device.ambientLevel).isAtLeast(2)

        viewModel.setNoiseMode(NoiseMode.OFF)
        runCurrent()
        assertThat(device.ncEnabled).isFalse()
    }

    @Test
    fun `dashboard controls playback`() = runTest(main.dispatcher) {
        val repository = connected()
        val viewModel = DashboardViewModel(repository)

        viewModel.togglePlayback()
        runCurrent()
        assertThat(repository.headphones.value.playback?.status).isEqualTo(PlaybackStatus.PLAYING)

        viewModel.togglePlayback()
        viewModel.setVolume(25)
        runCurrent()
        assertThat(device.playing).isFalse()
        assertThat(device.volume).isEqualTo(25)
    }

    @Test
    fun `equalizer edits bands and resets`() = runTest(main.dispatcher) {
        val viewModel = EqualizerViewModel(connected())

        viewModel.setBand(2, 7)
        viewModel.setClearBass(-4)
        runCurrent()
        assertThat(device.eqSteps).containsExactly(6, 11, 10, 17, 9, 12).inOrder()

        viewModel.reset()
        runCurrent()
        assertThat(device.eqSteps.all { it == 10 }).isTrue()

        viewModel.selectPreset(EqPresets.VOCAL)
        runCurrent()
        assertThat(device.eqPreset).isEqualTo(EqPresets.VOCAL)
    }

    @Test
    fun `touch controls reassign and narrow the cycle`() = runTest(main.dispatcher) {
        val viewModel = TouchControlsViewModel(connected())

        viewModel.setPreset(1, TouchPresets.NO_FUNCTION)
        viewModel.setAmbientCycle(TouchFunctions.ASM_OFF)
        runCurrent()

        assertThat(device.touchPresets[1]).isEqualTo(TouchPresets.NO_FUNCTION)
        assertThat(device.ambientCycle).isEqualTo(TouchFunctions.ASM_OFF)
    }

    @Test
    fun `device settings write through`() = runTest(main.dispatcher) {
        val viewModel = DeviceSettingsViewModel(connected())

        viewModel.setSpeakToChat(true)
        viewModel.setSpeakToChatTimeout(SpeakToChatTimeout.NEVER)
        viewModel.setHeadGestures(true)
        viewModel.setAutoPowerOff(AutoPowerOff.AFTER_30_MIN)
        viewModel.setGeneralSetting(0xD1, true)
        runCurrent()

        assertThat(device.speakToChat).isTrue()
        assertThat(device.speakToChatTimeout).isEqualTo(SpeakToChatTimeout.NEVER.code)
        assertThat(device.headGestures).isTrue()
        assertThat(device.autoPowerOff).isEqualTo(AutoPowerOff.AFTER_30_MIN.code)
        assertThat(device.generalSettings[0xD1]).isTrue()
    }

    @Test
    fun `auto power off falls back to sensible options`() {
        val reported = DeviceSettingsUiState(headphones = HeadphoneState(autoPowerOffOptions = listOf(AutoPowerOff.DISABLED, AutoPowerOff.WHEN_REMOVED)))
        assertThat(autoPowerOffOptions(reported)).containsExactly(AutoPowerOff.WHEN_REMOVED, AutoPowerOff.DISABLED).inOrder()
        assertThat(autoPowerOffOptions(DeviceSettingsUiState())).doesNotContain(AutoPowerOff.WHEN_REMOVED)
    }

    @Test
    fun `devices auto connects to the last headphones that are playing`() = runTest(main.dispatcher) {
        settings.update { it.copy(lastDevice = SavedDevice(buds.address, buds.name)) }
        val repository = DefaultHeadphonesRepository(backgroundScope, opener, settings, DiagnosticsLog())
        val snapshot = BluetoothSnapshot(
            available = true,
            enabled = true,
            permitted = true,
            devices = listOf(PairedDevice(buds.address, buds.name, DeviceKind.EARBUDS, audioConnected = true)),
        )
        DevicesViewModel({ MutableStateFlow(snapshot) }, { true }, repository, settings, showDemo = false)

        val connected = repository.connection.first { it is ConnectionState.Connected }

        assertThat(connected.target?.address).isEqualTo(buds.address)
    }

    @Test
    fun `devices does not auto connect when disabled`() = runTest(main.dispatcher) {
        settings.update { it.copy(autoConnect = false) }
        val repository = DefaultHeadphonesRepository(backgroundScope, opener, settings, DiagnosticsLog())
        val snapshot = BluetoothSnapshot(true, true, true, listOf(PairedDevice(buds.address, buds.name, DeviceKind.EARBUDS, true)))
        DevicesViewModel({ MutableStateFlow(snapshot) }, { true }, repository, settings, showDemo = false)
        runCurrent()

        assertThat(repository.connection.value).isEqualTo(ConnectionState.Idle)
    }

    @Test
    fun `devices rechecks the permission after the prompt`() = runTest(main.dispatcher) {
        var granted = false
        val repository = DefaultHeadphonesRepository(backgroundScope, opener, settings, DiagnosticsLog())
        val viewModel = DevicesViewModel(
            { MutableStateFlow(BluetoothSnapshot(true, true, granted, emptyList())) },
            { granted },
            repository,
            settings,
            showDemo = true,
        )
        backgroundScope.launch { viewModel.state.collect {} }
        runCurrent()
        assertThat(viewModel.state.value.bluetooth.permitted).isFalse()

        granted = true
        viewModel.onPermissionResult()
        runCurrent()
        assertThat(viewModel.state.value.bluetooth.permitted).isTrue()
        assertThat(viewModel.state.value.showDemo).isTrue()
    }

    @Test
    fun `about updates the settings`() = runTest(main.dispatcher) {
        val viewModel = AboutViewModel(settings)

        viewModel.setThemeMode(ThemeMode.DARK)
        viewModel.setDynamicColor(false)
        viewModel.setDiagnostics(true)
        runCurrent()

        assertThat(settings.settings.value).isEqualTo(AppSettings(themeMode = ThemeMode.DARK, dynamicColor = false, diagnosticsEnabled = true))
    }
}
