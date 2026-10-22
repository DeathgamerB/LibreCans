package org.librecans.app

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.plus
import org.librecans.app.data.bluetooth.BluetoothDevicesSource
import org.librecans.app.data.bluetooth.RfcommConnector
import org.librecans.app.data.connection.DefaultHeadphonesRepository
import org.librecans.app.data.connection.DefaultTransportOpener
import org.librecans.app.data.connection.HeadphonesRepository
import org.librecans.app.data.diagnostics.DiagnosticsLog
import org.librecans.app.data.settings.DataStoreSettingsRepository
import org.librecans.app.data.settings.KeystoreSettingsCipher
import org.librecans.app.data.settings.SettingsRepository

/** Application wide singletons, created once in [LibreCansApp]. */
class AppContainer(context: Context) {
    val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val diagnostics = DiagnosticsLog()
    val settings: SettingsRepository =
        DataStoreSettingsRepository.create(context, scope + Dispatchers.IO, KeystoreSettingsCipher())
    val bluetooth = BluetoothDevicesSource(context)
    val headphones: HeadphonesRepository = DefaultHeadphonesRepository(
        scope = scope,
        opener = DefaultTransportOpener(RfcommConnector(context)),
        settings = settings,
        log = diagnostics,
    )
}
