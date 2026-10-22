package org.librecans.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.librecans.app.ui.navigation.AppNavigation
import org.librecans.app.ui.theme.LibreCansTheme
import org.librecans.app.ui.theme.isDark

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as LibreCansApp).container

        setContent {
            val settings by container.settings.settings.collectAsStateWithLifecycle()
            val dark = settings.themeMode.isDark()
            DisposableEffect(dark) {
                val style = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }
            LibreCansTheme(themeMode = settings.themeMode, dynamicColor = settings.dynamicColor) {
                AppNavigation(container.headphones.connection)
            }
        }
    }
}
