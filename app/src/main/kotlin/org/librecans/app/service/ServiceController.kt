package org.librecans.app.service

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.librecans.app.data.connection.ConnectionState
import org.librecans.app.data.connection.FailureReason
import org.librecans.app.data.connection.HeadphonesRepository
import org.librecans.app.data.settings.SettingsRepository

/** Keeps [ConnectionService] running while there is a connection worth holding on to. */
class ServiceController(
    private val context: Context,
    private val repository: HeadphonesRepository,
    private val settings: SettingsRepository,
) {
    fun start(scope: CoroutineScope) {
        scope.launch {
            combine(repository.connection, settings.settings) { connection, prefs ->
                prefs.keepConnectedInBackground && shouldRun(connection)
            }.distinctUntilChanged().collect { run -> if (run) startService() }
        }
    }

    private fun startService() {
        runCatching { ContextCompat.startForegroundService(context, Intent(context, ConnectionService::class.java)) }
    }

    companion object {
        /** A lost link keeps the service alive so the headphones reconnect when they come back. */
        fun shouldRun(state: ConnectionState): Boolean = when (state) {
            is ConnectionState.Connecting, is ConnectionState.Connected -> state.target?.isDemo == false
            is ConnectionState.Failed -> state.reason == FailureReason.CONNECTION_LOST && state.target?.isDemo == false
            ConnectionState.Idle -> false
        }
    }
}
