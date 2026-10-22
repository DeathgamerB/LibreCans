package org.librecans.app.fakes

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.librecans.app.data.bluetooth.DeviceKind
import org.librecans.app.data.connection.ConnectionState
import org.librecans.app.data.connection.DefaultHeadphonesRepository
import org.librecans.app.data.connection.DeviceTarget
import org.librecans.app.data.diagnostics.DiagnosticsLog
import org.librecans.protocol.state.HeadphoneState

/** The full state the app sees after connecting to the simulated WF-1000XM5, for screen tests. */
object SimulatedState {
    val buds = DeviceTarget("AA:BB:CC:DD:EE:01", "WF-1000XM5", DeviceKind.EARBUDS)

    val xm5: HeadphoneState by lazy {
        var state: HeadphoneState? = null
        runTest {
            val repository = DefaultHeadphonesRepository(
                backgroundScope,
                SimulatorOpener(),
                FakeSettingsRepository(),
                DiagnosticsLog(),
                batteryRefreshMillis = 60_000,
            )
            repository.connect(buds)
            repository.connection.first { it is ConnectionState.Connected }
            runCurrent()
            state = repository.headphones.value
        }
        checkNotNull(state)
    }
}
