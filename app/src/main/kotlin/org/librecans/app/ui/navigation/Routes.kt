package org.librecans.app.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object DevicesRoute : NavKey

@Serializable
data object DashboardRoute : NavKey

@Serializable
data object EqualizerRoute : NavKey

@Serializable
data object TouchControlsRoute : NavKey

@Serializable
data object DeviceSettingsRoute : NavKey

@Serializable
data object DeviceInfoRoute : NavKey

@Serializable
data object AboutRoute : NavKey

@Serializable
data object DiagnosticsRoute : NavKey

/** Screens that only make sense while headphones are connected. */
val NavKey.needsConnection: Boolean
    get() = this == DashboardRoute || this == EqualizerRoute || this == TouchControlsRoute ||
        this == DeviceSettingsRoute || this == DeviceInfoRoute
