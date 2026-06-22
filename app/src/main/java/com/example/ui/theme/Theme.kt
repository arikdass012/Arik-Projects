package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GridGuardColorScheme = darkColorScheme(
    primary = NeonGreen,
    secondary = NeonBlue,
    tertiary = NeonAmber,
    background = SlateBackground,
    surface = SlateSurface,
    onBackground = Color.White,
    onSurface = Color.White,
    surfaceVariant = SlateSurfaceVariant,
    onSurfaceVariant = TextLabelMuted,
    error = NeonRed
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    // Force dark SCADA theme as requested by the user
    MaterialTheme(
        colorScheme = GridGuardColorScheme,
        typography = Typography,
        content = content
    )
}
