package org.librecans.app.data.settings

import android.content.Context
import java.io.File
import java.security.KeyStore

/** Removes files and keys left behind by LibreCans 1.x, which stored them differently. */
object LegacyCleanup {
    private val PREFERENCE_FILES = listOf("librecans_ambient_prefs", "librecans_debug_prefs")
    private const val LOG_FILE = "bluetooth_diagnostics.log"
    private const val LEGACY_KEY_ALIAS = "_androidx_security_master_key_"

    fun run(context: Context) {
        PREFERENCE_FILES.forEach { name ->
            if (File(context.applicationInfo.dataDir, "shared_prefs/$name.xml").exists()) {
                context.deleteSharedPreferences(name)
            }
        }
        File(context.filesDir, LOG_FILE).delete()
        runCatching {
            KeyStore.getInstance("AndroidKeyStore").apply {
                load(null)
                if (containsAlias(LEGACY_KEY_ALIAS)) deleteEntry(LEGACY_KEY_ALIAS)
            }
        }
    }
}
