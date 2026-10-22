package org.librecans.app.fakes

import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.librecans.app.data.connection.ConnectException
import org.librecans.app.data.connection.DeviceTarget
import org.librecans.app.data.connection.FailureReason
import org.librecans.app.data.connection.TransportOpener
import org.librecans.app.data.settings.AppSettings
import org.librecans.app.data.settings.SettingsRepository
import org.librecans.protocol.session.ByteTransport
import org.librecans.protocol.sim.InMemoryLink
import org.librecans.protocol.sim.SimulatedHeadphones

class MainDispatcherRule(val dispatcher: TestDispatcher = StandardTestDispatcher()) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)

    override fun finished(description: Description) = Dispatchers.resetMain()
}

class FakeSettingsRepository(initial: AppSettings = AppSettings()) : SettingsRepository {
    private val state = MutableStateFlow(initial)
    override val settings: StateFlow<AppSettings> = state.asStateFlow()

    override suspend fun load(): AppSettings = state.value

    override suspend fun update(transform: (AppSettings) -> AppSettings) = state.update(transform)
}

/** Opens links to simulated WF-1000XM5 headphones, or fails like a real socket would. */
class SimulatorOpener(var failure: FailureReason? = null, var silent: Boolean = false) : TransportOpener {
    val opened = AtomicInteger()
    val devices = mutableListOf<SimulatedHeadphones>()

    override suspend fun open(target: DeviceTarget, scope: CoroutineScope, onAttempt: (Int) -> Unit): ByteTransport {
        onAttempt(1)
        failure?.let { throw ConnectException(it) }
        opened.incrementAndGet()
        val link = InMemoryLink()
        if (!silent) devices += SimulatedHeadphones(link.device).also { it.start(scope) }
        return link.app
    }
}
