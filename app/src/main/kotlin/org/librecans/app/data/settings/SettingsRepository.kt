package org.librecans.app.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

interface SettingsRepository {
    val settings: StateFlow<AppSettings>

    /** Waits for the stored value instead of returning the initial default. */
    suspend fun load(): AppSettings

    suspend fun update(transform: (AppSettings) -> AppSettings)
}

class DataStoreSettingsRepository(
    private val store: DataStore<AppSettings>,
    scope: CoroutineScope,
) : SettingsRepository {
    override val settings: StateFlow<AppSettings> =
        store.data.stateIn(scope, SharingStarted.Eagerly, AppSettings())

    override suspend fun load(): AppSettings = store.data.first()

    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        store.updateData(transform)
    }

    companion object {
        private const val FILE = "settings.enc"

        fun create(
            context: Context,
            scope: CoroutineScope,
            cipher: SettingsCipher,
            fileName: String = FILE,
        ): DataStoreSettingsRepository {
            val store = DataStoreFactory.create(
                serializer = EncryptedJsonSerializer(AppSettings.serializer(), AppSettings(), cipher),
                scope = scope,
                produceFile = { context.dataStoreFile(fileName) },
            )
            return DataStoreSettingsRepository(store, scope)
        }
    }
}
