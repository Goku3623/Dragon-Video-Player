package com.example.ui.theme

import androidx.compose.ui.graphics.Color

enum class DragonThemePreset(
    val title: String,
    val subtitle: String,
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val primaryAccent: Color,
    val secondaryAccent: Color,
    val tertiaryGlow: Color,
    val onBackground: Color,
    val onSurfaceVariant: Color
) {
    DRAGON_FLAME(
        title = "Dragon Flame",
        subtitle = "Deep obsidian with crimson, orange & gold neon glows",
        background = Color(0xFF08070C),
        surface = Color(0xFF12101B),
        surfaceElevated = Color(0xFF1C1829),
        primaryAccent = Color(0xFFFF2A54),
        secondaryAccent = Color(0xFFFF7A1A),
        tertiaryGlow = Color(0xFFFFC72C),
        onBackground = Color(0xFFF9F6FF),
        onSurfaceVariant = Color(0xFFB7AFCB)
    ),
    ABYSSAL_DRAKE(
        title = "Abyssal Drake",
        subtitle = "OLED true black with cyan & electric-blue accents",
        background = Color(0xFF000000),
        surface = Color(0xFF080D16),
        surfaceElevated = Color(0xFF101929),
        primaryAccent = Color(0xFF00F0FF),
        secondaryAccent = Color(0xFF1A75FF),
        tertiaryGlow = Color(0xFF7B61FF),
        onBackground = Color(0xFFEEF9FF),
        onSurfaceVariant = Color(0xFF94B4CF)
    ),
    EMERALD_WYRM(
        title = "Emerald Wyrm",
        subtitle = "Smoky slate with jade & bright emerald highlights",
        background = Color(0xFF070E0B),
        surface = Color(0xFF0E1B15),
        surfaceElevated = Color(0xFF162920),
        primaryAccent = Color(0xFF00E676),
        secondaryAccent = Color(0xFF1DE9B6),
        tertiaryGlow = Color(0xFFAEEA00),
        onBackground = Color(0xFFEDFFF6),
        onSurfaceVariant = Color(0xFF9EC7B3)
    ),
    DYNAMIC_MATERIAL_YOU(
        title = "Dynamic Material You",
        subtitle = "Monet engine palette adapting to Android wallpaper",
        background = Color(0xFF0B0B10),
        surface = Color(0xFF151520),
        surfaceElevated = Color(0xFF212130),
        primaryAccent = Color(0xFFCFBCFF),
        secondaryAccent = Color(0xFFCCC2DC),
        tertiaryGlow = Color(0xFFEFB8C8),
        onBackground = Color(0xFFE6E1E5),
        onSurfaceVariant = Color(0xFFCAC4D0)
    )
}

fun hueToColor(hue: Float, saturation: Float = 0.88f, value: Float = 1.0f): Color {
    val h = (hue % 360f + 360f) % 360f
    return Color.hsv(h, saturation, value)
}
