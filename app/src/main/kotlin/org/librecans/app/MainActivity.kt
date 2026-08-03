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
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import kotlinx.coroutines.CancellationException

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
                            val backProgress = remember { Animatable(0f) }
                            PredictiveBackHandler { progress ->
                                try {
                                    progress.collect { backEvent ->
                                        backProgress.snapTo(backEvent.progress)
                                    }
                                    currentScreen = AppScreen.DASHBOARD
                                } catch (e: CancellationException) {
                                    backProgress.animateTo(0f)
                                }
                            }
                            val scale = 1f - (backProgress.value * 0.1f)
                            val alpha = 1f - (backProgress.value * 0.5f)

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .scale(scale)
                                    .alpha(alpha)
                            ) {
                                AboutScreen(
                                    onNavigateToDebugLogs = {
                                        currentScreen = AppScreen.DEBUG_LOG
                                    },
                                    onBack = {
                                        currentScreen = AppScreen.DASHBOARD
                                    }
                                )
                            }
                        }
                        AppScreen.DEBUG_LOG -> {
                            val backProgress = remember { Animatable(0f) }
                            PredictiveBackHandler { progress ->
                                try {
                                    progress.collect { backEvent ->
                                        backProgress.snapTo(backEvent.progress)
                                    }
                                    currentScreen = AppScreen.ABOUT
                                } catch (e: CancellationException) {
                                    backProgress.animateTo(0f)
                                }
                            }
                            val scale = 1f - (backProgress.value * 0.1f)
                            val alpha = 1f - (backProgress.value * 0.5f)

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .scale(scale)
                                    .alpha(alpha)
                            ) {
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
}
