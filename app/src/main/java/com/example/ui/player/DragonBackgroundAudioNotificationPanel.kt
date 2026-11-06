package com.example.ui.player

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas as AndroidCanvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import android.graphics.PorterDuff
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.hardware.display.DisplayManager
import android.os.Build
import android.view.Display
import android.widget.RemoteViews
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.VideoEntity
import com.example.player.StorageScannerHelper
import com.example.ui.screens.ActualVideoThumbnailBox
import com.example.ui.theme.DragonNotifPanelStyle
import com.example.ui.theme.DragonThemeConfig
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalDragonThemeConfig
import com.example.ui.theme.dragonGlassSurface
import com.example.ui.theme.dragonScaleMesh
import kotlin.math.cos
import kotlin.math.sin

/**
 * Compact Theme-Adaptive In-App Background Audio Notification Panel.
 * Matches the exact 4-row visual deck:
 * 1. Header Bar: [Headphone + BG AUDIO • THEME] | [✨ Style Pill] | [↗ Video] | [✕]
 * 2. Media Row: [Actual Video Thumbnail with 4K UHD & Duration] | [Title + Codec/Boost] | [Live Playing Animation]
 * 3. Capsule Scrubber: [Current Time] | [Split-Capsule Progress Bar + Vertical Cursor + End Dot] | [Total Duration]
 * 4. Transport Controls: [|◄] | [↺ 10] | [Glowing || / ▶] | [↻ 10] | [►|]
 */
@Composable
fun DragonBackgroundAudioNotificationPanel(
    video: VideoEntity,
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    volumeBoostPercent: Int,
    onExpandToVideoPlayer: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekRelative: (Long) -> Unit,
    onPlayPrevious: () -> Unit,
    onPlayNext: () -> Unit,
    onCycleNotifStyle: () -> Unit,
    onStopBackgroundSession: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeConfig = LocalDragonThemeConfig.current
    val scheme = MaterialTheme.colorScheme
    val notifStyle = themeConfig.notifPanelStyle
    val safeDuration = durationMs.coerceAtLeast(1L)

    // Smooth Choreographer VSYNC-driven animation running at the device's native refresh rate
    var wavePhase by remember { mutableFloatStateOf(0f) }
    var discRotation by remember { mutableFloatStateOf(0f) }
    var smoothPositionMs by remember { mutableFloatStateOf(currentPositionMs.toFloat()) }
    var lastSyncPositionMs by remember { androidx.compose.runtime.mutableLongStateOf(currentPositionMs) }
    var lastSyncNanos by remember { androidx.compose.runtime.mutableLongStateOf(0L) }

    LaunchedEffect(currentPositionMs) {
        lastSyncPositionMs = currentPositionMs
        lastSyncNanos = System.nanoTime()
    }

    LaunchedEffect(isPlaying) {
        if (!isPlaying) {
            smoothPositionMs = currentPositionMs.toFloat()
            return@LaunchedEffect
        }
        var lastFrameNanos = 0L
        val twoPi = (2.0 * Math.PI).toFloat()
        while (true) {
            withFrameNanos { frameNanos ->
                val dtSec = if (lastFrameNanos == 0L) {
                    1f / 60f
                } else {
                    ((frameNanos - lastFrameNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                }
                lastFrameNanos = frameNanos
                wavePhase = (wavePhase + dtSec * 3.4f) % twoPi
                discRotation = (discRotation + dtSec * 45f) % 360f

                if (lastSyncNanos > 0L) {
                    val elapsedSec = (frameNanos - lastSyncNanos) / 1_000_000_000f
                    smoothPositionMs = (lastSyncPositionMs + elapsedSec * 1000f).coerceIn(0f, safeDuration.toFloat())
                } else {
                    smoothPositionMs = currentPositionMs.toFloat()
                }
            }
        }
    }

    val progress = (smoothPositionMs / safeDuration.toFloat()).coerceIn(0f, 1f)
    val cardRadius = (themeConfig.hudCornerRadiusDp).coerceIn(12f, 26f).dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .dragonGlassSurface(
                cornerRadiusOverride = cardRadius,
                elevated = true,
                accentGlow = true
            )
            .dragonScaleMesh(scaleAlpha = 0.08f)
            .clickable { onExpandToVideoPlayer() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("background_audio_notification_panel"),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // 1. Header Row: [Headphone + BG AUDIO • THEME] | [✨ Dragon Pulse] | [↗ Video] | [✕]
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(scheme.primary, scheme.secondary)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Headphones,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(12.dp)
                    )
                }
                Text(
                    text = "BG AUDIO • ${themeConfig.preset.title.uppercase()}",
                    fontSize = 10.sp,
                    fontFamily = JetBrainsMonoFontFamily,
                    color = scheme.primary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                // Style Switcher Pill (✨ Dragon Pulse / Spectrum Bars / Cyber Vinyl / Minimal Glow)
                Surface(
                    onClick = onCycleNotifStyle,
                    shape = RoundedCornerShape(50),
                    color = scheme.primary.copy(alpha = 0.16f),
                    modifier = Modifier
                        .border(
                            width = 0.8.dp,
                            color = scheme.primary.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(50)
                        )
                        .testTag("cycle_bg_notif_style_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Cycle Notification Style",
                            tint = scheme.tertiary,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = notifStyle.label,
                            fontSize = 10.sp,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }

                // Expand & Continue Video Playback Pill
                Surface(
                    onClick = onExpandToVideoPlayer,
                    shape = RoundedCornerShape(50),
                    color = scheme.secondary.copy(alpha = 0.22f),
                    modifier = Modifier.testTag("expand_bg_notif_to_video_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInFull,
                            contentDescription = "Continue Video",
                            tint = scheme.primary,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = "Video",
                            fontSize = 10.sp,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }

                // Close / Stop Background Audio Session
                IconButton(
                    onClick = onStopBackgroundSession,
                    modifier = Modifier
                        .size(22.dp)
                        .testTag("stop_bg_audio_session_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Stop Background Audio",
                        tint = scheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }

        // 2. Artwork + Title/Metadata + Live Theme-Adaptive Wave Visualizer Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onExpandToVideoPlayer() },
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (notifStyle == DragonNotifPanelStyle.CYBER_VINYL) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .rotate(if (isPlaying) discRotation else 0f)
                        .clip(CircleShape)
                        .border(
                            width = 1.5.dp,
                            brush = Brush.sweepGradient(
                                listOf(scheme.primary, scheme.tertiary, scheme.secondary, scheme.primary)
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    ActualVideoThumbnailBox(
                        video = video,
                        compactMode = true,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color.Black)
                            .border(1.5.dp, scheme.primary, CircleShape)
                    )
                }
            } else {
                ActualVideoThumbnailBox(
                    video = video,
                    compactMode = false,
                    modifier = Modifier
                        .width(68.dp)
                        .height(42.dp)
                        .border(
                            width = 1.2.dp,
                            brush = Brush.linearGradient(listOf(scheme.primary, scheme.secondary)),
                            shape = RoundedCornerShape(9.dp)
                        )
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = video.title,
                    fontSize = 14.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${video.containerFormat} • ${video.audioCodec} • Boost ${volumeBoostPercent}%",
                    fontSize = 10.sp,
                    fontFamily = JetBrainsMonoFontFamily,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Live Animated Wave / Spectrum Visualizer on the Right
            ThemeAdaptiveAudioVisualizer(
                style = notifStyle,
                isPlaying = isPlaying,
                phase = wavePhase,
                primaryColor = scheme.primary,
                secondaryColor = scheme.secondary,
                tertiaryColor = scheme.tertiary,
                modifier = Modifier
                    .width(58.dp)
                    .height(28.dp)
                    .clickable { onCycleNotifStyle() }
            )
        }

        // 3. Capsule Progress Scrubber Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = StorageScannerHelper.formatDurationMs(currentPositionMs),
                fontSize = 10.sp,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.SemiBold,
                color = scheme.primary
            )

            CompactDragonProgressBar(
                progress = progress,
                primaryColor = scheme.primary,
                secondaryColor = scheme.secondary,
                onSeekFraction = { fraction ->
                    onSeekTo((fraction * safeDuration).toLong())
                },
                modifier = Modifier
                    .weight(1f)
                    .height(18.dp)
                    .testTag("bg_notif_seek_slider")
            )

            Text(
                text = StorageScannerHelper.formatDurationMs(safeDuration),
                fontSize = 10.sp,
                fontFamily = JetBrainsMonoFontFamily,
                color = scheme.onSurfaceVariant
            )
        }

        // 4. Transport Controls Row (Previous, -10s, Glowing Play/Pause, +10s, Next)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onPlayPrevious,
                modifier = Modifier
                    .size(30.dp)
                    .testTag("bg_notif_prev_button")
            ) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Previous Track",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = { onSeekRelative(-10_000L) },
                modifier = Modifier
                    .size(30.dp)
                    .testTag("bg_notif_rewind_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Replay10,
                    contentDescription = "Rewind 10 seconds",
                    tint = scheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Glowing Theme-Styled Play / Pause Button
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(scheme.primary, scheme.secondary)
                        )
                    )
                    .border(
                        width = 1.2.dp,
                        color = scheme.tertiary.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(11.dp)
                    )
                    .clickable { onTogglePlayPause() }
                    .testTag("bg_notif_play_pause_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause Background Audio" else "Play Background Audio",
                    tint = Color.Black,
                    modifier = Modifier.size(21.dp)
                )
            }

            IconButton(
                onClick = { onSeekRelative(10_000L) },
                modifier = Modifier
                    .size(30.dp)
                    .testTag("bg_notif_forward_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Forward10,
                    contentDescription = "Forward 10 seconds",
                    tint = scheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = onPlayNext,
                modifier = Modifier
                    .size(30.dp)
                    .testTag("bg_notif_next_button")
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next Track",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun CompactDragonProgressBar(
    progress: Float,
    primaryColor: Color,
    secondaryColor: Color,
    onSeekFraction: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val frac = (offset.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                    onSeekFraction(frac)
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    val frac = (change.position.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                    onSeekFraction(frac)
                }
            }
    ) {
        val w = size.width
        val h = size.height
        val trackHeight = 8.dp.toPx()
        val trackTop = (h - trackHeight) / 2f
        val corner = CornerRadius(trackHeight / 2f, trackHeight / 2f)
        val cursorX = (w * progress.coerceIn(0.02f, 0.98f))
        val gap = 5.dp.toPx()

        // Active Left Capsule
        val activeWidth = (cursorX - gap).coerceAtLeast(trackHeight)
        drawRoundRect(
            brush = Brush.horizontalGradient(listOf(primaryColor, secondaryColor)),
            topLeft = Offset(0f, trackTop),
            size = Size(activeWidth, trackHeight),
            cornerRadius = corner
        )

        // Inactive Right Capsule
        val inactiveStart = (cursorX + gap).coerceAtMost(w)
        val inactiveWidth = (w - inactiveStart).coerceAtLeast(0f)
        if (inactiveWidth > 2f) {
            drawRoundRect(
                color = Color.White.copy(alpha = 0.18f),
                topLeft = Offset(inactiveStart, trackTop),
                size = Size(inactiveWidth, trackHeight),
                cornerRadius = corner
            )
            // Subtle end dot inside inactive capsule
            val dotX = (w - trackHeight * 0.7f).coerceAtLeast(inactiveStart + 4f)
            drawCircle(
                color = primaryColor,
                radius = 2.2.dp.toPx(),
                center = Offset(dotX, h / 2f)
            )
        }

        // Vertical Glowing Pill Thumb
        val thumbW = 3.2.dp.toPx()
        val thumbH = 14.dp.toPx()
        drawRoundRect(
            color = primaryColor,
            topLeft = Offset(cursorX - thumbW / 2f, (h - thumbH) / 2f),
            size = Size(thumbW, thumbH),
            cornerRadius = CornerRadius(thumbW / 2f, thumbW / 2f)
        )
    }
}

@Composable
private fun ThemeAdaptiveAudioVisualizer(
    style: DragonNotifPanelStyle,
    isPlaying: Boolean,
    phase: Float,
    primaryColor: Color,
    secondaryColor: Color,
    tertiaryColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val brush = Brush.horizontalGradient(listOf(primaryColor, secondaryColor, tertiaryColor))

        when (style) {
            DragonNotifPanelStyle.DRAGON_PULSE, DragonNotifPanelStyle.MINIMAL_GLOW -> {
                val path1 = Path()
                val path2 = Path()
                val steps = 60
                val amp = if (isPlaying) h * 0.38f else h * 0.14f
                for (s in 0..steps) {
                    val sRatio = s.toFloat() / steps
                    val x = sRatio * w
                    val wave1 = sin(phase + s * 0.28f) * 0.76f + sin(phase * 0.55f + s * 0.12f) * 0.24f
                    val wave2 = sin(phase * 1.15f + s * 0.34f + 1.25f) * 0.70f + cos(phase * 0.70f + s * 0.16f) * 0.30f
                    val y1 = h / 2f + wave1 * amp
                    val y2 = h / 2f + wave2 * (amp * 0.78f)
                    if (s == 0) {
                        path1.moveTo(x, y1)
                        path2.moveTo(x, y2)
                    } else {
                        path1.lineTo(x, y1)
                        path2.lineTo(x, y2)
                    }
                }
                drawPath(path = path2, color = secondaryColor.copy(alpha = 0.55f), style = Stroke(width = 1.8.dp.toPx()))
                drawPath(path = path1, brush = brush, style = Stroke(width = 2.4.dp.toPx()))
            }

            DragonNotifPanelStyle.SPECTRUM_BARS -> {
                val barCount = 8
                val gap = 2.5.dp.toPx()
                val barWidth = ((w - gap * (barCount - 1)) / barCount).coerceAtLeast(2f)
                for (i in 0 until barCount) {
                    val dynamicFactor = if (isPlaying) {
                        val harmonic1 = sin(phase * 1.2f + i * 0.68f) * 0.26f
                        val harmonic2 = cos(phase * 0.72f + i * 1.05f) * 0.16f
                        (0.50f + harmonic1 + harmonic2).coerceIn(0.18f, 0.96f)
                    } else {
                        0.22f
                    }
                    val barHeight = (h * dynamicFactor).coerceAtLeast(barWidth)
                    val left = i * (barWidth + gap)
                    val top = h - barHeight
                    drawRoundRect(
                        brush = Brush.verticalGradient(listOf(tertiaryColor, primaryColor, secondaryColor)),
                        topLeft = Offset(left, top),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                    )
                }
            }

            DragonNotifPanelStyle.CYBER_VINYL -> {
                val center = Offset(w / 2f, h / 2f)
                val maxR = minOf(w, h) * 0.44f
                val pulse = if (isPlaying) (sin(phase * 1.4f) * 0.08f + 0.92f) else 0.75f
                drawCircle(
                    color = primaryColor.copy(alpha = 0.22f),
                    radius = maxR * pulse,
                    center = center
                )
                drawCircle(
                    brush = Brush.sweepGradient(listOf(primaryColor, tertiaryColor, secondaryColor, primaryColor)),
                    radius = maxR * 0.78f,
                    center = center,
                    style = Stroke(width = 2.2.dp.toPx())
                )
                drawCircle(
                    color = tertiaryColor,
                    radius = 2.5.dp.toPx(),
                    center = center
                )
            }
        }
    }
}

/**
 * Background BroadcastReceiver that processes all functional button clicks from the
 * Android System Notification Panel WITHOUT opening MainActivity.
 */
class DragonNotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val seekFraction = intent.getFloatExtra(DragonSystemAudioNotificationHelper.EXTRA_SEEK_FRACTION, -1f)
        DragonSystemAudioNotificationHelper.dispatchBackgroundAction(action, seekFraction)
    }
}

/**
 * Renders the Android System Notification Panel using a custom [RemoteViews] layout that
 * matches the exact visual design of the in-app background audio player (with zero extra Android
 * notification chrome/text outside the card, live playing animation, and background BroadcastReceiver
 * buttons that do NOT open the app when clicked).
 */
object DragonSystemAudioNotificationHelper {
    private const val CHANNEL_ID = "dragon_bg_audio_custom_deck_channel"
    private const val NOTIFICATION_ID = 4092

    const val ACTION_BG_EXPAND_PLAYER = "com.android.dragonvideo.action.BG_EXPAND_PLAYER"
    const val ACTION_BG_PLAY_PAUSE = "com.android.dragonvideo.action.BG_PLAY_PAUSE"
    const val ACTION_BG_REWIND = "com.android.dragonvideo.action.BG_REWIND"
    const val ACTION_BG_FORWARD = "com.android.dragonvideo.action.BG_FORWARD"
    const val ACTION_BG_PREV = "com.android.dragonvideo.action.BG_PREV"
    const val ACTION_BG_NEXT = "com.android.dragonvideo.action.BG_NEXT"
    const val ACTION_BG_CYCLE_STYLE = "com.android.dragonvideo.action.BG_CYCLE_STYLE"
    const val ACTION_BG_SEEK_FRACTION = "com.android.dragonvideo.action.BG_SEEK_FRACTION"
    const val ACTION_BG_STOP = "com.android.dragonvideo.action.BG_STOP"
    const val EXTRA_SEEK_FRACTION = "extra_seek_fraction"

    interface BackgroundActionListener {
        fun onTogglePlayPause()
        fun onSeekRelative(deltaMs: Long)
        fun onSeekFraction(fraction: Float)
        fun onPlayPrevious()
        fun onPlayNext()
        fun onCycleStyle()
        fun onStopBackgroundAudio()
    }

    @Volatile
    private var actionListener: BackgroundActionListener? = null

    private val reusableCardBitmap: Bitmap = Bitmap.createBitmap(960, 440, Bitmap.Config.ARGB_8888)
    private val reusableCardCanvas: AndroidCanvas = AndroidCanvas(reusableCardBitmap)

    fun getDeviceMaxRefreshRateHz(context: Context): Float {
        return runCatching {
            val dm = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
            val display = dm?.getDisplay(Display.DEFAULT_DISPLAY)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && display != null) {
                val maxModeRate = display.supportedModes?.maxOfOrNull { it.refreshRate }
                maxModeRate ?: display.refreshRate
            } else {
                display?.refreshRate ?: 60f
            }
        }.getOrDefault(60f).coerceIn(60f, 240f)
    }

    fun getDeviceDefaultRefreshRateHz(context: Context): Float = getDeviceMaxRefreshRateHz(context)

    fun registerActionListener(listener: BackgroundActionListener) {
        actionListener = listener
    }

    fun dispatchBackgroundAction(action: String, seekFraction: Float = -1f) {
        val listener = actionListener ?: return
        when (action) {
            ACTION_BG_PLAY_PAUSE -> listener.onTogglePlayPause()
            ACTION_BG_REWIND -> listener.onSeekRelative(-10_000L)
            ACTION_BG_FORWARD -> listener.onSeekRelative(10_000L)
            ACTION_BG_PREV -> listener.onPlayPrevious()
            ACTION_BG_NEXT -> listener.onPlayNext()
            ACTION_BG_CYCLE_STYLE -> listener.onCycleStyle()
            ACTION_BG_SEEK_FRACTION -> if (seekFraction in 0f..1f) listener.onSeekFraction(seekFraction)
            ACTION_BG_STOP -> listener.onStopBackgroundAudio()
        }
    }

    fun showOrUpdateBackgroundNotification(
        context: Context,
        video: VideoEntity,
        isPlaying: Boolean,
        currentPositionMs: Long,
        durationMs: Long,
        volumeBoostPercent: Int,
        themeConfig: DragonThemeConfig,
        animPhase: Float = 0f
    ) {
        ensureChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val cardBitmap = renderExactCardBitmap(
            video = video,
            isPlaying = isPlaying,
            currentPositionMs = currentPositionMs,
            durationMs = durationMs,
            volumeBoostPercent = volumeBoostPercent,
            themeConfig = themeConfig,
            animPhase = animPhase
        )

        val remoteViews = RemoteViews(context.packageName, R.layout.notification_dragon_bg_audio)
        remoteViews.setImageViewBitmap(R.id.notif_card_bg_image, cardBitmap)

        // Open Video Player PendingIntent (ONLY used when clicking the video area or "Video" button)
        val openVideoPending = PendingIntent.getActivity(
            context,
            200,
            Intent(context, MainActivity::class.java).apply {
                action = ACTION_BG_EXPAND_PLAYER
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Background Broadcast PendingIntents (Functional buttons DO NOT open the app!)
        val stylePending = buildBroadcastPendingIntent(context, ACTION_BG_CYCLE_STYLE, 201)
        val closePending = buildBroadcastPendingIntent(context, ACTION_BG_STOP, 202)
        val prevPending = buildBroadcastPendingIntent(context, ACTION_BG_PREV, 203)
        val rewindPending = buildBroadcastPendingIntent(context, ACTION_BG_REWIND, 204)
        val playPausePending = buildBroadcastPendingIntent(context, ACTION_BG_PLAY_PAUSE, 205)
        val forwardPending = buildBroadcastPendingIntent(context, ACTION_BG_FORWARD, 206)
        val nextPending = buildBroadcastPendingIntent(context, ACTION_BG_NEXT, 207)

        val seek0Pending = buildSeekBroadcastPendingIntent(context, 0.0f, 210)
        val seek1Pending = buildSeekBroadcastPendingIntent(context, 0.12f, 211)
        val seek2Pending = buildSeekBroadcastPendingIntent(context, 0.32f, 212)
        val seek3Pending = buildSeekBroadcastPendingIntent(context, 0.50f, 213)
        val seek4Pending = buildSeekBroadcastPendingIntent(context, 0.72f, 214)
        val seek5Pending = buildSeekBroadcastPendingIntent(context, 0.90f, 215)

        // Row 1 Hit Targets
        remoteViews.setOnClickPendingIntent(R.id.notif_hit_header_left, openVideoPending)
        remoteViews.setOnClickPendingIntent(R.id.notif_hit_style_pill, stylePending)
        remoteViews.setOnClickPendingIntent(R.id.notif_hit_video_pill, openVideoPending)
        remoteViews.setOnClickPendingIntent(R.id.notif_hit_close_btn, closePending)

        // Row 2 Hit Targets
        remoteViews.setOnClickPendingIntent(R.id.notif_hit_media_info, openVideoPending)
        remoteViews.setOnClickPendingIntent(R.id.notif_hit_visualizer, stylePending)

        // Row 3 Scrubber Hit Targets
        remoteViews.setOnClickPendingIntent(R.id.notif_hit_time_left, seek0Pending)
        remoteViews.setOnClickPendingIntent(R.id.notif_hit_seek_1, seek1Pending)
        remoteViews.setOnClickPendingIntent(R.id.notif_hit_seek_2, seek2Pending)
        remoteViews.setOnClickPendingIntent(R.id.notif_hit_seek_3, seek3Pending)
        remoteViews.setOnClickPendingIntent(R.id.notif_hit_seek_4, seek4Pending)
        remoteViews.setOnClickPendingIntent(R.id.notif_hit_seek_5, seek5Pending)
        remoteViews.setOnClickPendingIntent(R.id.notif_hit_time_right, openVideoPending)

        // Row 4 Functional Transport Buttons (Do NOT open app!)
        remoteViews.setOnClickPendingIntent(R.id.notif_hit_prev, prevPending)
        remoteViews.setOnClickPendingIntent(R.id.notif_hit_rewind, rewindPending)
        remoteViews.setOnClickPendingIntent(R.id.notif_hit_play_pause, playPausePending)
        remoteViews.setOnClickPendingIntent(R.id.notif_hit_forward, forwardPending)
        remoteViews.setOnClickPendingIntent(R.id.notif_hit_next, nextPending)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setCustomContentView(remoteViews)
            .setCustomBigContentView(remoteViews)
            .setCustomHeadsUpContentView(remoteViews)
            .setOngoing(isPlaying)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openVideoPending)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        }
    }

    fun cancelBackgroundNotification(context: Context) {
        runCatching {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        }
    }

    private fun buildBroadcastPendingIntent(
        context: Context,
        action: String,
        requestCode: Int
    ): PendingIntent {
        val intent = Intent(context, DragonNotificationActionReceiver::class.java).apply {
            this.action = action
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun buildSeekBroadcastPendingIntent(
        context: Context,
        fraction: Float,
        requestCode: Int
    ): PendingIntent {
        val intent = Intent(context, DragonNotificationActionReceiver::class.java).apply {
            this.action = ACTION_BG_SEEK_FRACTION
            putExtra(EXTRA_SEEK_FRACTION, fraction)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Dragon Background Audio Player",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Custom in-app style notification deck for background audio playback"
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
            }
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * Renders the exact 4-row card from the in-app background audio player onto a high-DPI Bitmap
     * so the Android System Notification Panel looks 100% identical to the user's screenshot.
     */
    private fun renderExactCardBitmap(
        video: VideoEntity,
        isPlaying: Boolean,
        currentPositionMs: Long,
        durationMs: Long,
        volumeBoostPercent: Int,
        themeConfig: DragonThemeConfig,
        animPhase: Float
    ): Bitmap = synchronized(reusableCardBitmap) {
        val w = 960f
        val h = 440f
        val bmp = reusableCardBitmap
        val canvas = reusableCardCanvas
        canvas.drawColor(0x00000000, PorterDuff.Mode.CLEAR)

        val bgArgb = themeConfig.effectiveBackground.toArgb()
        val surfArgb = themeConfig.effectiveSurface.toArgb()
        val primaryArgb = themeConfig.activePrimary.toArgb()
        val secondaryArgb = themeConfig.activeSecondary.toArgb()
        val tertiaryArgb = themeConfig.activeTertiaryGlow.toArgb()

        val monoBold = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        val monoReg = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        val sansBold = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)

        val cardRect = RectF(6f, 6f, w - 6f, h - 6f)
        val cardRadius = 38f

        // 1. Deep Obsidian / Theme Gradient Card Background
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, w, h,
                intArrayOf(bgArgb, surfArgb, bgArgb),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(cardRect, cardRadius, cardRadius, bgPaint)

        // 2. Center Radial Crimson/Theme Glow (matching screenshot center glow)
        val glowColorCenter = (primaryArgb and 0x00FFFFFF) or 0x42000000
        val glowColorMid = (secondaryArgb and 0x00FFFFFF) or 0x18000000
        val radialPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                w * 0.50f, h * 0.52f, h * 0.56f,
                intArrayOf(glowColorCenter, glowColorMid, 0x00000000),
                floatArrayOf(0f, 0.65f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(cardRect, cardRadius, cardRadius, radialPaint)

        // 3. Subtle Dragon-Scale Mesh Arcs across the card
        val scalePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.6f
            color = (primaryArgb and 0x00FFFFFF) or 0x1E000000
        }
        val stepX = 76f
        val stepY = 44f
        var rowIdx = 0
        var sy = 14f
        while (sy < h - 14f) {
            val offsetX = if (rowIdx % 2 == 0) 0f else stepX / 2f
            var sx = -stepX + offsetX
            while (sx < w + stepX) {
                val arcRect = RectF(sx, sy - stepY * 0.5f, sx + stepX, sy + stepY * 0.5f)
                canvas.drawArc(arcRect, 15f, 150f, false, scalePaint)
                sx += stepX
            }
            sy += stepY
            rowIdx++
        }

        // 4. Outer Neon Gradient Border
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3.2f
            shader = LinearGradient(
                0f, 0f, w, h,
                intArrayOf(primaryArgb, secondaryArgb, tertiaryArgb),
                floatArrayOf(0f, 0.6f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(cardRect, cardRadius, cardRadius, borderPaint)

        // =========================================================================
        // ROW 1 (y = 0..24%, center cy = 56f):
        // [Headphone + BG AUDIO • DRAGON...] | [✨ Dragon Pulse] | [↗ Video] | [✕]
        // =========================================================================
        val row1Cy = 56f

        // Headphone Circle Badge (cx = 56f, cy = 56f, r = 23f)
        val badgeCx = 56f
        val badgeR = 23f
        val badgeGradPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                badgeCx - badgeR, row1Cy - badgeR, badgeCx + badgeR, row1Cy + badgeR,
                primaryArgb, secondaryArgb, Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(badgeCx, row1Cy, badgeR, badgeGradPaint)

        // Black Headphone Icon inside Circle Badge
        val hpPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF08080C.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 3.2f
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawArc(RectF(badgeCx - 10f, row1Cy - 10f, badgeCx + 10f, row1Cy + 8f), 180f, 180f, false, hpPaint)
        val hpFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF08080C.toInt()
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(badgeCx - 12f, row1Cy - 2f, badgeCx - 6.5f, row1Cy + 9f), 2.5f, 2.5f, hpFillPaint)
        canvas.drawRoundRect(RectF(badgeCx + 6.5f, row1Cy - 2f, badgeCx + 12f, row1Cy + 9f), 2.5f, 2.5f, hpFillPaint)

        // "BG AUDIO • DRAGON..." Label
        val headerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryArgb
            typeface = monoBold
            textSize = 21f
            letterSpacing = 0.05f
        }
        val presetShort = themeConfig.preset.title.uppercase().take(7)
        canvas.drawText("BG AUDIO • $presetShort...", 92f, row1Cy + 7.5f, headerTextPaint)

        // Style Pill: [✨ Dragon Pulse] at x = 404f..666f (matches weight 40..70%)
        val stylePillRect = RectF(404f, 30f, 666f, 82f)
        val stylePillBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = (primaryArgb and 0x00FFFFFF) or 0x29000000
            style = Paint.Style.FILL
        }
        val stylePillStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = (primaryArgb and 0x00FFFFFF) or 0x7A000000
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRoundRect(stylePillRect, 26f, 26f, stylePillBgPaint)
        canvas.drawRoundRect(stylePillRect, 26f, 26f, stylePillStrokePaint)

        // Sparkle icon (✨) inside Style Pill
        drawSparkleStar(canvas, 434f, row1Cy - 2f, 9f, tertiaryArgb)
        drawSparkleStar(canvas, 446f, row1Cy + 7f, 5f, tertiaryArgb)

        val pillTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            typeface = monoBold
            textSize = 20.5f
        }
        canvas.drawText(themeConfig.notifPanelStyle.label, 462f, row1Cy + 7f, pillTextPaint)

        // Video Pill: [↗ Video] at x = 682f..842f (matches weight 70..88%)
        val videoPillRect = RectF(682f, 30f, 842f, 82f)
        val videoPillBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = (secondaryArgb and 0x00FFFFFF) or 0x3D000000
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(videoPillRect, 26f, 26f, videoPillBgPaint)

        // Diagonal Expand Arrow icon inside Video Pill
        val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryArgb
            style = Paint.Style.STROKE
            strokeWidth = 3f
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(706f, row1Cy + 8f, 724f, row1Cy - 10f, arrowPaint)
        canvas.drawLine(714f, row1Cy - 10f, 724f, row1Cy - 10f, arrowPaint)
        canvas.drawLine(724f, row1Cy - 10f, 724f, row1Cy, arrowPaint)
        canvas.drawLine(706f, row1Cy + 8f, 716f, row1Cy + 8f, arrowPaint)
        canvas.drawLine(706f, row1Cy + 8f, 706f, row1Cy - 2f, arrowPaint)

        canvas.drawText("Video", 738f, row1Cy + 7f, pillTextPaint)

        // Close Button [✕] at cx = 896f, cy = 56f (matches weight 88..100%)
        val closePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFC8BED2.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 3.4f
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(885f, row1Cy - 11f, 907f, row1Cy + 11f, closePaint)
        canvas.drawLine(907f, row1Cy - 11f, 885f, row1Cy + 11f, closePaint)

        // =========================================================================
        // ROW 2 (y = 24..57%, y = 108f..248f):
        // [Video Thumbnail with 4K UHD & Duration] | [Title + Codec] | [Playing Animation]
        // =========================================================================
        val thumbRect = RectF(42f, 114f, 224f, 236f)
        val cachedThumb = StorageScannerHelper.getCachedVideoThumbnail(video)

        if (cachedThumb != null && !cachedThumb.isRecycled) {
            val shader = BitmapShader(cachedThumb, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            val matrix = Matrix()
            val scale = maxOf(
                thumbRect.width() / cachedThumb.width.toFloat(),
                thumbRect.height() / cachedThumb.height.toFloat()
            )
            matrix.setScale(scale, scale)
            matrix.postTranslate(
                thumbRect.left + (thumbRect.width() - cachedThumb.width * scale) * 0.5f,
                thumbRect.top + (thumbRect.height() - cachedThumb.height * scale) * 0.5f
            )
            shader.setLocalMatrix(matrix)
            val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.shader = shader }
            canvas.drawRoundRect(thumbRect, 20f, 20f, thumbPaint)
        } else {
            val fallbackThumbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    thumbRect.left, thumbRect.top, thumbRect.right, thumbRect.bottom,
                    intArrayOf(0xFF16222A.toInt(), 0xFF0A1016.toInt()),
                    null,
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRoundRect(thumbRect, 20f, 20f, fallbackThumbPaint)
        }

        // Subtle dark scrim on thumbnail so badges & play icon pop
        val scrimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x48000000
        }
        canvas.drawRoundRect(thumbRect, 20f, 20f, scrimPaint)

        // Thumbnail Neon Border
        val thumbBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.8f
            shader = LinearGradient(
                thumbRect.left, thumbRect.top, thumbRect.right, thumbRect.bottom,
                primaryArgb, secondaryArgb, Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(thumbRect, 20f, 20f, thumbBorderPaint)

        // Top-left Resolution Badge ("4K UHD" / "1080p") inside Thumbnail
        val resBadgeRect = RectF(thumbRect.left + 8f, thumbRect.top + 8f, thumbRect.left + 98f, thumbRect.top + 36f)
        val badgeDarkBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xB8050508.toInt() }
        canvas.drawRoundRect(resBadgeRect, 8f, 8f, badgeDarkBg)
        val resTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryArgb
            typeface = monoBold
            textSize = 16.5f
        }
        canvas.drawText(video.resolutionBadge.take(7), resBadgeRect.left + 9f, resBadgeRect.bottom - 7.5f, resTextPaint)

        // Center Play Triangle inside Thumbnail
        val thumbCx = thumbRect.centerX()
        val thumbCy = thumbRect.centerY()
        canvas.drawCircle(thumbCx, thumbCy, 20f, badgeDarkBg)
        val playTriPath = AndroidPath().apply {
            moveTo(thumbCx - 5f, thumbCy - 9f)
            lineTo(thumbCx + 9f, thumbCy)
            lineTo(thumbCx - 5f, thumbCy + 9f)
            close()
        }
        val whiteFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            style = Paint.Style.FILL
        }
        canvas.drawPath(playTriPath, whiteFill)

        // Bottom-right Duration Badge inside Thumbnail
        val durString = StorageScannerHelper.formatDurationMs(durationMs.coerceAtLeast(1L))
        val durBadgeRect = RectF(thumbRect.right - 88f, thumbRect.bottom - 34f, thumbRect.right - 8f, thumbRect.bottom - 6f)
        canvas.drawRoundRect(durBadgeRect, 8f, 8f, badgeDarkBg)
        val durBadgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            typeface = monoBold
            textSize = 17f
        }
        canvas.drawText(durString, durBadgeRect.left + 10f, durBadgeRect.bottom - 7.5f, durBadgeTextPaint)

        // Video Title & Metadata in Center (x = 248f..724f)
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            typeface = sansBold
            textSize = 33f
        }
        val clippedTitle = ellipsizeText(video.title, titlePaint, 470f)
        canvas.drawText(clippedTitle, 248f, 166f, titlePaint)

        val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFA69EB4.toInt()
            typeface = monoReg
            textSize = 21.5f
        }
        val metaRaw = "${video.containerFormat} • ${video.audioCodec} • Boost ${volumeBoostPercent}%"
        val clippedMeta = ellipsizeText(metaRaw, metaPaint, 470f)
        canvas.drawText(clippedMeta, 248f, 210f, metaPaint)

        // Right-side Live Playing Animation (x = 744f..918f, y = 128f..222f)
        drawNotificationPlayingAnimation(
            canvas = canvas,
            notifStyle = themeConfig.notifPanelStyle,
            isPlaying = isPlaying,
            phase = animPhase,
            left = 744f,
            top = 128f,
            right = 918f,
            bottom = 222f,
            primaryArgb = primaryArgb,
            secondaryArgb = secondaryArgb,
            tertiaryArgb = tertiaryArgb
        )

        // =========================================================================
        // ROW 3 (y = 57..72%, cy = 284f):
        // [00:45] | [Active Capsule + Vertical Cursor + Inactive Capsule with Dot] | [02:36]
        // =========================================================================
        val row3Cy = 284f
        val safeDur = durationMs.coerceAtLeast(1L)
        val progress = (currentPositionMs.toFloat() / safeDur.toFloat()).coerceIn(0f, 1f)

        val posTimePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryArgb
            typeface = monoBold
            textSize = 22f
        }
        canvas.drawText(StorageScannerHelper.formatDurationMs(currentPositionMs), 42f, row3Cy + 8f, posTimePaint)

        val totalTimePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFB5AEC2.toInt()
            typeface = monoBold
            textSize = 22f
        }
        canvas.drawText(durString, 836f, row3Cy + 8f, totalTimePaint)

        // Split-Capsule Progress Scrubber (x = 152f..812f)
        val barLeft = 152f
        val barRight = 812f
        val barW = barRight - barLeft
        val trackH = 22f
        val trackTop = row3Cy - trackH / 2f
        val trackBot = row3Cy + trackH / 2f
        val cursorX = barLeft + barW * progress.coerceIn(0.03f, 0.97f)
        val gap = 12f

        // Active Left Capsule
        val activeRight = (cursorX - gap).coerceAtLeast(barLeft + trackH)
        val activeCapsulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                barLeft, row3Cy, activeRight, row3Cy,
                primaryArgb, secondaryArgb, Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(RectF(barLeft, trackTop, activeRight, trackBot), trackH / 2f, trackH / 2f, activeCapsulePaint)

        // Inactive Right Capsule
        val inactiveLeft = (cursorX + gap).coerceAtMost(barRight)
        if (barRight - inactiveLeft > 6f) {
            val inactivePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0x33FFFFFF
            }
            canvas.drawRoundRect(RectF(inactiveLeft, trackTop, barRight, trackBot), trackH / 2f, trackH / 2f, inactivePaint)

            // Glowing Primary Dot at right end of inactive capsule
            val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = primaryArgb }
            canvas.drawCircle((barRight - 14f).coerceAtLeast(inactiveLeft + 6f), row3Cy, 5f, dotPaint)
        }

        // Vertical Glowing Pill Cursor Bar
        val cursorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = primaryArgb }
        canvas.drawRoundRect(
            RectF(cursorX - 4f, row3Cy - 19f, cursorX + 4f, row3Cy + 19f),
            4f,
            4f,
            cursorPaint
        )

        // =========================================================================
        // ROW 4 (y = 72..100%, cy = 376f):
        // Transport Controls: [|◄] | [↺ 10] | [Glowing || / ▶] | [↻ 10] | [►|]
        // =========================================================================
        val row4Cy = 376f

        // 1. Previous Track [|◄] at cx = 152f
        drawSkipPreviousIcon(canvas, 152f, row4Cy, 0xFFFFFFFF.toInt())

        // 2. Rewind 10s [↺ 10] at cx = 316f
        drawTenSecondSeekIcon(canvas, 316f, row4Cy, isForward = false, colorArgb = secondaryArgb, monoBold = monoBold)

        // 3. Center Glowing Play/Pause Button at cx = 480f, cy = 376f
        val playBoxRect = RectF(430f, row4Cy - 44f, 530f, row4Cy + 44f)
        val playBoxFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                playBoxRect.left, playBoxRect.top, playBoxRect.right, playBoxRect.bottom,
                primaryArgb, secondaryArgb, Shader.TileMode.CLAMP
            )
        }
        val playBoxStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = tertiaryArgb
        }
        canvas.drawRoundRect(playBoxRect, 26f, 26f, playBoxFill)
        canvas.drawRoundRect(playBoxRect, 26f, 26f, playBoxStroke)

        val darkIconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF0A0A0E.toInt()
            style = Paint.Style.FILL
        }
        if (isPlaying) {
            // Pause Bars (||)
            canvas.drawRoundRect(RectF(465f, row4Cy - 19f, 475f, row4Cy + 19f), 3f, 3f, darkIconPaint)
            canvas.drawRoundRect(RectF(485f, row4Cy - 19f, 495f, row4Cy + 19f), 3f, 3f, darkIconPaint)
        } else {
            // Play Triangle (▶)
            val playPath = AndroidPath().apply {
                moveTo(468f, row4Cy - 20f)
                lineTo(498f, row4Cy)
                lineTo(468f, row4Cy + 20f)
                close()
            }
            canvas.drawPath(playPath, darkIconPaint)
        }

        // 4. Forward 10s [↻ 10] at cx = 644f
        drawTenSecondSeekIcon(canvas, 644f, row4Cy, isForward = true, colorArgb = secondaryArgb, monoBold = monoBold)

        // 5. Next Track [►|] at cx = 808f
        drawSkipNextIcon(canvas, 808f, row4Cy, 0xFFFFFFFF.toInt())

        return bmp
    }

    private fun drawNotificationPlayingAnimation(
        canvas: AndroidCanvas,
        notifStyle: DragonNotifPanelStyle,
        isPlaying: Boolean,
        phase: Float,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        primaryArgb: Int,
        secondaryArgb: Int,
        tertiaryArgb: Int
    ) {
        val w = right - left
        val h = bottom - top
        val cy = (top + bottom) / 2f

        when (notifStyle) {
            DragonNotifPanelStyle.DRAGON_PULSE, DragonNotifPanelStyle.MINIMAL_GLOW -> {
                val path1 = AndroidPath()
                val path2 = AndroidPath()
                val steps = 60
                val amp = if (isPlaying) h * 0.38f else h * 0.16f
                for (s in 0..steps) {
                    val sRatio = s.toFloat() / steps
                    val x = left + sRatio * w
                    val wave1 = sin(phase + s * 0.28f) * 0.76f + sin(phase * 0.55f + s * 0.12f) * 0.24f
                    val wave2 = sin(phase * 1.15f + s * 0.34f + 1.25f) * 0.70f + cos(phase * 0.70f + s * 0.16f) * 0.30f
                    val y1 = cy + wave1 * amp
                    val y2 = cy + wave2 * (amp * 0.78f)
                    if (s == 0) {
                        path1.moveTo(x, y1)
                        path2.moveTo(x, y2)
                    } else {
                        path1.lineTo(x, y1)
                        path2.lineTo(x, y2)
                    }
                }
                val paint2 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 4.2f
                    strokeCap = Paint.Cap.ROUND
                    color = (secondaryArgb and 0x00FFFFFF) or 0x99000000.toInt()
                }
                val paint1 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 5.6f
                    strokeCap = Paint.Cap.ROUND
                    shader = LinearGradient(
                        left, cy - amp, right, cy + amp,
                        intArrayOf(primaryArgb, secondaryArgb, tertiaryArgb),
                        null,
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawPath(path2, paint2)
                canvas.drawPath(path1, paint1)
            }

            DragonNotifPanelStyle.SPECTRUM_BARS -> {
                val barCount = 8
                val gap = 7f
                val barW = ((w - gap * (barCount - 1)) / barCount).coerceAtLeast(6f)
                val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.FILL
                    shader = LinearGradient(
                        left, top, left, bottom,
                        intArrayOf(tertiaryArgb, primaryArgb, secondaryArgb),
                        null,
                        Shader.TileMode.CLAMP
                    )
                }
                for (i in 0 until barCount) {
                    val factor = if (isPlaying) {
                        val harmonic1 = sin(phase * 1.2f + i * 0.68f) * 0.24f
                        val harmonic2 = cos(phase * 0.72f + i * 1.05f) * 0.16f
                        (0.52f + harmonic1 + harmonic2).coerceIn(0.18f, 0.96f)
                    } else {
                        0.24f
                    }
                    val bh = (h * factor).coerceAtLeast(barW)
                    val bx = left + i * (barW + gap)
                    val by = bottom - bh
                    canvas.drawRoundRect(RectF(bx, by, bx + barW, bottom), barW / 2f, barW / 2f, barPaint)
                }
            }

            DragonNotifPanelStyle.CYBER_VINYL -> {
                val cx = (left + right) / 2f
                val maxR = minOf(w, h) * 0.45f
                val pulse = if (isPlaying) (sin(phase) * 0.14f + 0.86f) else 0.75f
                val glowRing = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.FILL
                    color = (primaryArgb and 0x00FFFFFF) or 0x44000000
                }
                val strokeRing = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 4.5f
                    shader = LinearGradient(
                        cx - maxR, cy - maxR, cx + maxR, cy + maxR,
                        intArrayOf(primaryArgb, tertiaryArgb, secondaryArgb),
                        null,
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawCircle(cx, cy, maxR * pulse, glowRing)
                canvas.drawCircle(cx, cy, maxR * 0.74f, strokeRing)
                val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = tertiaryArgb }
                canvas.drawCircle(cx, cy, 6f, dotPaint)
            }
        }
    }

    private fun drawSparkleStar(canvas: AndroidCanvas, cx: Float, cy: Float, r: Float, colorArgb: Int) {
        val path = AndroidPath().apply {
            moveTo(cx, cy - r)
            quadTo(cx, cy, cx + r, cy)
            quadTo(cx, cy, cx, cy + r)
            quadTo(cx, cy, cx - r, cy)
            quadTo(cx, cy, cx, cy - r)
            close()
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorArgb
            style = Paint.Style.FILL
        }
        canvas.drawPath(path, paint)
    }

    private fun drawSkipPreviousIcon(canvas: AndroidCanvas, cx: Float, cy: Float, colorArgb: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorArgb
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(cx - 16f, cy - 16f, cx - 10f, cy + 16f), 2f, 2f, paint)
        val tri = AndroidPath().apply {
            moveTo(cx - 8f, cy)
            lineTo(cx + 16f, cy - 16f)
            lineTo(cx + 16f, cy + 16f)
            close()
        }
        canvas.drawPath(tri, paint)
    }

    private fun drawSkipNextIcon(canvas: AndroidCanvas, cx: Float, cy: Float, colorArgb: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorArgb
            style = Paint.Style.FILL
        }
        val tri = AndroidPath().apply {
            moveTo(cx + 8f, cy)
            lineTo(cx - 16f, cy - 16f)
            lineTo(cx - 16f, cy + 16f)
            close()
        }
        canvas.drawPath(tri, paint)
        canvas.drawRoundRect(RectF(cx + 10f, cy - 16f, cx + 16f, cy + 16f), 2f, 2f, paint)
    }

    private fun drawTenSecondSeekIcon(
        canvas: AndroidCanvas,
        cx: Float,
        cy: Float,
        isForward: Boolean,
        colorArgb: Int,
        monoBold: Typeface
    ) {
        val r = 21f
        val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorArgb
            style = Paint.Style.STROKE
            strokeWidth = 3.8f
            strokeCap = Paint.Cap.ROUND
        }
        val oval = RectF(cx - r, cy - r, cx + r, cy + r)
        if (isForward) {
            canvas.drawArc(oval, -60f, 300f, false, arcPaint)
            // Clockwise arrowhead at top
            val headPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = colorArgb
                style = Paint.Style.FILL
            }
            val head = AndroidPath().apply {
                moveTo(cx - 2f, cy - r - 7f)
                lineTo(cx + 11f, cy - r + 1f)
                lineTo(cx - 2f, cy - r + 8f)
                close()
            }
            canvas.drawPath(head, headPaint)
        } else {
            canvas.drawArc(oval, -240f, 300f, false, arcPaint)
            // Counter-clockwise arrowhead at top
            val headPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = colorArgb
                style = Paint.Style.FILL
            }
            val head = AndroidPath().apply {
                moveTo(cx + 2f, cy - r - 7f)
                lineTo(cx - 11f, cy - r + 1f)
                lineTo(cx + 2f, cy - r + 8f)
                close()
            }
            canvas.drawPath(head, headPaint)
        }

        val numPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorArgb
            typeface = monoBold
            textSize = 15.5f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("10", cx, cy + 6f, numPaint)
    }

    private fun ellipsizeText(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var end = text.length
        while (end > 1 && paint.measureText(text.substring(0, end) + "...") > maxWidth) {
            end--
        }
        return text.substring(0, end) + "..."
    }
}
