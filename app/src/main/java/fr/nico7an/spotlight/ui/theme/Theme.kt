package fr.nico7an.spotlight.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class SpotlightPalette(
    // Panneau de recherche
    val panel: Color,
    val panelStroke: Color,
    val footer: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val placeholder: Color,
    val accent: Color,
    val onAccent: Color,
    // Écran de réglages
    val background: Color,
    val card: Color,
    val divider: Color,
    val field: Color,
    val success: Color,
    val warning: Color,
)

private val LightPalette = SpotlightPalette(
    panel = Color(0xEBF8F8FA),
    panelStroke = Color(0x1A000000),
    footer = Color(0x0A000000),
    primaryText = Color(0xFF1C1C1E),
    secondaryText = Color(0xFF6E6E73),
    placeholder = Color(0xFF9A9AA0),
    accent = Color(0xFF0A7CFF),
    onAccent = Color.White,
    background = Color(0xFFF4F4F6),
    card = Color.White,
    divider = Color(0x14000000),
    field = Color(0xFFEFEFF3),
    success = Color(0xFF34C759),
    warning = Color(0xFFFF9500),
)

private val DarkPalette = SpotlightPalette(
    panel = Color(0xE61E1E21),
    panelStroke = Color(0x24FFFFFF),
    footer = Color(0x0DFFFFFF),
    primaryText = Color(0xFFF5F5F7),
    secondaryText = Color(0xFF98989F),
    placeholder = Color(0xFF6C6C72),
    accent = Color(0xFF0A84FF),
    onAccent = Color.White,
    background = Color.Black,
    card = Color(0xFF17171A),
    divider = Color(0x1FFFFFFF),
    field = Color(0xFF26262A),
    success = Color(0xFF30D158),
    warning = Color(0xFFFF9F0A),
)

val LocalSpotlightPalette = staticCompositionLocalOf { LightPalette }

@Composable
fun SpotlightTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val palette = if (dark) DarkPalette else LightPalette
    val scheme = if (dark) {
        darkColorScheme(
            primary = palette.accent,
            onPrimary = palette.onAccent,
            background = palette.background,
            onBackground = palette.primaryText,
            surface = palette.card,
            onSurface = palette.primaryText,
            onSurfaceVariant = palette.secondaryText,
            surfaceContainerHigh = palette.card,
            outline = palette.secondaryText,
        )
    } else {
        lightColorScheme(
            primary = palette.accent,
            onPrimary = palette.onAccent,
            background = palette.background,
            onBackground = palette.primaryText,
            surface = palette.card,
            onSurface = palette.primaryText,
            onSurfaceVariant = palette.secondaryText,
            surfaceContainerHigh = palette.card,
            outline = palette.secondaryText,
        )
    }
    MaterialTheme(colorScheme = scheme) {
        CompositionLocalProvider(LocalSpotlightPalette provides palette, content = content)
    }
}
