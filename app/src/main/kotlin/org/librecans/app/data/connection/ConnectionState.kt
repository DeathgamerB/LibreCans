package org.librecans.app.data.connection

import org.librecans.app.data.bluetooth.DeviceKind

data class DeviceTarget(
    val address: String,
    val name: String,
    val kind: DeviceKind,
    val isDemo: Boolean = false,
)

enum class FailureReason {
    BLUETOOTH_UNAVAILABLE,
    BLUETOOTH_OFF,
    NO_PERMISSION,
    NOT_FOUND,
    CONNECTION_FAILED,
    NO_RESPONSE,
    UNSUPPORTED_PROTOCOL,
    CONNECTION_LOST,
}

class ConnectException(val reason: FailureReason, cause: Throwable? = null) : Exception(reason.name, cause)

sealed interface ConnectionState {
    val target: DeviceTarget? get() = null

    data object Idle : ConnectionState

    data class Connecting(override val target: DeviceTarget, val attempt: Int = 1) : ConnectionState

    data class Connected(override val target: DeviceTarget) : ConnectionState

    data class Failed(override val target: DeviceTarget?, val reason: FailureReason) : ConnectionState
}

val ConnectionState.isActive: Boolean
    get() = this is ConnectionState.Connecting || this is ConnectionState.Connected
