package org.librecans.app.data.settings

import kotlinx.serialization.Serializable

@Serializable
enum class ThemeMode { SYSTEM, LIGHT, DARK }

@Serializable
data class SavedDevice(val address: String, val name: String)

@Serializable
data class AppSettings(
    val lastDevice: SavedDevice? = null,
    val autoConnect: Boolean = true,
    val keepConnectedInBackground: Boolean = true,
    val dynamicColor: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val diagnosticsEnabled: Boolean = false,
)
