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
import androidx.compose.ui.unit.IntOffset
import androidx.activity.BackEventCompat
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

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
                    var targetBackScreen by remember { mutableStateOf<AppScreen?>(null) }
                    var backProgress by remember { mutableFloatStateOf(0f) }
                    var swipeEdge by remember { mutableIntStateOf(BackEventCompat.EDGE_LEFT) }

                    // State to capture the screen we are leaving during a gesture
                    var gestureExitScreen by remember { mutableStateOf<AppScreen?>(null) }

                    // Mapping of screen to its logical "previous" screen for back navigation
                    fun getPreviousScreen(screen: AppScreen): AppScreen? = when (screen) {
                        AppScreen.ABOUT -> AppScreen.DASHBOARD
                        AppScreen.DEBUG_LOG -> AppScreen.ABOUT
                        else -> null
                    }

                    // Handle predictive back
                    val previousScreen = getPreviousScreen(currentScreen)
                    if (previousScreen != null) {
                        PredictiveBackHandler { progress ->
                            try {
                                targetBackScreen = previousScreen
                                gestureExitScreen = null
                                progress.collect { backEvent ->
                                    backProgress = backEvent.progress
                                    swipeEdge = backEvent.swipeEdge
                                }
                                // Commit
                                gestureExitScreen = currentScreen
                                currentScreen = previousScreen
                            } catch (e: CancellationException) {
                                // Cancelled
                                gestureExitScreen = null
                            } finally {
                                backProgress = 0f
                            }
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        // Background screen (destination of back swipe)
                        if (targetBackScreen != null) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                // AOSP reference for background: starts slightly scaled down and dimmed, animating up as the gesture progresses
                                val animatedBgProgress by animateFloatAsState(targetValue = backProgress, label = "bgAnim")
                                val bgScale = 0.9f + (animatedBgProgress * 0.1f)
                                val bgAlpha = 0.5f + (animatedBgProgress * 0.5f)
                                Box(modifier = Modifier
                                    .fillMaxSize()
                                    .scale(bgScale)
                                    .alpha(bgAlpha)) {
                                    RenderScreen(
                                        screen = targetBackScreen!!,
                                        viewModel = viewModel,
                                        onNavigate = { currentScreen = it }
                                    )
                                }
                            }
                        }

                        // Foreground screen (current)
                        AnimatedContent(
                            targetState = currentScreen,
                            transitionSpec = {
                                if (gestureExitScreen == initialState) {
                                    // Give exiting screen a higher zIndex (keep destination at targetContentZIndex = -1f so it stays behind)
                                    if (swipeEdge == BackEventCompat.EDGE_LEFT) {
                                        (EnterTransition.None togetherWith slideOutHorizontally(animationSpec = tween(300)) { width -> width } + fadeOut()).apply { targetContentZIndex = -1f }
                                    } else {
                                        (EnterTransition.None togetherWith slideOutHorizontally(animationSpec = tween(300)) { width -> -width } + fadeOut()).apply { targetContentZIndex = -1f }
                                    }
                                } else if (targetState == AppScreen.PAIRING || initialState == AppScreen.PAIRING) {
                                    fadeIn() togetherWith fadeOut()
                                } else if (targetState.ordinal > initialState.ordinal) {
                                    // Moving deeper: slide in from right
                                    slideInHorizontally { width -> width } + fadeIn() togetherWith slideOutHorizontally { width -> -width / 3 } + fadeOut()
                                } else {
                                    // Moving back normally (not gesture): slide in from left
                                    slideInHorizontally { width -> -width / 3 } + fadeIn() togetherWith slideOutHorizontally { width -> width } + fadeOut()
                                }
                            },
                            label = "Screen Transition"
                        ) { screen ->

                            val isGestureActive = screen == currentScreen && targetBackScreen != null
                            // If this screen is actively exiting due to a committed gesture, freeze its progress at its last known value (so it doesn't unshrink)
                            val isExiting = screen == gestureExitScreen
                            val currentProgress = if (isExiting) 1f else if (isGestureActive) backProgress else 0f
                            val animatedProgress by animateFloatAsState(targetValue = currentProgress, label = "backProgressAnim", finishedListener = { if (it == 0f) targetBackScreen = null })

                            // Apply AOSP Material 3 predictive back styling
                            // Scale down to 90%, add 32dp corner radius, and shift to the edge
                            val scale = 1f - (animatedProgress * 0.1f)
                            val cornerRadius = (animatedProgress * 32).dp
                            val xOffset = if (swipeEdge == BackEventCompat.EDGE_LEFT) (animatedProgress * 150).roundToInt() else -(animatedProgress * 150).roundToInt()

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .offset { IntOffset(xOffset, 0) }
                                    .scale(scale)
                                    .clip(RoundedCornerShape(cornerRadius))
                            ) {
                                RenderScreen(
                                    screen = screen,
                                    viewModel = viewModel,
                                    onNavigate = { currentScreen = it }
                                )
                            }
                        }

                        LaunchedEffect(currentScreen) {
                            if (gestureExitScreen != null) {
                                delay(300) // Wait for the tween(300) transition to finish
                                gestureExitScreen = null
                                targetBackScreen = null
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RenderScreen(
    screen: AppScreen,
    viewModel: HeadphonesViewModel,
    onNavigate: (AppScreen) -> Unit
) {
    when (screen) {
        AppScreen.PAIRING -> {
            PairingScreen(
                viewModel = viewModel,
                onNavigateToMain = {
                    onNavigate(AppScreen.DASHBOARD)
                }
            )
        }
        AppScreen.DASHBOARD -> {
            MainDashboard(
                viewModel = viewModel,
                onNavigateToAbout = {
                    onNavigate(AppScreen.ABOUT)
                },
                onNavigateToPairing = {
                    onNavigate(AppScreen.PAIRING)
                }
            )
        }
        AppScreen.ABOUT -> {
            AboutScreen(
                onNavigateToDebugLogs = {
                    onNavigate(AppScreen.DEBUG_LOG)
                },
                onBack = {
                    onNavigate(AppScreen.DASHBOARD)
                }
            )
        }
        AppScreen.DEBUG_LOG -> {
            DebugLogScreen(
                onBack = {
                    onNavigate(AppScreen.ABOUT)
                }
            )
        }
    }
}
