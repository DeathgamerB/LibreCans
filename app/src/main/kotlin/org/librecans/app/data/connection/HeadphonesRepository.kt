package org.librecans.app.data.connection

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.librecans.app.data.diagnostics.DiagnosticsLog
import org.librecans.app.data.settings.SavedDevice
import org.librecans.app.data.settings.SettingsRepository
import org.librecans.protocol.client.HeadphonesClient
import org.librecans.protocol.client.InitResult
import org.librecans.protocol.session.ByteTransport
import org.librecans.protocol.session.MdrSession
import org.librecans.protocol.session.SessionConfig
import org.librecans.protocol.state.HeadphoneState

interface HeadphonesRepository {
    val connection: StateFlow<ConnectionState>
    val headphones: StateFlow<HeadphoneState>

    fun connect(target: DeviceTarget)

    fun disconnect()

    /** Runs [action] against the connected headphones; ignored while disconnected. */
    fun control(action: HeadphonesClient.() -> Unit)

    suspend fun powerOff(): Boolean
}

/**
 * Owns at most one connection at a time. Every connect bumps a generation counter so a stale
 * attempt that is still unwinding can never overwrite the state of a newer one.
 */
class DefaultHeadphonesRepository(
    private val scope: CoroutineScope,
    private val opener: TransportOpener,
    private val settings: SettingsRepository,
    private val log: DiagnosticsLog,
    private val sessionConfig: SessionConfig = SessionConfig(),
    private val batteryRefreshMillis: Long = 60_000,
) : HeadphonesRepository {
    private val _connection = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    override val connection: StateFlow<ConnectionState> = _connection.asStateFlow()

    private val _headphones = MutableStateFlow(HeadphoneState())
    override val headphones: StateFlow<HeadphoneState> = _headphones.asStateFlow()

    private val lock = Any()
    private var generation = 0
    private var job: Job? = null

    @Volatile
    private var client: HeadphonesClient? = null

    override fun connect(target: DeviceTarget) {
        synchronized(lock) {
            val current = _connection.value
            if (current.isActive && current.target?.address == target.address) return
            job?.cancel()
            val id = ++generation
            client = null
            _headphones.value = HeadphoneState()
            _connection.value = ConnectionState.Connecting(target)
            log.event("Connecting to ${target.name}")
            job = scope.launch { run(id, target) }
        }
    }

    override fun disconnect() {
        synchronized(lock) {
            generation++
            job?.cancel()
            job = null
            client = null
            _headphones.value = HeadphoneState()
            _connection.value = ConnectionState.Idle
        }
        log.event("Disconnected by user")
    }

    override fun control(action: HeadphonesClient.() -> Unit) {
        client?.action()
    }

    override suspend fun powerOff(): Boolean = client?.powerOff() ?: false

    /** A blocking socket read only ends once the socket closes, so cleanup closes it first. */
    private suspend fun run(id: Int, target: DeviceTarget) = coroutineScope {
        var transport: ByteTransport? = null
        try {
            transport = try {
                opener.open(target, this) { attempt -> publish(id, ConnectionState.Connecting(target, attempt)) }
            } catch (e: ConnectException) {
                fail(id, target, e.reason)
                return@coroutineScope
            }
            val session = MdrSession(transport, this, sessionConfig, log)
            val headphones = HeadphonesClient(session, this)
            launch { headphones.state.collect { if (isCurrent(id)) _headphones.value = it } }
            session.start()

            when (headphones.initialize()) {
                InitResult.READY -> Unit
                InitResult.NO_RESPONSE -> return@coroutineScope fail(id, target, FailureReason.NO_RESPONSE)
                InitResult.UNSUPPORTED_PROTOCOL -> return@coroutineScope fail(id, target, FailureReason.UNSUPPORTED_PROTOCOL)
            }
            synchronized(lock) {
                if (!isCurrent(id)) return@coroutineScope
                client = headphones
                _connection.value = ConnectionState.Connected(target)
            }
            log.event("Connected to ${target.name}")
            if (!target.isDemo) settings.update { it.copy(lastDevice = SavedDevice(target.address, target.name)) }
            launch {
                while (isActive) {
                    delay(batteryRefreshMillis)
                    headphones.refreshBattery()
                }
            }

            val cause = session.closed.await()
            log.event("Connection closed" + (cause?.message?.let { ": $it" } ?: ""))
            fail(id, target, FailureReason.CONNECTION_LOST)
        } finally {
            transport?.close()
            coroutineContext.cancelChildren()
        }
    }

    private fun fail(id: Int, target: DeviceTarget, reason: FailureReason) {
        synchronized(lock) {
            if (!isCurrent(id)) return
            client = null
            _headphones.value = HeadphoneState()
            _connection.value = ConnectionState.Failed(target, reason)
        }
        log.event("Connection failed: $reason")
    }

    private fun publish(id: Int, state: ConnectionState) = synchronized(lock) {
        if (isCurrent(id)) _connection.value = state
    }

    private fun isCurrent(id: Int) = id == generation
}
