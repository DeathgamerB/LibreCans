package org.librecans.app.ui.common

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import org.librecans.app.R
import org.librecans.protocol.model.NoiseMode

@get:StringRes
val NoiseMode.label: Int
    get() = when (this) {
        NoiseMode.NOISE_CANCELLING -> R.string.noise_mode_nc
        NoiseMode.AMBIENT -> R.string.noise_mode_ambient
        NoiseMode.OFF -> R.string.noise_mode_off
    }

@get:StringRes
val NoiseMode.shortLabel: Int
    get() = when (this) {
        NoiseMode.NOISE_CANCELLING -> R.string.noise_mode_nc_short
        NoiseMode.AMBIENT -> R.string.noise_mode_ambient_short
        NoiseMode.OFF -> R.string.noise_mode_off
    }

@get:DrawableRes
val NoiseMode.icon: Int
    get() = when (this) {
        NoiseMode.NOISE_CANCELLING -> R.drawable.ic_noise_control_on
        NoiseMode.AMBIENT -> R.drawable.ic_hearing
        NoiseMode.OFF -> R.drawable.ic_noise_control_off
    }

/** The order the earbuds cycle through: NC, ambient, off. */
fun NoiseMode.next(): NoiseMode = when (this) {
    NoiseMode.NOISE_CANCELLING -> NoiseMode.AMBIENT
    NoiseMode.AMBIENT -> NoiseMode.OFF
    NoiseMode.OFF -> NoiseMode.NOISE_CANCELLING
}
