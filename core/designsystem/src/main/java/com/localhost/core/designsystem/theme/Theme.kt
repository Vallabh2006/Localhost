package com.localhost.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val LocalAccentColor = compositionLocalOf { Color(0xFF5A75E0) }

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun LocalhostTheme(
    accentName: String = "Indigo",
    content: @Composable () -> Unit
) {
    val accentColor = remember(accentName) {
        val c = getAccentColor(accentName)
        PrimaryAccent = c
        PrimaryAccentContainer = c.copy(alpha = 0.15f)
        PrimaryAccentLight = c.copy(alpha = 0.85f)
        PrimaryAccentDark = c
        c
    }

    val colorScheme = darkColorScheme(
        primary = accentColor,
        onPrimary = TextOnAccent,
        primaryContainer = accentColor.copy(alpha = 0.20f),
        onPrimaryContainer = accentColor,
        secondary = accentColor.copy(alpha = 0.75f),
        onSecondary = TextOnAccent,
        background = DarkBackground,
        onBackground = TextPrimary,
        surface = DarkSurface,
        onSurface = TextPrimary,
        surfaceVariant = DarkSurfaceElevated,
        onSurfaceVariant = TextSecondary,
        outline = DarkBorder,
        outlineVariant = accentColor.copy(alpha = 0.35f),
        error = StatusRed,
        onError = TextOnAccent
    )

    CompositionLocalProvider(LocalAccentColor provides accentColor) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = AppShapes,
            content = content
        )
    }
}
