package com.controlfinanciero.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Paleta de marca: estética fintech oscura, violeta-forward (inspirada en refs del usuario).
private val Violet = Color(0xFF9B30F5)
private val Magenta = Color(0xFFC724E0)
private val BlueAccent = Color(0xFF3B82F6)

private val DarkColorScheme = darkColorScheme(
    primary = Violet,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3A1A66),
    onPrimaryContainer = Color(0xFFE9D7FF),
    secondary = BlueAccent,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF1E3A6B),
    onSecondaryContainer = Color(0xFFD6E4FF),
    tertiary = Magenta,
    onTertiary = Color.White,
    background = Color(0xFF0F0D1A),
    onBackground = Color(0xFFEDEAF5),
    surface = Color(0xFF1B1830),
    onSurface = Color(0xFFEDEAF5),
    surfaceVariant = Color(0xFF272336),
    onSurfaceVariant = Color(0xFF9A93B5),
    surfaceContainerLowest = Color(0xFF0C0A16),
    surfaceContainerLow = Color(0xFF181527),
    surfaceContainer = Color(0xFF1B1830),
    surfaceContainerHigh = Color(0xFF231F38),
    surfaceContainerHighest = Color(0xFF2B2742),
    error = Color(0xFFF43F5E),
    onError = Color.White,
    outline = Color(0xFF3D3753),
    outlineVariant = Color(0xFF2B2742),
)

private val LightColorScheme = lightColorScheme(
    primary = Violet,
    onPrimary = Color.White,
    secondary = BlueAccent,
    tertiary = Magenta,
    background = Color(0xFFF7F5FC),
    onBackground = Color(0xFF16132A),
    surface = Color.White,
    onSurface = Color(0xFF16132A),
    surfaceVariant = Color(0xFFEFEBFB),
    onSurfaceVariant = Color(0xFF635B7A),
    error = Color(0xFFF43F5E),
    onError = Color.White,
)

@Composable
fun ControlFinancieroTheme(
    // Dark-first: oscuro por defecto. El esquema claro queda disponible para un toggle futuro.
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
