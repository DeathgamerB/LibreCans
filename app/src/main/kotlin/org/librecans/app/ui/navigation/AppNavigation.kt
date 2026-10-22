package org.librecans.app.ui.navigation

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import kotlinx.coroutines.flow.StateFlow
import org.librecans.app.data.connection.ConnectionState
import org.librecans.app.ui.about.AboutEntry
import org.librecans.app.ui.about.DiagnosticsEntry
import org.librecans.app.ui.dashboard.DashboardEntry
import org.librecans.app.ui.devices.DevicesEntry
import org.librecans.app.ui.equalizer.EqualizerEntry
import org.librecans.app.ui.settings.DeviceSettingsEntry
import org.librecans.app.ui.touch.TouchControlsEntry

@Composable
fun AppNavigation(connection: StateFlow<ConnectionState>) {
    val backStack = rememberNavBackStack(DevicesRoute)
    ConnectionNavigationEffect(connection, backStack)
    val motion = MaterialTheme.motionScheme

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        transitionSpec = {
            ContentTransform(
                slideInHorizontally(motion.defaultSpatialSpec()) { it / 4 } + fadeIn(motion.defaultEffectsSpec()),
                slideOutHorizontally(motion.defaultSpatialSpec()) { -it / 8 } + fadeOut(motion.fastEffectsSpec()),
            )
        },
        popTransitionSpec = {
            ContentTransform(
                slideInHorizontally(motion.defaultSpatialSpec()) { -it / 8 } + fadeIn(motion.defaultEffectsSpec()),
                slideOutHorizontally(motion.defaultSpatialSpec()) { it / 4 } + fadeOut(motion.fastEffectsSpec()),
            )
        },
        predictivePopTransitionSpec = {
            ContentTransform(
                fadeIn(motion.defaultEffectsSpec()),
                scaleOut(motion.defaultSpatialSpec(), targetScale = 0.9f) + fadeOut(motion.defaultEffectsSpec()),
            )
        },
        entryProvider = entryProvider {
            entry<DevicesRoute> {
                DevicesEntry(onOpenDashboard = { backStack.push(DashboardRoute) }, onOpenAbout = { backStack.push(AboutRoute) })
            }
            entry<DashboardRoute> {
                DashboardEntry(
                    onOpenEqualizer = { backStack.push(EqualizerRoute) },
                    onOpenTouchControls = { backStack.push(TouchControlsRoute) },
                    onOpenDeviceSettings = { backStack.push(DeviceSettingsRoute) },
                )
            }
            entry<EqualizerRoute> { EqualizerEntry(onBack = { backStack.removeLastOrNull() }) }
            entry<TouchControlsRoute> { TouchControlsEntry(onBack = { backStack.removeLastOrNull() }) }
            entry<DeviceSettingsRoute> { DeviceSettingsEntry(onBack = { backStack.removeLastOrNull() }) }
            entry<AboutRoute> {
                AboutEntry(onBack = { backStack.removeLastOrNull() }, onOpenDiagnostics = { backStack.push(DiagnosticsRoute) })
            }
            entry<DiagnosticsRoute> { DiagnosticsEntry(onBack = { backStack.removeLastOrNull() }) }
        },
    )
}

/**
 * Opens the dashboard when a connection is established and leaves the device screens once it
 * is gone, so they never show stale controls.
 */
@Composable
private fun ConnectionNavigationEffect(connection: StateFlow<ConnectionState>, backStack: NavBackStack<NavKey>) {
    LaunchedEffect(connection, backStack) {
        var previous: ConnectionState = connection.value
        connection.collect { state ->
            val justConnected = state is ConnectionState.Connected && previous !is ConnectionState.Connected
            val gone = state !is ConnectionState.Connected && previous is ConnectionState.Connected
            if (justConnected && backStack.lastOrNull() == DevicesRoute) backStack.push(DashboardRoute)
            if (gone && backStack.any { it.needsConnection }) backStack.removeAll { it.needsConnection }
            previous = state
        }
    }
}

private fun NavBackStack<NavKey>.push(key: NavKey) {
    if (lastOrNull() != key) add(key)
}
