package org.librecans.app.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

private const val LIGHT_ENOUGH = 0.45f
private const val TOWARDS_WHITE = 0.75f

/**
 * A light, theme-tinted colour to put behind the dark device artwork. Some palettes (monochrome
 * wallpapers, dark themes) make the primary container dark, so it is lightened when needed.
 */
@Composable
@ReadOnlyComposable
fun artworkBackdrop(): Color {
    val container = MaterialTheme.colorScheme.primaryContainer
    return if (container.luminance() >= LIGHT_ENOUGH) container else lerp(container, Color.White, TOWARDS_WHITE)
}
