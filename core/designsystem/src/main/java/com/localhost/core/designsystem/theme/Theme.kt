package com.localhost.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
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
    secondaryHex: String = "#181C28",
    successHex: String = "#10B981",
    failHex: String = "#F43F5E",
    warnHex: String = "#F59E0B",
    themeMode: String = "DARK",
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode.uppercase()) {

        "LIGHT" -> false
        "DARK" -> true
        else -> isSystemDark
    }

    IsDarkTheme = isDark
    val primaryColor = remember(accentName) {
        val c = getAccentColor(accentName)
        PrimaryAccent = c
        PrimaryAccentContainer = c.copy(alpha = 0.18f)
        PrimaryAccentLight = c.copy(alpha = 0.85f)
        PrimaryAccentDark = c
        c
    }

    val secondaryColor = remember(secondaryHex) {
        val c = parseHexColor(secondaryHex, Color(0xFF181C28))
        SecondaryAccent = c
        SecondaryAccentBorder = c.copy(alpha = 0.5f)
        c
    }

    val successColor = remember(successHex) {
        val c = parseHexColor(successHex, Color(0xFF10B981))
        StatusSuccess = c
        StatusSuccessBg = c.copy(alpha = 0.15f)
        c
    }

    val failColor = remember(failHex) {
        val c = parseHexColor(failHex, Color(0xFFF43F5E))
        StatusFail = c
        StatusFailBg = c.copy(alpha = 0.15f)
        c
    }

    val warnColor = remember(warnHex) {
        val c = parseHexColor(warnHex, Color(0xFFF59E0B))
        StatusWarn = c
        StatusWarnBg = c.copy(alpha = 0.15f)
        c
    }

    val effectiveSecondary = if (!isDark && secondaryHex.equals("#181C28", ignoreCase = true)) {
        Color(0xFFEDF1F9)
    } else {
        secondaryColor
    }

    if (!isDark) {
        DarkBackground = Color(0xFFF4F6FB)
        DarkSurface = Color(0xFFFFFFFF)
        DarkSurfaceElevated = effectiveSecondary
        DarkSurfaceVariant = Color(0xFFE2E7F4)
        DarkBorder = Color(0xFFD2D8E6)
        DarkBorderSubtle = Color(0xFFE0E5F0)
        TextPrimary = Color(0xFF0F172A)
        TextSecondary = Color(0xFF475569)
        TextMuted = Color(0xFF64748B)
        TextOnAccent = Color(0xFFFFFFFF)
        CodeBackground = Color(0xFFF8FAFC)
        CodeBorder = Color(0xFFE2E8F0)
    } else {
        DarkBackground = Color(0xFF0C0E14)
        DarkSurface = Color(0xFF141720)
        DarkSurfaceElevated = secondaryColor
        DarkSurfaceVariant = Color(0xFF222738)
        DarkBorder = Color(0xFF252A3A)
        DarkBorderSubtle = Color(0xFF1E2332)
        TextPrimary = Color(0xFFF3F4F8)
        TextSecondary = Color(0xFF9AA3B8)
        TextMuted = Color(0xFF646E85)
        TextOnAccent = Color(0xFFFFFFFF)
        CodeBackground = Color(0xFF0A0C10)
        CodeBorder = Color(0xFF1E2230)
    }

    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = primaryColor,
            onPrimary = TextOnAccent,
            primaryContainer = primaryColor.copy(alpha = 0.20f),
            onPrimaryContainer = primaryColor,
            secondary = secondaryColor,
            onSecondary = TextOnAccent,
            background = DarkBackground,
            onBackground = TextPrimary,
            surface = DarkSurface,
            onSurface = TextPrimary,
            surfaceVariant = DarkSurfaceElevated,
            onSurfaceVariant = TextSecondary,
            outline = DarkBorder,
            outlineVariant = primaryColor.copy(alpha = 0.35f),
            error = failColor,
            onError = TextOnAccent
        )
    } else {
        lightColorScheme(
            primary = primaryColor,
            onPrimary = TextOnAccent,
            primaryContainer = primaryColor.copy(alpha = 0.15f),
            onPrimaryContainer = primaryColor,
            secondary = effectiveSecondary,
            onSecondary = TextPrimary,
            background = DarkBackground,
            onBackground = TextPrimary,
            surface = DarkSurface,
            onSurface = TextPrimary,
            surfaceVariant = effectiveSecondary,
            onSurfaceVariant = TextSecondary,
            outline = DarkBorder,
            outlineVariant = primaryColor.copy(alpha = 0.35f),
            error = failColor,
            onError = TextOnAccent
        )
    }

    CompositionLocalProvider(
        LocalAccentColor provides primaryColor,
        androidx.compose.material3.LocalContentColor provides (if (isDark) Color(0xFFF3F4F8) else Color(0xFF0F172A))
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = AppShapes,
            content = content
        )
    }
}
