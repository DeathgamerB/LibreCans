package org.librecans.app.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Haptic feedback by meaning, so every kind of action has its own feel. The system maps each type
 * to the device's tuned effect and honours the user's touch feedback setting.
 */
@Immutable
class Haptics(private val feedback: HapticFeedback) {
    /** Opening a screen, a sheet or a list row. */
    fun tap() = perform(HapticFeedbackType.ContextClick)

    /** Small transport keys such as play, skip or a simulated earbud tap. */
    fun key() = perform(HapticFeedbackType.KeyboardTap)

    /** Picking one of several options. */
    fun select() = perform(HapticFeedbackType.SegmentTick)

    /** Each step a slider passes while dragged. */
    fun step() = perform(HapticFeedbackType.SegmentFrequentTick)

    /** Letting go of a slider. */
    fun release() = perform(HapticFeedbackType.GestureEnd)

    fun toggle(on: Boolean) = perform(if (on) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)

    /** Something worked: connected, applied, reset. */
    fun confirm() = perform(HapticFeedbackType.Confirm)

    /** Something failed. */
    fun reject() = perform(HapticFeedbackType.Reject)

    /** Ending something: disconnecting, powering off, clearing. */
    fun heavy() = perform(HapticFeedbackType.LongPress)

    private fun perform(type: HapticFeedbackType) = feedback.performHapticFeedback(type)
}

@Composable
fun rememberHaptics(): Haptics {
    val feedback = LocalHapticFeedback.current
    return remember(feedback) { Haptics(feedback) }
}
