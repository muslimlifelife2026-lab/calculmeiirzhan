package com.calculator.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary           = AccentViolet,
    primaryContainer  = AccentVioletLight,
    secondary         = AccentAmber,
    secondaryContainer = AccentAmberLight,
    tertiary          = AccentCyan,
    background        = Background,
    surface           = SurfaceCard,
    surfaceVariant    = SurfaceElevated,
    onPrimary         = Background,
    onSecondary       = Color.Black,
    onTertiary        = Color.Black,
    onBackground      = TextPrimary,
    onSurface         = TextPrimary,
    onSurfaceVariant  = TextSecondary,
    error             = ErrorRed,
    onError           = Background,
    outline           = TextMuted
)

@Composable
fun AeroCalcTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography
    ) {
        // The glass host is transparent and intentionally has no opaque Material Surface.
        CompositionLocalProvider(LocalContentColor provides TextPrimary, content = content)
    }
}
