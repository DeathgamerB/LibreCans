package org.librecans.app.data.connection

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import org.librecans.app.data.bluetooth.DeviceKind
import org.librecans.app.data.bluetooth.RfcommConnector
import org.librecans.protocol.session.ByteTransport
import org.librecans.protocol.sim.InMemoryLink
import org.librecans.protocol.sim.SimulatedHeadphones

fun interface TransportOpener {
    /** Opens a byte stream to [target]; [scope] lives as long as the connection. */
    suspend fun open(target: DeviceTarget, scope: CoroutineScope, onAttempt: (Int) -> Unit): ByteTransport
}

class DefaultTransportOpener(private val rfcomm: RfcommConnector) : TransportOpener {
    override suspend fun open(target: DeviceTarget, scope: CoroutineScope, onAttempt: (Int) -> Unit): ByteTransport =
        if (target.isDemo) DemoHeadphones.open(scope) else rfcomm.open(target.address, onAttempt)
}

/** A simulated WF-1000XM5 for trying the app on an emulator or without headphones. */
object DemoHeadphones {
    val target = DeviceTarget(address = "00:00:00:00:00:00", name = "WF-1000XM5", kind = DeviceKind.EARBUDS, isDemo = true)

    @Volatile
    private var active: Pair<SimulatedHeadphones, CoroutineScope>? = null

    fun open(scope: CoroutineScope): ByteTransport {
        val link = InMemoryLink()
        val device = SimulatedHeadphones(link.device).apply {
            start(scope)
            startBatteryDrain(scope)
        }
        active = device to scope
        scope.coroutineContext.job.invokeOnCompletion { if (active?.first === device) active = null }
        return link.app
    }

    /** Taps the simulated earbud, which then notifies the app like real headphones would. */
    fun simulateTap() {
        val (device, scope) = active ?: return
        scope.launch { device.simulateAmbientTap() }
    }
}
