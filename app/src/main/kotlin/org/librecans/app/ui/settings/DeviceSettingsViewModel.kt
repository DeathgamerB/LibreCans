package org.librecans.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.librecans.app.data.connection.DeviceTarget
import org.librecans.app.data.connection.HeadphonesRepository
import org.librecans.protocol.model.AutoPowerOff
import org.librecans.protocol.model.ConnectionPriority
import org.librecans.protocol.model.SpeakToChatSensitivity
import org.librecans.protocol.model.SpeakToChatTimeout
import org.librecans.protocol.state.HeadphoneState

data class DeviceSettingsUiState(val target: DeviceTarget? = null, val headphones: HeadphoneState = HeadphoneState())

class DeviceSettingsViewModel(private val repository: HeadphonesRepository) : ViewModel() {
    val state: StateFlow<DeviceSettingsUiState> =
        combine(repository.connection, repository.headphones) { connection, headphones ->
            DeviceSettingsUiState(connection.target, headphones)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            DeviceSettingsUiState(repository.connection.value.target, repository.headphones.value),
        )

    fun setUpscaling(enabled: Boolean) = repository.control { setUpscaling(enabled) }

    fun setConnectionPriority(priority: ConnectionPriority) = repository.control { setConnectionPriority(priority) }

    fun setSpeakToChat(enabled: Boolean) = repository.control { setSpeakToChat(enabled) }

    fun setSpeakToChatSensitivity(sensitivity: SpeakToChatSensitivity) = repository.control {
        setSpeakToChatConfig(sensitivity, state.value.speakToChat?.timeout ?: SpeakToChatTimeout.STANDARD)
    }

    fun setSpeakToChatTimeout(timeout: SpeakToChatTimeout) = repository.control {
        setSpeakToChatConfig(state.value.speakToChat?.sensitivity ?: SpeakToChatSensitivity.AUTO, timeout)
    }

    fun setHeadGestures(enabled: Boolean) = repository.control { setHeadGestures(enabled) }

    fun setAdaptiveVolume(enabled: Boolean) = repository.control { setAdaptiveVolume(enabled) }

    fun setPauseWhenRemoved(enabled: Boolean) = repository.control { setPauseWhenRemoved(enabled) }

    fun setAutoPowerOff(value: AutoPowerOff) = repository.control { setAutoPowerOff(value) }

    fun setVoiceGuidance(enabled: Boolean) = repository.control { setVoiceGuidance(enabled) }

    fun setVoiceGuidanceVolume(volume: Int) = repository.control { setVoiceGuidanceVolume(volume) }

    fun setGeneralSetting(id: Int, enabled: Boolean) = repository.control { setGeneralSetting(id, enabled) }

    fun powerOff() {
        viewModelScope.launch { repository.powerOff() }
    }
}
