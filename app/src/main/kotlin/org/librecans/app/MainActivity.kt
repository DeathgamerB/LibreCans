package org.librecans.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.activity.compose.BackHandler

enum class AppScreen {
    PAIRING,
    DASHBOARD,
    ABOUT,
    DEBUG_LOG
}

class MainActivity : ComponentActivity() {
    private val viewModel: HeadphonesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        DebugLogger.initialize(applicationContext)
        setContent {
            LibreCansTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    var currentScreen by remember { mutableStateOf(AppScreen.PAIRING) }

                    when (currentScreen) {
                        AppScreen.PAIRING -> {
                            PairingScreen(
                                viewModel = viewModel,
                                onNavigateToMain = {
                                    currentScreen = AppScreen.DASHBOARD
                                }
                            )
                        }
                        AppScreen.DASHBOARD -> {
                            MainDashboard(
                                viewModel = viewModel,
                                onNavigateToAbout = {
                                    currentScreen = AppScreen.ABOUT
                                },
                                onNavigateToPairing = {
                                    currentScreen = AppScreen.PAIRING
                                }
                            )
                        }
                        AppScreen.ABOUT -> {
                            BackHandler { currentScreen = AppScreen.DASHBOARD }
                            AboutScreen(
                                onNavigateToDebugLogs = {
                                    currentScreen = AppScreen.DEBUG_LOG
                                },
                                onBack = {
                                    currentScreen = AppScreen.DASHBOARD
                                }
                            )
                        }
                        AppScreen.DEBUG_LOG -> {
                            BackHandler { currentScreen = AppScreen.ABOUT }
                            DebugLogScreen(
                                onBack = {
                                    currentScreen = AppScreen.ABOUT
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
