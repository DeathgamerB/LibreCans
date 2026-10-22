package org.librecans.app

import android.app.Application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.librecans.app.data.settings.LegacyCleanup
import org.librecans.app.service.ServiceController

class LibreCansApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.scope.launch(Dispatchers.IO) { LegacyCleanup.run(this@LibreCansApp) }
        container.scope.launch {
            container.settings.settings.map { it.diagnosticsEnabled }.distinctUntilChanged()
                .collect { container.diagnostics.enabled = it }
        }
        ServiceController(this, container.headphones, container.settings).start(container.scope)
    }
}
