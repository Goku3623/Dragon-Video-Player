package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class DragonThemeConfig(
    val preset: DragonThemePreset = DragonThemePreset.DRAGON_FLAME,
    val useCustomAccent: Boolean = false,
    val customAccentHue: Float = 348f,
    val glowIntensity: Float = 0.75f,
    val hudCornerRadiusDp: Float = 18f,
    val backdropBlurStrength: Float = 0.72f,
    val showDragonScalePattern: Boolean = true,
    val borderStyle: DragonBorderStyle = DragonBorderStyle.NEON_GRADIENT,
    val notifPanelStyle: DragonNotifPanelStyle = DragonNotifPanelStyle.SPECTRUM_BARS,
    val neonBorderWidthDp: Float = 1.3f,
    val surfaceOpacity: Float = 0.80f,
    val useTrueOledBlack: Boolean = false,
    val enableAnimatedAura: Boolean = true
) {
    val activePrimary: Color
        get() = if (useCustomAccent) hueToColor(customAccentHue, 0.88f, 1.0f) else preset.primaryAccent

    val activeSecondary: Color
        get() = if (useCustomAccent) hueToColor((customAccentHue + 32f) % 360f, 0.90f, 0.96f) else preset.secondaryAccent

    val activeTertiaryGlow: Color
        get() = if (useCustomAccent) hueToColor((customAccentHue + 64f) % 360f, 0.85f, 1.0f) else preset.tertiaryGlow

    val effectiveBackground: Color
        get() = if (useTrueOledBlack) Color(0xFF000000) else preset.background

    val effectiveSurface: Color
        get() = if (useTrueOledBlack) Color(0xFF06060A) else preset.surface
}

val LocalDragonThemeConfig = staticCompositionLocalOf { DragonThemeConfig() }

@Composable
fun DragonVideoPlayerTheme(
    themeConfig: DragonThemeConfig = DragonThemeConfig(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val baseScheme = if (themeConfig.preset == DragonThemePreset.DYNAMIC_MATERIAL_YOU &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    ) {
        val dynamic = dynamicDarkColorScheme(context)
        dynamic.copy(
            primary = if (themeConfig.useCustomAccent) themeConfig.activePrimary else dynamic.primary,
            secondary = if (themeConfig.useCustomAccent) themeConfig.activeSecondary else dynamic.secondary,
            tertiary = if (themeConfig.useCustomAccent) themeConfig.activeTertiaryGlow else dynamic.tertiary,
            background = if (themeConfig.useTrueOledBlack) Color.Black else dynamic.background,
            surface = if (themeConfig.useTrueOledBlack) Color(0xFF07070B) else dynamic.surface
        )
    } else {
        darkColorScheme(
            primary = themeConfig.activePrimary,
            onPrimary = Color(0xFF09080D),
            primaryContainer = themeConfig.activePrimary.copy(alpha = 0.22f),
            onPrimaryContainer = themeConfig.preset.onBackground,
            secondary = themeConfig.activeSecondary,
            onSecondary = Color(0xFF09080D),
            secondaryContainer = themeConfig.activeSecondary.copy(alpha = 0.2f),
            onSecondaryContainer = themeConfig.preset.onBackground,
            tertiary = themeConfig.activeTertiaryGlow,
            background = themeConfig.effectiveBackground,
            onBackground = themeConfig.preset.onBackground,
            surface = themeConfig.effectiveSurface,
            onSurface = themeConfig.preset.onBackground,
            surfaceVariant = themeConfig.preset.surfaceElevated,
            onSurfaceVariant = themeConfig.preset.onSurfaceVariant,
            outline = themeConfig.activePrimary.copy(alpha = 0.35f)
        )
    }

    val radius = themeConfig.hudCornerRadiusDp.dp
    val shapes = Shapes(
        extraSmall = RoundedCornerShape((themeConfig.hudCornerRadiusDp * 0.4f).dp),
        small = RoundedCornerShape((themeConfig.hudCornerRadiusDp * 0.6f).dp),
        medium = RoundedCornerShape(radius),
        large = RoundedCornerShape((themeConfig.hudCornerRadiusDp * 1.25f).dp),
        extraLarge = RoundedCornerShape((themeConfig.hudCornerRadiusDp * 1.5f).dp)
    )

    CompositionLocalProvider(LocalDragonThemeConfig provides themeConfig) {
        MaterialTheme(
            colorScheme = baseScheme,
            typography = Typography,
            shapes = shapes,
            content = content
        )
    }
}

/**
 * Applies a dark-first glassmorphic container with dynamic glow border and optional dragon-scale motif.
 */
@Composable
fun Modifier.dragonGlassSurface(
    cornerRadiusOverride: Dp? = null,
    elevated: Boolean = false,
    accentGlow: Boolean = false
): Modifier {
    val config = LocalDragonThemeConfig.current
    val scheme = MaterialTheme.colorScheme
    val cornerRadius = cornerRadiusOverride ?: config.hudCornerRadiusDp.dp
    val shape = RoundedCornerShape(cornerRadius)
    val baseAlpha = (config.surfaceOpacity * (0.75f + config.backdropBlurStrength * 0.25f)).coerceIn(0.38f, 0.97f)
    val surfaceColor = if (elevated) scheme.surfaceVariant.copy(alpha = baseAlpha) else scheme.surface.copy(alpha = baseAlpha)
    val glowAlpha = (config.glowIntensity * (if (accentGlow) 0.68f else 0.32f)).coerceIn(0.05f, 0.92f)
    val rimWidth = (config.neonBorderWidthDp * (if (accentGlow) 1.15f else 0.85f)).coerceIn(0.4f, 4.0f).dp

    val borderBrush = when (config.borderStyle) {
        DragonBorderStyle.NEON_GRADIENT -> Brush.linearGradient(
            colors = listOf(
                scheme.primary.copy(alpha = glowAlpha),
                scheme.tertiary.copy(alpha = glowAlpha * 0.55f),
                scheme.secondary.copy(alpha = glowAlpha * 0.80f)
            )
        )
        DragonBorderStyle.CYBER_PRISM -> Brush.sweepGradient(
            colors = listOf(
                scheme.primary.copy(alpha = glowAlpha),
                scheme.secondary.copy(alpha = glowAlpha),
                scheme.tertiary.copy(alpha = glowAlpha * 0.85f),
                scheme.primary.copy(alpha = glowAlpha)
            )
        )
        DragonBorderStyle.FROSTED_GLASS -> Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = (0.28f * config.backdropBlurStrength).coerceIn(0.08f, 0.45f)),
                scheme.primary.copy(alpha = glowAlpha * 0.35f),
                Color.White.copy(alpha = 0.08f)
            )
        )
        DragonBorderStyle.MINIMAL_STEALTH -> Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.10f),
                scheme.primary.copy(alpha = glowAlpha * 0.22f)
            )
        )
    }

    return this
        .clip(shape)
        .background(
            brush = Brush.linearGradient(
                colors = listOf(
                    surfaceColor,
                    scheme.surface.copy(alpha = (baseAlpha - 0.08f).coerceAtLeast(0.32f)),
                    if (accentGlow) scheme.primary.copy(alpha = 0.10f * config.glowIntensity) else surfaceColor
                )
            ),
            shape = shape
        )
        .drawBehind {
            if (config.glowIntensity > 0.05f && accentGlow) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            scheme.primary.copy(alpha = 0.20f * config.glowIntensity),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.85f, size.height * 0.15f),
                        radius = size.maxDimension * 0.72f
                    )
                )
                if (config.enableAnimatedAura) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                scheme.secondary.copy(alpha = 0.14f * config.glowIntensity),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.15f, size.height * 0.85f),
                            radius = size.maxDimension * 0.55f
                        )
                    )
                }
            }
        }
        .border(
            width = rimWidth,
            brush = borderBrush,
            shape = shape
        )
}

/**
 * Draws a subtle geometric dragon-scale pattern behind headers and hero surfaces.
 */
@Composable
fun Modifier.dragonScaleMesh(
    scaleAlpha: Float = 0.07f
): Modifier {
    val config = LocalDragonThemeConfig.current
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    return this.drawBehind {
        if (!config.showDragonScalePattern) return@drawBehind
        val stepX = 36.dp.toPx()
        val stepY = 22.dp.toPx()
        val cols = (size.width / stepX).toInt() + 2
        val rows = (size.height / stepY).toInt() + 2
        val strokeWidth = 1.dp.toPx()
        val effectiveAlpha = (scaleAlpha * (0.4f + config.glowIntensity * 0.6f)).coerceIn(0.02f, 0.16f)

        for (r in 0..rows) {
            val offsetX = if (r % 2 == 0) 0f else stepX / 2f
            for (c in -1..cols) {
                val cx = c * stepX + offsetX
                val cy = r * stepY
                val path = Path().apply {
                    moveTo(cx - stepX * 0.5f, cy)
                    quadraticTo(cx, cy + stepY * 0.95f, cx + stepX * 0.5f, cy)
                }
                drawPath(
                    path = path,
                    color = if ((r + c) % 3 == 0) secondary.copy(alpha = effectiveAlpha) else primary.copy(alpha = effectiveAlpha),
                    style = Stroke(width = strokeWidth)
                )
            }
        }
    }
}
