package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.launch

/**
 * 2-Second Red Dragon Splash Animation shown when opening Dragon Video Player.
 * Features unfurling crimson dragon wings, fierce horned dragon head with glowing molten eyes,
 * swirling fire-breath vortex, and rising crimson/gold embers.
 */
@Composable
fun DragonIntroAnimationOverlay(
    modifier: Modifier = Modifier
) {
    val introProgress = remember { Animatable(0f) }
    val wingUnfurl = remember { Animatable(0.25f) }
    val fireBurst = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            introProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1850, easing = FastOutSlowInEasing)
            )
        }
        launch {
            wingUnfurl.animateTo(
                targetValue = 1.06f,
                animationSpec = tween(durationMillis = 1100, easing = FastOutSlowInEasing)
            )
            wingUnfurl.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing)
            )
        }
        launch {
            fireBurst.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing)
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "red_dragon_intro_loop")
    val flamePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "flame_phase"
    )
    val vortexAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "vortex_angle"
    )

    val deepObsidian = Color(0xFF060408)
    val darkCrimson = Color(0xFF8E001C)
    val blazingRed = Color(0xFFFF173A)
    val dragonScarlet = Color(0xFFFF3D00)
    val moltenGold = Color(0xFFFFC400)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF2B0712),
                        Color(0xFF100409),
                        deepObsidian
                    ),
                    radius = 1100f
                )
            )
            .testTag("red_dragon_intro_overlay"),
        contentAlignment = Alignment.Center
    ) {
        // Full-Screen Ambient Embers, Shockwave Rings & Dragon Scale Aura
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height * 0.44f
            val maxDim = size.maxDimension

            // Expanding Crimson Dragon Fire Aura
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        blazingRed.copy(alpha = 0.34f * fireBurst.value),
                        dragonScarlet.copy(alpha = 0.14f * fireBurst.value),
                        Color.Transparent
                    ),
                    center = Offset(cx, cy),
                    radius = maxDim * (0.25f + 0.25f * fireBurst.value)
                ),
                center = Offset(cx, cy),
                radius = maxDim * 0.5f
            )

            // Rising Red & Gold Fire Embers
            val emberCount = 28
            for (i in 0 until emberCount) {
                val seed = i * 37.3f
                val angle = (seed + flamePhase * (if (i % 2 == 0) 1.2f else -0.9f))
                val dist = (45f + (i * 19f) % 280f) * (0.55f + 0.55f * introProgress.value)
                val ex = cx + cos(angle) * dist
                val ey = cy + sin(angle) * dist - (introProgress.value * 70f * ((i % 4) + 1) * 0.25f)
                val emberRadius = (2.2f + (i % 4) * 1.3f).dp.toPx()
                val emberColor = when (i % 3) {
                    0 -> blazingRed
                    1 -> moltenGold
                    else -> dragonScarlet
                }
                drawCircle(
                    color = emberColor.copy(alpha = (0.35f + 0.5f * sin(flamePhase + i).coerceAtLeast(0f))),
                    radius = emberRadius,
                    center = Offset(ex, ey)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // Center Animated Red Dragon Emblem Canvas
            Box(
                modifier = Modifier.size(290.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawAnimatedRedDragonCrest(
                        wingScale = wingUnfurl.value,
                        fireProgress = fireBurst.value,
                        flamePhase = flamePhase,
                        vortexAngle = vortexAngle,
                        darkCrimson = darkCrimson,
                        blazingRed = blazingRed,
                        dragonScarlet = dragonScarlet,
                        moltenGold = moltenGold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Animated Brand Title & Tagline
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .scale(0.88f + 0.12f * introProgress.value)
                    .alpha((introProgress.value * 1.4f).coerceIn(0f, 1f))
            ) {
                Text(
                    text = "DRAGON VIDEO PLAYER",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 24.sp,
                        letterSpacing = 3.sp,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    blazingRed.copy(alpha = 0.28f),
                                    dragonScarlet.copy(alpha = 0.28f)
                                )
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "CRIMSON ENGINE • 4K UHD & DOLBY AUDIO",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.6.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = moltenGold
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawAnimatedRedDragonCrest(
    wingScale: Float,
    fireProgress: Float,
    flamePhase: Float,
    vortexAngle: Float,
    darkCrimson: Color,
    blazingRed: Color,
    dragonScarlet: Color,
    moltenGold: Color
) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val cy = h / 2f

    // 1. Coiling Crimson Dragon Tail / Fire Ring Vortex
    rotate(degrees = vortexAngle, pivot = Offset(cx, cy)) {
        drawArc(
            brush = Brush.sweepGradient(
                listOf(
                    Color.Transparent,
                    darkCrimson,
                    blazingRed,
                    dragonScarlet,
                    moltenGold,
                    Color.Transparent
                )
            ),
            startAngle = 15f,
            sweepAngle = 290f * fireProgress,
            useCenter = false,
            topLeft = Offset(w * 0.08f, h * 0.08f),
            size = Size(w * 0.84f, h * 0.84f),
            style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
        )
    }

    rotate(degrees = -vortexAngle * 0.7f, pivot = Offset(cx, cy)) {
        drawArc(
            brush = Brush.sweepGradient(
                listOf(
                    Color.Transparent,
                    blazingRed.copy(alpha = 0.75f),
                    moltenGold.copy(alpha = 0.85f),
                    Color.Transparent
                )
            ),
            startAngle = 140f,
            sweepAngle = 220f * fireProgress,
            useCenter = false,
            topLeft = Offset(w * 0.15f, h * 0.15f),
            size = Size(w * 0.70f, h * 0.70f),
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
        )
    }

    val wingFlapOffset = sin(flamePhase) * (h * 0.025f)
    val dragonBrush = Brush.linearGradient(
        colors = listOf(moltenGold, dragonScarlet, blazingRed, darkCrimson),
        start = Offset(cx, h * 0.12f),
        end = Offset(cx, h * 0.88f)
    )
    val innerWingBrush = Brush.linearGradient(
        colors = listOf(blazingRed, darkCrimson, Color(0xFF4A000E)),
        start = Offset(cx, h * 0.25f),
        end = Offset(cx, h * 0.80f)
    )

    // 2. Unfurling Left & Right Red Dragon Wings (Symmetrical Fierce Bat/Dragon Wing Silhouettes)
    for (dir in listOf(-1f, 1f)) {
        scale(scaleX = dir * wingScale, scaleY = wingScale, pivot = Offset(cx, cy)) {
            // Outer Primary Dragon Wing Pinion
            val outerWing = Path().apply {
                moveTo(cx + w * 0.05f, cy + h * 0.02f)
                cubicTo(
                    cx + w * 0.16f, cy - h * 0.22f + wingFlapOffset,
                    cx + w * 0.34f, cy - h * 0.34f + wingFlapOffset,
                    cx + w * 0.46f, cy - h * 0.20f + wingFlapOffset
                )
                // Wingtip claw & scalloped dragon membrane
                quadraticTo(
                    cx + w * 0.37f, cy - h * 0.10f + wingFlapOffset,
                    cx + w * 0.42f, cy + h * 0.02f + wingFlapOffset * 0.6f
                )
                quadraticTo(
                    cx + w * 0.31f, cy + h * 0.04f,
                    cx + w * 0.33f, cy + h * 0.17f
                )
                quadraticTo(
                    cx + w * 0.22f, cy + h * 0.14f,
                    cx + w * 0.20f, cy + h * 0.27f
                )
                quadraticTo(
                    cx + w * 0.12f, cy + h * 0.20f,
                    cx + w * 0.06f, cy + h * 0.16f
                )
                close()
            }
            drawPath(path = outerWing, brush = innerWingBrush)
            drawPath(
                path = outerWing,
                brush = dragonBrush,
                style = Stroke(width = 2.2.dp.toPx(), join = StrokeJoin.Round)
            )

            // Wing Bone Struts in Glowing Crimson-Gold
            val wingRib = Path().apply {
                moveTo(cx + w * 0.07f, cy - h * 0.02f)
                quadraticTo(
                    cx + w * 0.24f, cy - h * 0.20f + wingFlapOffset,
                    cx + w * 0.42f, cy + h * 0.02f + wingFlapOffset * 0.6f
                )
                moveTo(cx + w * 0.16f, cy - h * 0.10f + wingFlapOffset * 0.5f)
                lineTo(cx + w * 0.33f, cy + h * 0.17f)
            }
            drawPath(
                path = wingRib,
                color = moltenGold.copy(alpha = 0.72f),
                style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }

    // 3. Coiling S-Curved Red Dragon Neck, Spine & Fierce Horned Head
    scale(scaleX = wingScale, scaleY = wingScale, pivot = Offset(cx, cy)) {
        // Sinuous Dragon Body & Coiled Tail
        val bodyPath = Path().apply {
            moveTo(cx - w * 0.04f, cy + h * 0.33f)
            cubicTo(
                cx - w * 0.18f, cy + h * 0.24f,
                cx + w * 0.16f, cy + h * 0.08f,
                cx - w * 0.02f, cy - h * 0.12f
            )
            cubicTo(
                cx + w * 0.06f, cy - h * 0.04f,
                cx + w * 0.22f, cy + h * 0.16f,
                cx + w * 0.03f, cy + h * 0.36f
            )
            close()
        }
        drawPath(path = bodyPath, brush = dragonBrush)

        // Fierce Dragon Head & Swept-Back Crimson Horns
        val headPath = Path().apply {
            // Crown & Long Swept-Back Dragon Horn
            moveTo(cx - w * 0.16f, cy - h * 0.32f)
            quadraticTo(cx - w * 0.04f, cy - h * 0.24f, cx + w * 0.02f, cy - h * 0.20f)
            // Secondary brow spike
            lineTo(cx - w * 0.08f, cy - h * 0.22f)
            lineTo(cx + w * 0.05f, cy - h * 0.16f)
            // Upper snout & fang tip
            lineTo(cx + w * 0.18f, cy - h * 0.14f)
            lineTo(cx + w * 0.15f, cy - h * 0.09f)
            // Open roaring upper jaw
            lineTo(cx + w * 0.04f, cy - h * 0.10f)
            // Roaring lower jaw
            lineTo(cx + w * 0.14f, cy - h * 0.03f)
            lineTo(cx + w * 0.01f, cy - h * 0.04f)
            // Cheek crest & neck join
            lineTo(cx - w * 0.07f, cy - h * 0.11f)
            close()
        }
        drawPath(path = headPath, brush = dragonBrush)
        drawPath(
            path = headPath,
            color = moltenGold.copy(alpha = 0.85f),
            style = Stroke(width = 1.6.dp.toPx(), join = StrokeJoin.Round)
        )

        // Glowing Molten Dragon Eye
        val eyeCenter = Offset(cx + w * 0.04f, cy - h * 0.14f)
        drawCircle(
            color = moltenGold,
            radius = 5.dp.toPx(),
            center = eyeCenter
        )
        drawCircle(
            color = Color.White,
            radius = 2.2.dp.toPx(),
            center = eyeCenter
        )

        // 4. Roaring Dragon Fire-Breath Plume Erupting from Jaws
        if (fireProgress > 0.15f) {
            val flameFlicker = sin(flamePhase * 2f) * (h * 0.02f)
            val fireBreathPath = Path().apply {
                moveTo(cx + w * 0.12f, cy - h * 0.07f)
                cubicTo(
                    cx + w * 0.24f, cy - h * 0.14f + flameFlicker,
                    cx + w * 0.34f, cy - h * 0.10f - flameFlicker,
                    cx + w * 0.42f * fireProgress, cy - h * 0.06f + flameFlicker
                )
                cubicTo(
                    cx + w * 0.32f, cy + h * 0.01f + flameFlicker,
                    cx + w * 0.22f, cy - h * 0.01f,
                    cx + w * 0.12f, cy - h * 0.07f
                )
                close()
            }
            drawPath(
                path = fireBreathPath,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.White,
                        moltenGold,
                        dragonScarlet,
                        blazingRed.copy(alpha = 0.0f)
                    ),
                    startX = cx + w * 0.10f,
                    endX = cx + w * 0.44f
                )
            )
        }
    }
}
