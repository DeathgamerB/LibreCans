package org.librecans.app.ui.about

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.librecans.app.data.diagnostics.DiagnosticsLog
import org.librecans.app.data.diagnostics.LogEntry
import org.librecans.app.data.settings.AppSettings
import org.librecans.app.data.settings.SettingsRepository
import org.librecans.app.data.settings.ThemeMode

class AboutViewModel(private val settings: SettingsRepository) : ViewModel() {
    val state: StateFlow<AppSettings> = settings.settings

    fun setDynamicColor(enabled: Boolean) = update { it.copy(dynamicColor = enabled) }

    fun setThemeMode(mode: ThemeMode) = update { it.copy(themeMode = mode) }

    fun setAutoConnect(enabled: Boolean) = update { it.copy(autoConnect = enabled) }

    fun setKeepConnected(enabled: Boolean) = update { it.copy(keepConnectedInBackground = enabled) }

    fun setDiagnostics(enabled: Boolean) = update { it.copy(diagnosticsEnabled = enabled) }

    private fun update(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { settings.update(transform) }
    }
}

class DiagnosticsViewModel(private val log: DiagnosticsLog) : ViewModel() {
    val entries: StateFlow<List<LogEntry>> = log.entries

    val enabled: Boolean get() = log.enabled

    fun clear() = log.clear()

    fun export(): String = log.export()
}
