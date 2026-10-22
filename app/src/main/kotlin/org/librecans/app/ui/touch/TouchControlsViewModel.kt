package org.librecans.app.ui.touch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.librecans.app.data.connection.HeadphonesRepository
import org.librecans.protocol.model.GestureBinding
import org.librecans.protocol.model.PresetOption
import org.librecans.protocol.model.TouchActions as A
import org.librecans.protocol.model.TouchControls
import org.librecans.protocol.model.TouchFunctions as F
import org.librecans.protocol.model.TouchPresets as P

data class TouchSide(val key: Int, val preset: Int, val options: List<PresetOption>) {
    val gestures: List<GestureBinding> get() = options.firstOrNull { it.preset == preset }?.gestures.orEmpty()
}

data class TouchControlsUiState(
    val sides: List<TouchSide> = emptyList(),
    val ambientCycle: Int? = null,
    val cycleOptions: List<Int> = F.AMBIENT_CYCLES,
) {
    val available: Boolean get() = sides.isNotEmpty()
    val showCycle: Boolean get() = ambientCycle != null && sides.any { P.controlsAmbientSound(it.preset) }
}

class TouchControlsViewModel(private val repository: HeadphonesRepository) : ViewModel() {
    val state: StateFlow<TouchControlsUiState> = repository.headphones.map { TouchModel.uiState(it.touchControls) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TouchModel.uiState(repository.headphones.value.touchControls))

    fun setPreset(key: Int, preset: Int) = repository.control { setTouchPreset(key, preset) }

    fun setAmbientCycle(function: Int) = repository.control { setAmbientCycle(function) }
}

/** Builds the per earbud choices, falling back to the documented WF-1000XM5 behaviour. */
object TouchModel {
    val FALLBACK_OPTIONS: List<PresetOption> = listOf(
        PresetOption(P.AMBIENT_SOUND_CONTROL, listOf(GestureBinding(A.SINGLE_TAP, F.NC_ASM_OFF), GestureBinding(A.TAP_AND_HOLD, F.QUICK_ATTENTION))),
        PresetOption(
            P.AMBIENT_SOUND_CONTROL_QUICK_ACCESS,
            listOf(
                GestureBinding(A.SINGLE_TAP, F.NC_ASM_OFF),
                GestureBinding(A.DOUBLE_TAP, F.QUICK_ACCESS_1),
                GestureBinding(A.TRIPLE_TAP, F.QUICK_ACCESS_2),
                GestureBinding(A.TAP_AND_HOLD, F.QUICK_ATTENTION),
            ),
        ),
        PresetOption(
            P.PLAYBACK_CONTROL,
            listOf(
                GestureBinding(A.SINGLE_TAP, F.PLAY_PAUSE),
                GestureBinding(A.DOUBLE_TAP, F.NEXT_TRACK),
                GestureBinding(A.TRIPLE_TAP, F.PREVIOUS_TRACK),
                GestureBinding(A.TAP_AND_HOLD, F.VOICE_RECOGNITION),
            ),
        ),
        PresetOption(P.VOLUME_CONTROL, listOf(GestureBinding(A.SINGLE_TAP, F.VOLUME_UP), GestureBinding(A.TAP_AND_HOLD, F.VOLUME_DOWN))),
    )

    private val NOT_ASSIGNED = PresetOption(P.NO_FUNCTION, emptyList())

    fun uiState(touch: TouchControls?): TouchControlsUiState {
        if (touch == null || touch.presets.isEmpty()) return TouchControlsUiState()
        val cycle = touch.ambientCycle()
        val sides = touch.presets.mapIndexed { key, preset ->
            TouchSide(key, preset, optionsFor(touch, key, preset).map { it.display(cycle) })
        }
        return TouchControlsUiState(sides = sides, ambientCycle = cycle)
    }

    /** Gestures in tap order, with the ambient sound tap showing the cycle actually configured. */
    private fun PresetOption.display(cycle: Int?): PresetOption = copy(
        gestures = gestures.map {
            if (cycle != null && P.controlsAmbientSound(preset) && it.action == A.SINGLE_TAP) it.copy(function = cycle) else it
        }.sortedBy { it.action },
    )

    fun optionsFor(touch: TouchControls, key: Int, current: Int): List<PresetOption> {
        val reported = touch.keys.firstOrNull { it.key == key }?.options.orEmpty()
        val base = reported.ifEmpty { FALLBACK_OPTIONS.filter { it.preset != P.AMBIENT_SOUND_CONTROL_QUICK_ACCESS || current == it.preset } }
        val withCurrent = if (base.any { it.preset == current } || current == P.NO_FUNCTION) {
            base
        } else {
            base + (FALLBACK_OPTIONS.firstOrNull { it.preset == current } ?: PresetOption(current, emptyList()))
        }
        return withCurrent.filterNot { it.preset == P.NO_FUNCTION } + NOT_ASSIGNED
    }
}
