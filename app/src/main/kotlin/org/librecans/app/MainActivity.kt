package org.librecans.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

enum class AppScreen {
    PAIRING,
    DASHBOARD,
    ABOUT
}

class MainActivity : ComponentActivity() {
    private val viewModel: HeadphonesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                            AboutScreen(
                                onBack = {
                                    currentScreen = AppScreen.DASHBOARD
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
