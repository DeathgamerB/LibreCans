package org.librecans.app.ui.info

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.librecans.app.data.connection.DeviceTarget
import org.librecans.app.data.connection.HeadphonesRepository
import org.librecans.protocol.state.HeadphoneState

data class DeviceInfoUiState(val target: DeviceTarget? = null, val headphones: HeadphoneState = HeadphoneState())

class DeviceInfoViewModel(repository: HeadphonesRepository) : ViewModel() {
    val state: StateFlow<DeviceInfoUiState> =
        combine(repository.connection, repository.headphones) { connection, headphones ->
            DeviceInfoUiState(connection.target, headphones)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            DeviceInfoUiState(repository.connection.value.target, repository.headphones.value),
        )
}
