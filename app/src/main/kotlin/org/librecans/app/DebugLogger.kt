package org.librecans.app

import android.content.Context
import org.librecans.app.BuildConfig
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DebugLogger {
    private const val PREFS_NAME = "librecans_debug_prefs_encrypted"
    private const val KEY_DEBUG_MODE = "debug_mode_enabled"
    private const val LOG_FILE_NAME = "bluetooth_diagnostics.log"

    private val logBuffer = mutableListOf<String>()
    private const val MAX_BUFFER_SIZE = 1000

    private val dateFormat = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat {
            return SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
        }
    }

    var isDebugMode: Boolean = false
        private set

    private fun getEncryptedPrefs(context: Context) = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun initialize(context: Context) {
        val prefs = PrefsHelper.getEncryptedPrefs(context, PREFS_NAME)
        isDebugMode = prefs.getBoolean(KEY_DEBUG_MODE, false)
    }

    fun toggleDebugMode(context: Context, enabled: Boolean) {
        isDebugMode = enabled
        val prefs = PrefsHelper.getEncryptedPrefs(context, PREFS_NAME)
        prefs.edit().putBoolean(KEY_DEBUG_MODE, enabled).apply()
        if (!enabled) {
            clearLogs(context)
        }
    }

    @Synchronized
    fun log(context: Context, tag: String, message: String) {
        if (!BuildConfig.DEBUG || !isDebugMode) return

        val timestamp = dateFormat.get()!!.format(Date())
        val formattedEntry = "[$timestamp] [$tag] $message"

        // 1. Add to in-memory buffer
        logBuffer.add(formattedEntry)
        if (logBuffer.size > MAX_BUFFER_SIZE) {
            logBuffer.removeAt(0)
        }

        // 2. Append to local log file in app-specific storage with size pruning (max 500 KB)
        try {
            val logFile = File(context.filesDir, LOG_FILE_NAME)
            if (logFile.exists() && logFile.length() > 500 * 1024) {
                val lines = logFile.readLines()
                val retainedLines = if (lines.size > 300) lines.takeLast(300) else lines
                logFile.writeText(retainedLines.joinToString("\n") + "\n")
            }
            FileOutputStream(logFile, true).use { out ->
                out.write((formattedEntry + "\n").toByteArray())
            }
        } catch (e: Exception) {
            Log.e("DebugLogger", "Failed to write to local log file", e)
        }
    }

    @Synchronized
    fun getLogs(context: Context): List<String> {
        if (!BuildConfig.DEBUG) return emptyList()
        val logFile = File(context.filesDir, LOG_FILE_NAME)
        if (logFile.exists()) {
            try {
                return logFile.readLines()
            } catch (e: Exception) {
                Log.e("DebugLogger", "Failed to read local log file", e)
            }
        }
        return logBuffer.toList()
    }

    @Synchronized
    fun clearLogs(context: Context) {
        if (!BuildConfig.DEBUG) return
        logBuffer.clear()
        val logFile = File(context.filesDir, LOG_FILE_NAME)
        if (logFile.exists()) {
            logFile.delete()
        }
    }
}
