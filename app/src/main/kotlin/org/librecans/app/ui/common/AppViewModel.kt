package org.librecans.app.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import org.librecans.app.AppContainer
import org.librecans.app.LibreCansApp

/** Creates a ViewModel for the current navigation entry from the app wide [AppContainer]. */
@Composable
inline fun <reified VM : ViewModel> appViewModel(crossinline create: (AppContainer) -> VM): VM {
    val container = (LocalContext.current.applicationContext as LibreCansApp).container
    return viewModel { create(container) }
}
