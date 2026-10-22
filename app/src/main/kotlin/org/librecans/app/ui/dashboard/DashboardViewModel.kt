package org.librecans.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.librecans.app.data.connection.ConnectionState
import org.librecans.app.data.connection.DemoHeadphones
import org.librecans.app.data.connection.HeadphonesRepository
import org.librecans.protocol.model.NoiseControl
import org.librecans.protocol.model.NoiseMode
import org.librecans.protocol.model.PlaybackCommand
import org.librecans.protocol.model.PlaybackStatus
import org.librecans.protocol.state.HeadphoneState

data class DashboardUiState(
    val connection: ConnectionState = ConnectionState.Idle,
    val headphones: HeadphoneState = HeadphoneState(),
)

class DashboardViewModel(private val repository: HeadphonesRepository) : ViewModel() {
    val state: StateFlow<DashboardUiState> = combine(repository.connection, repository.headphones, ::DashboardUiState)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            DashboardUiState(repository.connection.value, repository.headphones.value),
        )

    fun setNoiseMode(mode: NoiseMode) = repository.control {
        updateNoiseControl { current ->
            val level = current.ambientLevel.takeIf { it >= NoiseControl.MIN_AMBIENT_LEVEL } ?: NoiseControl.MAX_AMBIENT_LEVEL
            current.copy(mode = mode, ambientLevel = level)
        }
    }

    fun setAmbientLevel(level: Int, settled: Boolean) = repository.control {
        updateNoiseControl(settled) { current ->
            current.copy(
                mode = NoiseMode.AMBIENT,
                ambientLevel = level,
                focusOnVoice = current.focusOnVoice && level >= NoiseControl.MIN_FOCUS_ON_VOICE_LEVEL,
            )
        }
    }

    fun setFocusOnVoice(enabled: Boolean) = repository.control {
        updateNoiseControl { current ->
            val level = if (enabled) current.ambientLevel.coerceAtLeast(NoiseControl.MIN_FOCUS_ON_VOICE_LEVEL) else current.ambientLevel
            current.copy(mode = NoiseMode.AMBIENT, ambientLevel = level, focusOnVoice = enabled)
        }
    }

    fun setVolume(volume: Int) = repository.control { setVolume(volume) }

    fun togglePlayback() = repository.control {
        val playing = state.value.playback?.status == PlaybackStatus.PLAYING
        playback(if (playing) PlaybackCommand.PAUSE else PlaybackCommand.PLAY)
    }

    fun skip(next: Boolean) = repository.control {
        playback(if (next) PlaybackCommand.NEXT_TRACK else PlaybackCommand.PREVIOUS_TRACK)
    }

    fun simulateTap() = DemoHeadphones.simulateTap()

    fun disconnect() = repository.disconnect()
}
