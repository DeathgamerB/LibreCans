package org.librecans.app.ui.equalizer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.librecans.app.data.connection.HeadphonesRepository
import org.librecans.protocol.model.EqLevels
import org.librecans.protocol.model.EqPreset
import org.librecans.protocol.model.EqPresets
import org.librecans.protocol.model.Equalizer
import org.librecans.protocol.model.EqualizerMath
import org.librecans.protocol.state.HeadphoneState

data class EqualizerUiState(
    val equalizer: Equalizer? = null,
    val presets: List<EqPreset> = EqPresets.DEFAULTS,
) {
    val levels: EqLevels get() = equalizer?.takeIf { it.steps.isNotEmpty() }?.levels ?: EqLevels(0, List(5) { 0 })
    val range: IntRange get() = EqualizerMath.rangeFor(equalizer?.steps?.size ?: 6)
}

class EqualizerViewModel(private val repository: HeadphonesRepository) : ViewModel() {
    val state: StateFlow<EqualizerUiState> = repository.headphones.map(::toUiState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), toUiState(repository.headphones.value))

    fun selectPreset(id: Int) = repository.control { setEqualizerPreset(id) }

    fun setBand(index: Int, level: Int) = repository.control {
        updateEqualizerLevels { levels ->
            if (index !in levels.bands.indices) levels
            else levels.copy(bands = levels.bands.toMutableList().also { it[index] = level })
        }
    }

    fun setClearBass(level: Int) = repository.control {
        updateEqualizerLevels { levels -> if (levels.clearBass == null) levels else levels.copy(clearBass = level) }
    }

    fun reset() = repository.control {
        updateEqualizerLevels { levels -> EqLevels(levels.clearBass?.let { 0 }, levels.bands.map { 0 }) }
    }

    private fun toUiState(state: HeadphoneState) = EqualizerUiState(
        equalizer = state.equalizer,
        presets = state.equalizerCapability?.presets?.takeIf { it.isNotEmpty() } ?: EqPresets.DEFAULTS,
    )
}
