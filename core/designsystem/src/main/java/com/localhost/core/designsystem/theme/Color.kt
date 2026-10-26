package com.localhost.core.designsystem.theme

import androidx.compose.ui.graphics.Color

fun parseHexColor(hex: String, fallback: Color): Color {
    val clean = hex.trim()
    return try {
        if (clean.startsWith("#")) {
            Color(android.graphics.Color.parseColor(clean))
        } else if (clean.length == 6 || clean.length == 8) {
            Color(android.graphics.Color.parseColor("#$clean"))
        } else {
            fallback
        }
    } catch (_: Exception) {
        fallback
    }
}

fun getAccentColor(name: String): Color {
    if (name.startsWith("#")) {
        return parseHexColor(name, Color(0xFF5A75E0))
    }
    return when (name.lowercase()) {
        "emerald" -> Color(0xFF10B981)
        "cyan" -> Color(0xFF06B6D4)
        "amber" -> Color(0xFFF59E0B)
        "rose" -> Color(0xFFF43F5E)
        "purple" -> Color(0xFFA855F7)
        "teal" -> Color(0xFF14B8A6)
        "sunset" -> Color(0xFFFF6B4A)
        "violet" -> Color(0xFF7C3AED)
        "crimson" -> Color(0xFFE11D48)
        "lime" -> Color(0xFF84CC16)
        "indigo" -> Color(0xFF5A75E0)
        else -> parseHexColor(name, Color(0xFF5A75E0))
    }
}

var PrimaryAccent: Color = Color(0xFF5A75E0)
var PrimaryAccentLight: Color = Color(0xFF758DE8)
var PrimaryAccentDark: Color = Color(0xFF455DC9)
var PrimaryAccentContainer: Color = Color(0x265A75E0)

var SecondaryAccent: Color = Color(0xFF181C28)
var SecondaryAccentBorder: Color = Color(0xFF262C3F)

var StatusSuccess: Color = Color(0xFF10B981)
var StatusSuccessBg: Color = Color(0x1F10B981)

var StatusFail: Color = Color(0xFFF43F5E)
var StatusFailBg: Color = Color(0x1FF43F5E)

var StatusWarn: Color = Color(0xFFF59E0B)
var StatusWarnBg: Color = Color(0x1FF59E0B)

val StatusGreen: Color get() = StatusSuccess
val StatusGreenBg: Color get() = StatusSuccessBg
val StatusRed: Color get() = StatusFail
val StatusRedBg: Color get() = StatusFailBg
val StatusYellow: Color get() = StatusWarn
val StatusYellowBg: Color get() = StatusWarnBg
val StatusOrange: Color get() = StatusWarn
val StatusOrangeBg: Color get() = StatusWarnBg

val PrimaryBlue get() = PrimaryAccent
val PrimaryBlueVariant get() = PrimaryAccentDark
val SecondaryTeal = Color(0xFF818CF8)
val AccentCyan = Color(0xFF6366F1)

var DarkBackground = Color(0xFF0C0E14)
var DarkSurface = Color(0xFF141720)
var DarkSurfaceElevated = Color(0xFF1B1F2C)
var DarkSurfaceVariant = Color(0xFF222738)
var DarkBorder = Color(0xFF252A3A)
var DarkBorderSubtle = Color(0xFF1E2332)
val DarkBorderGlow get() = PrimaryAccent.copy(alpha = 0.35f)

val StatusGray = Color(0xFF94A3B8)
val StatusGrayBg = Color(0x1F94A3B8)

var TextPrimary = Color(0xFFF3F4F8)
var TextSecondary = Color(0xFF9AA3B8)
var TextMuted = Color(0xFF646E85)
var TextOnAccent = Color(0xFFFFFFFF)

var CodeBackground = Color(0xFF0A0C10)
var CodeBorder = Color(0xFF1E2230)

var IsDarkTheme: Boolean = true
val StartButtonColor: Color get() = if (IsDarkTheme) StatusGreen else Color(0xFF059669)
val StartButtonTextColor: Color get() = if (IsDarkTheme) Color.Black else Color.White
