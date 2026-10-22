package org.librecans.app.data

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.librecans.app.data.bluetooth.DeviceKind
import org.librecans.app.data.connection.ConnectionState
import org.librecans.app.data.connection.DefaultHeadphonesRepository
import org.librecans.app.data.connection.DemoHeadphones
import org.librecans.app.data.connection.DeviceTarget
import org.librecans.app.data.connection.FailureReason
import org.librecans.app.data.connection.TransportOpener
import org.librecans.app.data.diagnostics.DiagnosticsLog
import org.librecans.app.fakes.FakeSettingsRepository
import org.librecans.app.fakes.SimulatorOpener
import org.librecans.protocol.model.NoiseMode
import org.librecans.protocol.session.ByteTransport
import org.librecans.protocol.sim.InMemoryLink

class HeadphonesRepositoryTest {
    private val buds = DeviceTarget("AA:BB:CC:DD:EE:01", "WF-1000XM5", DeviceKind.EARBUDS)
    private val other = DeviceTarget("AA:BB:CC:DD:EE:02", "WH-1000XM5", DeviceKind.HEADPHONES)
    private val settings = FakeSettingsRepository()

    private fun TestScope.repository(opener: TransportOpener = SimulatorOpener()) =
        DefaultHeadphonesRepository(backgroundScope, opener, settings, DiagnosticsLog(), batteryRefreshMillis = 60_000)

    @Test
    fun `connecting initializes the headphones and remembers them`() = runTest {
        val repository = repository()

        repository.connect(buds)
        assertThat(repository.connection.value).isEqualTo(ConnectionState.Connecting(buds))
        val connected = repository.connection.first { it is ConnectionState.Connected }
        runCurrent()

        assertThat(connected.target).isEqualTo(buds)
        assertThat(repository.headphones.value.identity.modelName).isEqualTo("WF-1000XM5")
        assertThat(repository.headphones.value.batteries.case?.percent).isEqualTo(53)
        assertThat(settings.settings.value.lastDevice?.address).isEqualTo(buds.address)
    }

    @Test
    fun `connecting twice to the same headphones opens one link`() = runTest {
        val opener = SimulatorOpener()
        val repository = repository(opener)

        repository.connect(buds)
        repository.connect(buds)
        repository.connection.first { it is ConnectionState.Connected }

        assertThat(opener.opened.get()).isEqualTo(1)
    }

    @Test
    fun `switching headphones replaces the previous connection`() = runTest {
        val repository = repository()

        repository.connect(buds)
        repository.connection.first { it is ConnectionState.Connected }
        repository.connect(other)
        val connected = repository.connection.first { it is ConnectionState.Connected && it.target == other }

        assertThat(connected.target).isEqualTo(other)
    }

    @Test
    fun `a cancelled attempt never overwrites the newer one`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val slow = TransportOpener { target, scope, attempt ->
            if (target == buds) gate.await()
            SimulatorOpener().open(target, scope, attempt)
        }
        val repository = repository(slow)

        repository.connect(buds)
        runCurrent()
        repository.connect(other)
        repository.connection.first { it is ConnectionState.Connected }
        gate.complete(Unit)
        runCurrent()

        assertThat(repository.connection.value.target).isEqualTo(other)
    }

    @Test
    fun `disconnecting returns to idle and clears the state`() = runTest {
        val repository = repository()
        repository.connect(buds)
        repository.connection.first { it is ConnectionState.Connected }

        repository.disconnect()
        runCurrent()

        assertThat(repository.connection.value).isEqualTo(ConnectionState.Idle)
        assertThat(repository.headphones.value.identity.modelName).isNull()
    }

    @Test
    fun `controls reach the headphones while connected`() = runTest {
        val opener = SimulatorOpener()
        val repository = repository(opener)
        repository.connect(buds)
        repository.connection.first { it is ConnectionState.Connected }

        repository.control { setNoiseControl(NoiseMode.AMBIENT, 9, focusOnVoice = false) }
        runCurrent()

        assertThat(opener.devices.single().model.ncAmbient).isTrue()
        assertThat(opener.devices.single().model.ambientLevel).isEqualTo(9)
    }

    @Test
    fun `losing the link is reported`() = runTest {
        val opener = SimulatorOpener()
        val repository = repository(opener)
        repository.connect(buds)
        repository.connection.first { it is ConnectionState.Connected }

        repository.powerOff()
        val failed = repository.connection.first { it is ConnectionState.Failed } as ConnectionState.Failed

        assertThat(failed.reason).isEqualTo(FailureReason.CONNECTION_LOST)
        assertThat(failed.target).isEqualTo(buds)
    }

    @Test
    fun `socket failures are reported with their reason`() = runTest {
        val repository = repository(SimulatorOpener(failure = FailureReason.BLUETOOTH_OFF))

        repository.connect(buds)
        val failed = repository.connection.first { it is ConnectionState.Failed } as ConnectionState.Failed

        assertThat(failed.reason).isEqualTo(FailureReason.BLUETOOTH_OFF)
    }

    @Test
    fun `silent headphones time out`() = runTest {
        val repository = repository(SimulatorOpener(silent = true))

        repository.connect(buds)
        advanceTimeBy(30_000)
        val failed = repository.connection.first { it is ConnectionState.Failed } as ConnectionState.Failed

        assertThat(failed.reason).isEqualTo(FailureReason.NO_RESPONSE)
    }

    @Test
    fun `cancelling closes the transport so blocked reads end`() = runTest {
        val closed = CompletableDeferred<Unit>()
        val opener = TransportOpener { _, _, _ ->
            val link = InMemoryLink()
            object : ByteTransport by link.app {
                override fun close() {
                    link.app.close()
                    closed.complete(Unit)
                }
            }
        }
        val repository = repository(opener)

        repository.connect(buds)
        runCurrent()
        repository.disconnect()
        runCurrent()

        assertThat(closed.isCompleted).isTrue()
    }

    @Test
    fun `demo headphones are not remembered`() = runTest {
        val repository = repository(TransportOpener { _, scope: CoroutineScope, _ -> DemoHeadphones.open(scope) })

        repository.connect(DemoHeadphones.target)
        repository.connection.first { it is ConnectionState.Connected }
        runCurrent()

        assertThat(settings.settings.value.lastDevice).isNull()
        repository.disconnect()
    }
}
