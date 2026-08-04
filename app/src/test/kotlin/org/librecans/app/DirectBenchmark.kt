package org.librecans.app

import org.junit.Test
import kotlin.system.measureTimeMillis
import android.content.Context
import android.content.SharedPreferences

class DirectBenchmark {

    class DummyEditor : SharedPreferences.Editor {
        override fun putString(key: String, value: String?): SharedPreferences.Editor = this
        override fun putStringSet(key: String, values: Set<String>?): SharedPreferences.Editor = this
        override fun putInt(key: String, value: Int): SharedPreferences.Editor = this
        override fun putLong(key: String, value: Long): SharedPreferences.Editor = this
        override fun putFloat(key: String, value: Float): SharedPreferences.Editor = this
        override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor = this
        override fun remove(key: String): SharedPreferences.Editor = this
        override fun clear(): SharedPreferences.Editor = this
        override fun commit(): Boolean = true
        override fun apply() {}
    }

    class DummyPrefs : SharedPreferences {
        override fun getAll(): Map<String, *> = emptyMap<String, Any>()
        override fun getString(key: String, defValue: String?): String? = defValue
        override fun getStringSet(key: String, defValues: Set<String>?): Set<String>? = defValues
        override fun getInt(key: String, defValue: Int): Int = defValue
        override fun getLong(key: String, defValue: Long): Long = defValue
        override fun getFloat(key: String, defValue: Float): Float = defValue
        override fun getBoolean(key: String, defValue: Boolean): Boolean = defValue
        override fun contains(key: String): Boolean = false
        override fun edit(): SharedPreferences.Editor = DummyEditor()
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {}
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {}
    }

    @Test
    fun simulateRepositoryCalls() {
        // Without mocked android framework, just test the core logic cost manually
        // We know getSharedPreferences does I/O on real Android
        // Simulated I/O delay
        fun getSharedPreferencesSimulated(): SharedPreferences {
            Thread.sleep(1) // Simulate Context I/O cost
            return DummyPrefs()
        }

        val cachedPrefs = DummyPrefs()

        val timeUncached = measureTimeMillis {
            for (i in 1..100) {
                val prefs = getSharedPreferencesSimulated()
                prefs.edit().putInt("key", i).apply()
            }
        }

        val timeCached = measureTimeMillis {
            for (i in 1..100) {
                val prefs = cachedPrefs
                prefs.edit().putInt("key", i).apply()
            }
        }

        println("UNCACHED (simulated context call): $timeUncached ms")
        println("CACHED: $timeCached ms")
    }
}
