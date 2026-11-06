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
    AMETHYST_VOID(
        title = "Amethyst Void",
        subtitle = "Cosmic midnight with ultraviolet, magenta & plasma pink",
        background = Color(0xFF090511),
        surface = Color(0xFF140B24),
        surfaceElevated = Color(0xFF211238),
        primaryAccent = Color(0xFFD500F9),
        secondaryAccent = Color(0xFF7C4DFF),
        tertiaryGlow = Color(0xFFFF4081),
        onBackground = Color(0xFFFDF5FF),
        onSurfaceVariant = Color(0xFFCBB2E6)
    ),
    SOLAR_BAHAMUT(
        title = "Solar Bahamut",
        subtitle = "Royal onyx with molten gold, amber & solar flare",
        background = Color(0xFF0C0904),
        surface = Color(0xFF181208),
        surfaceElevated = Color(0xFF261C0D),
        primaryAccent = Color(0xFFFFB300),
        secondaryAccent = Color(0xFFFF6D00),
        tertiaryGlow = Color(0xFFFFEA00),
        onBackground = Color(0xFFFFFBF0),
        onSurfaceVariant = Color(0xFFD7C29E)
    ),
    FROST_LEVIATHAN(
        title = "Frost Leviathan",
        subtitle = "Glacial navy with ice mint, arctic cyan & crystal blue",
        background = Color(0xFF050B11),
        surface = Color(0xFF0C1722),
        surfaceElevated = Color(0xFF132536),
        primaryAccent = Color(0xFF64FFDA),
        secondaryAccent = Color(0xFF40C4FF),
        tertiaryGlow = Color(0xFFB2FF59),
        onBackground = Color(0xFFF0FFFF),
        onSurfaceVariant = Color(0xFF9EC6D8)
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

enum class DragonBorderStyle(val label: String, val description: String) {
    NEON_GRADIENT("Neon Laser", "Tri-color glowing gradient rim"),
    CYBER_PRISM("Cyber Prism", "High-contrast dual-accent edge"),
    FROSTED_GLASS("Frosted Crystal", "Soft translucent specular border"),
    MINIMAL_STEALTH("Stealth Rim", "Ultra-thin subtle dark edge")
}

enum class DragonNotifPanelStyle(val label: String, val description: String) {
    SPECTRUM_BARS("Spectrum EQ", "Live multi-band equalizer + neon progress"),
    DRAGON_PULSE("Dragon Pulse", "Flowing plasma wave + glowing emblem"),
    CYBER_VINYL("Cyber Disc", "Spinning holographic artwork + studio deck"),
    MINIMAL_GLOW("Minimal Aura", "Compact glass notification with edge glow")
}

fun hueToColor(hue: Float, saturation: Float = 0.88f, value: Float = 1.0f): Color {
    val h = (hue % 360f + 360f) % 360f
    return Color.hsv(h, saturation, value)
}
