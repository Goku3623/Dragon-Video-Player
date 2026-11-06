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
    val showDragonScalePattern: Boolean = true
) {
    val activePrimary: Color
        get() = if (useCustomAccent) hueToColor(customAccentHue, 0.88f, 1.0f) else preset.primaryAccent

    val activeSecondary: Color
        get() = if (useCustomAccent) hueToColor((customAccentHue + 32f) % 360f, 0.90f, 0.96f) else preset.secondaryAccent

    val activeTertiaryGlow: Color
        get() = if (useCustomAccent) hueToColor((customAccentHue + 64f) % 360f, 0.85f, 1.0f) else preset.tertiaryGlow
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
        if (themeConfig.useCustomAccent) {
            dynamic.copy(
                primary = themeConfig.activePrimary,
                secondary = themeConfig.activeSecondary,
                tertiary = themeConfig.activeTertiaryGlow
            )
        } else {
            dynamic
        }
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
            background = themeConfig.preset.background,
            onBackground = themeConfig.preset.onBackground,
            surface = themeConfig.preset.surface,
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
    val baseAlpha = (0.48f + config.backdropBlurStrength * 0.44f).coerceIn(0.45f, 0.94f)
    val surfaceColor = if (elevated) scheme.surfaceVariant.copy(alpha = baseAlpha) else scheme.surface.copy(alpha = baseAlpha)
    val glowAlpha = (config.glowIntensity * (if (accentGlow) 0.55f else 0.28f)).coerceIn(0.05f, 0.85f)

    return this
        .clip(shape)
        .background(
            brush = Brush.linearGradient(
                colors = listOf(
                    surfaceColor,
                    scheme.surface.copy(alpha = (baseAlpha - 0.08f).coerceAtLeast(0.35f)),
                    if (accentGlow) scheme.primary.copy(alpha = 0.08f * config.glowIntensity) else surfaceColor
                )
            ),
            shape = shape
        )
        .drawBehind {
            if (config.glowIntensity > 0.05f && accentGlow) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            scheme.primary.copy(alpha = 0.18f * config.glowIntensity),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.85f, size.height * 0.15f),
                        radius = size.maxDimension * 0.7f
                    )
                )
            }
        }
        .border(
            width = if (accentGlow) 1.2.dp else 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    scheme.primary.copy(alpha = glowAlpha),
                    Color.White.copy(alpha = 0.12f * config.backdropBlurStrength),
                    scheme.secondary.copy(alpha = glowAlpha * 0.65f)
                )
            ),
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
