package com.nexrhythm.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = NexPrimary,
    onPrimary = Color.White,
    primaryContainer = NexPrimarySoft,
    onPrimaryContainer = NexPrimary,
    background = NexBackground,
    onBackground = NexText,
    surface = NexSurface,
    onSurface = NexText,
    surfaceVariant = NexSurface,
    onSurfaceVariant = NexMuted,
    outline = NexOutline,
    outlineVariant = NexDivider
)

private val DarkColorScheme = darkColorScheme(
    primary = NexDarkPrimary,
    onPrimary = NexDarkBackground,
    primaryContainer = NexDarkPrimarySoft,
    onPrimaryContainer = NexDarkPrimary,
    background = NexDarkBackground,
    onBackground = NexDarkText,
    surface = NexDarkSurface,
    onSurface = NexDarkText,
    surfaceVariant = NexDarkSurface,
    onSurfaceVariant = NexDarkMuted,
    outline = NexDarkOutline,
    outlineVariant = NexDarkDivider
)

@Composable
fun NexRhythmTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}