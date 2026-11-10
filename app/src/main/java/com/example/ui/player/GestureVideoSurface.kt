package com.example.ui.player

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.domain.model.AspectRatioMode
import com.example.domain.model.SubtitleStyleConfig
import com.example.player.StorageScannerHelper
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.dragonGlassSurface
import com.example.ui.viewmodel.readDeviceBrightnessFraction
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt

enum class ActiveSwipeMode {
    NONE,
    BRIGHTNESS_LEFT,
    VOLUME_RIGHT,
    SLIDE_SEEK,
    DOUBLE_TAP_REWIND,
    DOUBLE_TAP_FORWARD
}

@OptIn(UnstableApi::class)
@Composable
fun GestureVideoSurface(
    exoPlayer: ExoPlayer,
    aspectRatioMode: AspectRatioMode,
    zoomScale: Float,
    panOffsetX: Float,
    panOffsetY: Float,
    isHudLocked: Boolean,
    screenBrightness: Float,
    isBrightnessOverridden: Boolean,
    volumeBoostPercent: Int,
    currentPositionMs: Long,
    durationMs: Long,
    isLongPress2xActive: Boolean,
    activeSubtitleCue: String?,
    subtitleStyle: SubtitleStyleConfig,
    isFloatingSubtitleControlOpen: Boolean,
    onSingleTapToggleHud: () -> Unit,
    onDoubleTapSeek: (deltaMs: Long) -> Unit,
    onLongPressBurstChanged: (active: Boolean) -> Unit,
    onBrightnessChanged: (Float) -> Unit,
    onVolumeBoostChanged: (Int) -> Unit,
    onSeekToPosition: (Long) -> Unit,
    onZoomAndPanChanged: (scaleMult: Float, dx: Float, dy: Float) -> Unit,
    onResetZoom: () -> Unit,
    onUpdateSubtitleStyle: ((SubtitleStyleConfig) -> SubtitleStyleConfig) -> Unit,
    onCloseFloatingSubtitleControl: () -> Unit,
    onOpenFullSubtitleSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var surfaceSize by remember { mutableStateOf(IntSize(1920, 1080)) }
    var activeSwipeMode by remember { mutableStateOf(ActiveSwipeMode.NONE) }

    // Live local floats for zero-latency, continuous 60/120fps swipe response
    var liveBrightness by remember { mutableFloatStateOf(screenBrightness) }
    var liveVolumeFloat by remember { mutableFloatStateOf(volumeBoostPercent.toFloat()) }
    var liveSeekTargetMs by remember { mutableLongStateOf(currentPositionMs) }
    var liveSeekDeltaMs by remember { mutableLongStateOf(0L) }

    // Draggable & resizable floating subtitle position & width scale
    var subtitleOffsetX by remember { mutableFloatStateOf(0f) }
    var subtitleOffsetY by remember { mutableFloatStateOf(0f) }
    var subtitleBoxWidthFraction by remember { mutableFloatStateOf(0.84f) }

    // Keep updated references so pointerInput never restarts mid-gesture
    val latestBrightness by rememberUpdatedState(screenBrightness)
    val latestBrightnessOverridden by rememberUpdatedState(isBrightnessOverridden)
    val latestVolumeBoost by rememberUpdatedState(volumeBoostPercent)
    val latestPositionMs by rememberUpdatedState(currentPositionMs)
    val latestDurationMs by rememberUpdatedState(durationMs)
    val latestHudLocked by rememberUpdatedState(isHudLocked)

    // Sync external state into live display state when not actively swiping.
    // Keep window.attributes.screenBrightness = BRIGHTNESS_OVERRIDE_NONE (-1f) until the user
    // explicitly swipes to adjust brightness so the video starts at the exact device brightness.
    LaunchedEffect(screenBrightness, isBrightnessOverridden) {
        if (activeSwipeMode != ActiveSwipeMode.BRIGHTNESS_LEFT) {
            liveBrightness = if (isBrightnessOverridden) {
                screenBrightness
            } else {
                readDeviceBrightnessFraction(context)
            }
        }
        val activity = context as? Activity ?: return@LaunchedEffect
        val window = activity.window
        val params = window.attributes
        params.screenBrightness = if (isBrightnessOverridden) {
            screenBrightness.coerceIn(0.01f, 1.0f)
        } else {
            WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        }
        window.attributes = params
    }

    LaunchedEffect(volumeBoostPercent) {
        if (activeSwipeMode != ActiveSwipeMode.VOLUME_RIGHT) {
            liveVolumeFloat = volumeBoostPercent.toFloat()
        }
        runCatching {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return@runCatching
            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
            val targetSystemVol = ((volumeBoostPercent.coerceIn(0, 100) / 100f) * maxVol).roundToInt()
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetSystemVol, 0)
        }
    }

    LaunchedEffect(activeSwipeMode) {
        if (activeSwipeMode == ActiveSwipeMode.DOUBLE_TAP_REWIND ||
            activeSwipeMode == ActiveSwipeMode.DOUBLE_TAP_FORWARD
        ) {
            delay(550L)
            activeSwipeMode = ActiveSwipeMode.NONE
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .onSizeChanged { surfaceSize = it }
            .testTag("video_gesture_surface")
            // 1. Tap, Double-Tap (10s fast seek), and Long-Press (2x speed burst)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        onSingleTapToggleHud()
                    },
                    onDoubleTap = { offset ->
                        if (latestHudLocked) {
                            onSingleTapToggleHud()
                            return@detectTapGestures
                        }
                        val width = surfaceSize.width.coerceAtLeast(1)
                        if (offset.x < width * 0.42f) {
                            activeSwipeMode = ActiveSwipeTypeRewind()
                            onDoubleTapSeek(-10_000L)
                        } else if (offset.x > width * 0.58f) {
                            activeSwipeMode = ActiveSwipeTypeForward()
                            onDoubleTapSeek(10_000L)
                        } else {
                            onSingleTapToggleHud()
                        }
                    },
                    onLongPress = {
                        if (!latestHudLocked) {
                            onLongPressBurstChanged(true)
                        }
                    },
                    onPress = {
                        tryAwaitRelease()
                        onLongPressBurstChanged(false)
                    }
                )
            }
            // 2. Refined Ultra-Responsive Single-Finger Swipe (Brightness Left, Volume Right, Slide-to-Seek)
            //    + Two-Finger Pinch-to-Zoom
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (latestHudLocked) return@awaitEachGesture

                    val startX = down.position.x
                    var totalDx = 0f
                    var totalDy = 0f
                    var gestureMode = ActiveSwipeMode.NONE
                    val startPositionMs = latestPositionMs
                    val videoDurMs = latestDurationMs.coerceAtLeast(1_000L)

                    liveBrightness = if (latestBrightnessOverridden) {
                        latestBrightness
                    } else {
                        readDeviceBrightnessFraction(context)
                    }
                    liveVolumeFloat = latestVolumeBoost.toFloat()
                    liveSeekTargetMs = startPositionMs
                    liveSeekDeltaMs = 0L

                    val touchSlopPx = viewConfiguration.touchSlop * 0.65f

                    do {
                        val event = awaitPointerEvent()
                        val pointers = event.changes.filter { it.pressed }

                        // If 2+ fingers are down, handle smooth pinch-to-zoom & pan
                        if (pointers.size >= 2) {
                            if (gestureMode != ActiveSwipeMode.NONE) {
                                activeSwipeMode = ActiveSwipeMode.NONE
                                gestureMode = ActiveSwipeMode.NONE
                            }
                            val p1 = pointers[0]
                            val p2 = pointers[1]
                            val prevDist = (p1.previousPosition - p2.previousPosition).getDistance()
                            val currDist = (p1.position - p2.position).getDistance()
                            if (prevDist > 10f) {
                                val zoomFactor = (currDist / prevDist).coerceIn(0.85f, 1.18f)
                                val panDelta = (p1.positionChange() + p2.positionChange()) / 2f
                                onZoomAndPanChanged(zoomFactor, panDelta.x, panDelta.y)
                                p1.consume()
                                p2.consume()
                            }
                            continue
                        }

                        val change = event.changes.firstOrNull() ?: break
                        if (!change.pressed) break

                        val delta = change.positionChange()
                        totalDx += delta.x
                        totalDy += delta.y

                        val w = surfaceSize.width.coerceAtLeast(1).toFloat()
                        val h = surfaceSize.height.coerceAtLeast(1).toFloat()

                        if (gestureMode == ActiveSwipeMode.NONE) {
                            if (abs(totalDx) > touchSlopPx || abs(totalDy) > touchSlopPx) {
                                gestureMode = if (abs(totalDx) > abs(totalDy) * 1.2f) {
                                    ActiveSwipeMode.SLIDE_SEEK
                                } else if (startX < w * 0.5f) {
                                    // Left half of display -> Brightness up/down
                                    ActiveSwipeMode.BRIGHTNESS_LEFT
                                } else {
                                    // Right half of display -> Volume up/down
                                    ActiveSwipeMode.VOLUME_RIGHT
                                }
                                activeSwipeMode = gestureMode
                            }
                        }

                        when (gestureMode) {
                            ActiveSwipeMode.BRIGHTNESS_LEFT -> {
                                change.consume()
                                // Immediate incremental sensitivity: 55% screen height = full 0..100% range
                                val step = -delta.y / (h * 0.55f)
                                liveBrightness = (liveBrightness + step).coerceIn(0.0f, 1.0f)
                                onBrightnessChanged(liveBrightness)
                            }

                            ActiveSwipeMode.VOLUME_RIGHT -> {
                                change.consume()
                                // Immediate incremental sensitivity: 55% screen height = full 0..200% range
                                val step = (-delta.y / (h * 0.55f)) * 200f
                                liveVolumeFloat = (liveVolumeFloat + step).coerceIn(0f, 200f)
                                onVolumeBoostChanged(liveVolumeFloat.roundToInt())
                            }

                            ActiveSwipeMode.SLIDE_SEEK -> {
                                change.consume()
                                // Horizontal slide-to-seek: full width scrubs up to 120s (or full video if shorter)
                                val scrubSpanMs = minOf(videoDurMs, 120_000L).coerceAtLeast(15_000L)
                                val seekDelta = ((totalDx / (w * 0.75f)) * scrubSpanMs).toLong()
                                val target = (startPositionMs + seekDelta).coerceIn(0L, videoDurMs)
                                liveSeekTargetMs = target
                                liveSeekDeltaMs = target - startPositionMs
                                // Real-time scrubbing preview on player
                                onSeekToPosition(target)
                            }

                            else -> Unit
                        }
                    } while (event.changes.any { it.pressed })

                    if (gestureMode != ActiveSwipeMode.NONE) {
                        activeSwipeMode = ActiveSwipeMode.NONE
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Video Player Surface with Aspect Ratio + Pinch Zoom & Pan
        val aspectMod = when (aspectRatioMode) {
            AspectRatioMode.FILL_16_9 -> Modifier.aspectRatio(16f / 9f)
            AspectRatioMode.FILL_18_9 -> Modifier.aspectRatio(18f / 9f)
            AspectRatioMode.FILL_21_9 -> Modifier.aspectRatio(21f / 9f)
            else -> Modifier.fillMaxSize()
        }

        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    keepScreenOn = true
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    player = exoPlayer
                }
            },
            update = { view ->
                if (view.player != exoPlayer) {
                    view.player = exoPlayer
                }
                view.keepScreenOn = true
                view.resizeMode = when (aspectRatioMode) {
                    AspectRatioMode.FIT,
                    AspectRatioMode.FILL_16_9,
                    AspectRatioMode.FILL_18_9,
                    AspectRatioMode.FILL_21_9 -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                    AspectRatioMode.CROP -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    AspectRatioMode.STRETCH -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                }
            },
            modifier = aspectMod.graphicsLayer {
                scaleX = zoomScale
                scaleY = zoomScale
                translationX = panOffsetX
                translationY = panOffsetY
            }
        )

        // Draggable & Resizable Subtitle Display Box
        if ((!activeSubtitleCue.isNullOrBlank() || isFloatingSubtitleControlOpen) && subtitleStyle.enabled) {
            val displayCue = activeSubtitleCue ?: "Subtitle Preview • Drag to move or use resize slider"
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset { IntOffset(subtitleOffsetX.roundToInt(), subtitleOffsetY.roundToInt()) }
                    .padding(bottom = 72.dp, start = 16.dp, end = 16.dp)
                    .fillMaxWidth(subtitleBoxWidthFraction.coerceIn(0.45f, 0.98f))
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = subtitleStyle.backgroundOpacity))
                    .border(
                        width = if (isFloatingSubtitleControlOpen) 1.5.dp else 0.dp,
                        color = if (isFloatingSubtitleControlOpen) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            subtitleOffsetX = (subtitleOffsetX + pan.x).coerceIn(-500f, 500f)
                            subtitleOffsetY = (subtitleOffsetY + pan.y).coerceIn(-600f, 180f)
                            if (abs(zoom - 1f) > 0.01f) {
                                val newFontSize = (subtitleStyle.fontSizeSp * zoom).coerceIn(12f, 40f)
                                onUpdateSubtitleStyle { it.copy(fontSizeSp = newFontSize) }
                            }
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("subtitle_overlay_box"),
                contentAlignment = Alignment.Center
            ) {
                val shadowRadius = (subtitleStyle.outlineWidthDp * 3.5f).coerceAtLeast(1f)
                Text(
                    text = displayCue,
                    color = subtitleStyle.textColorChoice.color,
                    fontSize = subtitleStyle.fontSizeSp.sp,
                    fontFamily = subtitleStyle.fontChoice.toFontFamily(),
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                        shadow = Shadow(
                            color = Color.Black,
                            offset = Offset(subtitleStyle.outlineWidthDp, subtitleStyle.outlineWidthDp),
                            blurRadius = shadowRadius
                        )
                    )
                )
            }
        }

        // Floating Resizable Subtitle Controller Panel (Directly in Player)
        AnimatedVisibility(
            visible = isFloatingSubtitleControlOpen && !isHudLocked,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 20.dp)
        ) {
            FloatingSubtitleResizeController(
                config = subtitleStyle,
                boxWidthFraction = subtitleBoxWidthFraction,
                onUpdateConfig = onUpdateSubtitleStyle,
                onBoxWidthChange = { subtitleBoxWidthFraction = it.coerceIn(0.45f, 0.98f) },
                onResetPosition = {
                    subtitleOffsetX = 0f
                    subtitleOffsetY = 0f
                    subtitleBoxWidthFraction = 0.84f
                },
                onOpenAdvancedStudio = onOpenFullSubtitleSheet,
                onClose = onCloseFloatingSubtitleControl
            )
        }

        // Long-Press 2.0x Speed Burst Pill
        AnimatedVisibility(
            visible = isLongPress2xActive,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 24.dp)
        ) {
            Row(
                modifier = Modifier
                    .dragonGlassSurface(cornerRadiusOverride = 24.dp, elevated = true, accentGlow = true)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = "2x Speed Burst",
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "2.0x Speed",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White
                )
            }
        }

        // Pinch-to-Zoom Reset Pill
        AnimatedVisibility(
            visible = abs(zoomScale - 1.0f) > 0.03f && !isHudLocked,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 64.dp)
        ) {
            Row(
                modifier = Modifier
                    .dragonGlassSurface(cornerRadiusOverride = 20.dp, elevated = true)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ZoomIn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = "${(zoomScale * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White
                )
                TextButton(
                    onClick = onResetZoom,
                    modifier = Modifier.height(28.dp)
                ) {
                    Text("Reset", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // Left-Side Responsive Brightness Vertical Gauge (Appears when swiping left half up/down)
        AnimatedVisibility(
            visible = activeSwipeMode == ActiveSwipeMode.BRIGHTNESS_LEFT,
            enter = fadeIn(tween(80)),
            exit = fadeOut(tween(150)),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 28.dp)
        ) {
            ResponsiveVerticalSwipeBar(
                isBrightness = true,
                fraction = liveBrightness.coerceIn(0f, 1f),
                valueText = "${(liveBrightness * 100).roundToInt().coerceIn(0, 100)}%",
                isBoost = false
            )
        }

        // Right-Side Responsive Volume Vertical Gauge (Appears when swiping right half up/down)
        AnimatedVisibility(
            visible = activeSwipeMode == ActiveSwipeMode.VOLUME_RIGHT,
            enter = fadeIn(tween(80)),
            exit = fadeOut(tween(150)),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 28.dp)
        ) {
            val volInt = liveVolumeFloat.roundToInt().coerceIn(0, 200)
            ResponsiveVerticalSwipeBar(
                isBrightness = false,
                fraction = (liveVolumeFloat / 200f).coerceIn(0f, 1f),
                valueText = "$volInt%",
                isBoost = volInt > 100
            )
        }

        // Center Slide-to-Seek & Double-Tap Feedback Pill
        AnimatedVisibility(
            visible = activeSwipeMode == ActiveSwipeMode.SLIDE_SEEK ||
                activeSwipeMode == ActiveSwipeMode.DOUBLE_TAP_REWIND ||
                activeSwipeMode == ActiveSwipeMode.DOUBLE_TAP_FORWARD,
            enter = fadeIn(tween(80)),
            exit = fadeOut(tween(150)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            SlideSeekFeedbackCard(
                mode = activeSwipeMode,
                seekTargetMs = liveSeekTargetMs,
                seekDeltaMs = liveSeekDeltaMs,
                durationMs = durationMs
            )
        }
    }
}

private fun ActiveSwipeTypeRewind() = ActiveSwipeMode.DOUBLE_TAP_REWIND
private fun ActiveSwipeTypeForward() = ActiveSwipeMode.DOUBLE_TAP_FORWARD

@Composable
private fun ResponsiveVerticalSwipeBar(
    isBrightness: Boolean,
    fraction: Float,
    valueText: String,
    isBoost: Boolean
) {
    val scheme = MaterialTheme.colorScheme
    val smoothFraction by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 40),
        label = "swipe_bar_fill"
    )
    val activeColor = when {
        isBrightness -> scheme.tertiary
        isBoost -> scheme.primary
        else -> scheme.secondary
    }

    val icon = if (isBrightness) {
        when {
            fraction < 0.33f -> Icons.Default.BrightnessLow
            fraction < 0.68f -> Icons.Default.BrightnessMedium
            else -> Icons.Default.BrightnessHigh
        }
    } else {
        when {
            fraction <= 0.005f -> Icons.AutoMirrored.Filled.VolumeOff
            fraction < 0.35f -> Icons.AutoMirrored.Filled.VolumeDown
            else -> Icons.AutoMirrored.Filled.VolumeUp
        }
    }

    Column(
        modifier = Modifier
            .width(56.dp)
            .dragonGlassSurface(cornerRadiusOverride = 28.dp, elevated = true, accentGlow = true)
            .padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = activeColor,
            modifier = Modifier.size(22.dp)
        )

        Box(
            modifier = Modifier
                .width(10.dp)
                .height(140.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.16f)),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(smoothFraction)
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(activeColor, activeColor.copy(alpha = 0.7f))
                        )
                    )
            )
            if (!isBrightness) {
                // 100% standard volume marker at midpoint
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .height(1.5.dp)
                        .background(Color.White.copy(alpha = 0.65f))
                )
            }
        }

        Text(
            text = valueText,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = Color.White
        )
    }
}

@Composable
private fun SlideSeekFeedbackCard(
    mode: ActiveSwipeMode,
    seekTargetMs: Long,
    seekDeltaMs: Long,
    durationMs: Long
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .dragonGlassSurface(cornerRadiusOverride = 20.dp, elevated = true, accentGlow = true)
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        when (mode) {
            ActiveSwipeMode.SLIDE_SEEK -> {
                val sign = if (seekDeltaMs >= 0) "+" else "-"
                val deltaText = StorageScannerHelper.formatDurationMs(abs(seekDeltaMs))
                val targetText = StorageScannerHelper.formatDurationMs(seekTargetMs)
                val totalText = StorageScannerHelper.formatDurationMs(durationMs)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.width(190.dp)
                ) {
                    Text(
                        text = "$sign$deltaText",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = scheme.primary
                    )
                    Text(
                        text = "$targetText / $totalText",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White
                    )
                    LinearProgressIndicator(
                        progress = {
                            if (durationMs > 0L) (seekTargetMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                            else 0f
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(CircleShape),
                        color = scheme.primary,
                        trackColor = Color.White.copy(alpha = 0.18f)
                    )
                }
            }

            ActiveSwipeMode.DOUBLE_TAP_REWIND -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.FastRewind, contentDescription = null, tint = scheme.primary)
                    Text("-10s", style = MaterialTheme.typography.titleMedium, color = Color.White)
                }
            }

            ActiveSwipeMode.DOUBLE_TAP_FORWARD -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("+10s", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    Icon(Icons.Default.FastForward, contentDescription = null, tint = scheme.primary)
                }
            }

            else -> Unit
        }
    }
}

/**
 * Floating & Resizable Subtitle Controller Widget right inside the video player.
 */
@Composable
private fun FloatingSubtitleResizeController(
    config: SubtitleStyleConfig,
    boxWidthFraction: Float,
    onUpdateConfig: ((SubtitleStyleConfig) -> SubtitleStyleConfig) -> Unit,
    onBoxWidthChange: (Float) -> Unit,
    onResetPosition: () -> Unit,
    onOpenAdvancedStudio: () -> Unit,
    onClose: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    var panelOffsetX by remember { mutableFloatStateOf(0f) }
    var panelOffsetY by remember { mutableFloatStateOf(-40f) }

    Column(
        modifier = Modifier
            .offset { IntOffset(panelOffsetX.roundToInt(), panelOffsetY.roundToInt()) }
            .widthIn(min = 260.dp, max = 340.dp)
            .dragonGlassSurface(cornerRadiusOverride = 20.dp, elevated = true, accentGlow = true)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, _, _ ->
                    panelOffsetX = (panelOffsetX + pan.x).coerceIn(-350f, 350f)
                    panelOffsetY = (panelOffsetY + pan.y).coerceIn(-350f, 350f)
                }
            }
            .padding(14.dp)
            .testTag("floating_subtitle_controller"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header with Drag Handle, More Settings, and Close
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DragIndicator,
                    contentDescription = "Drag floating subtitle widget",
                    tint = scheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Icon(
                    imageVector = Icons.Default.Subtitles,
                    contentDescription = null,
                    tint = scheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Floating Subtitle Resize",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onOpenAdvancedStudio,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Detailed Subtitle Studio",
                        tint = scheme.tertiary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close floating subtitle panel",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // 1. Live Font Size Resize Slider (12sp - 40sp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Text Size", style = MaterialTheme.typography.labelMedium, color = Color.White)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    onClick = { onUpdateConfig { it.copy(fontSizeSp = (it.fontSizeSp - 2f).coerceIn(12f, 40f)) } },
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.12f)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Smaller text", tint = Color.White, modifier = Modifier.padding(4.dp).size(14.dp))
                }
                Text(
                    text = "${config.fontSizeSp.roundToInt()} sp",
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.primary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Surface(
                    onClick = { onUpdateConfig { it.copy(fontSizeSp = (it.fontSizeSp + 2f).coerceIn(12f, 40f)) } },
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.12f)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Larger text", tint = Color.White, modifier = Modifier.padding(4.dp).size(14.dp))
                }
            }
        }
        Slider(
            value = config.fontSizeSp,
            onValueChange = { s -> onUpdateConfig { it.copy(fontSizeSp = s) } },
            valueRange = 12f..40f,
            colors = SliderDefaults.colors(
                thumbColor = scheme.primary,
                activeTrackColor = scheme.primary
            ),
            modifier = Modifier.height(22.dp)
        )

        // 2. Subtitle Box Width / Container Resize Slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Default.OpenInFull, contentDescription = null, tint = scheme.secondary, modifier = Modifier.size(13.dp))
                Text("Box Width", style = MaterialTheme.typography.labelMedium, color = Color.White)
            }
            Text(
                text = "${(boxWidthFraction * 100).roundToInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = scheme.secondary
            )
        }
        Slider(
            value = boxWidthFraction,
            onValueChange = onBoxWidthChange,
            valueRange = 0.45f..0.98f,
            colors = SliderDefaults.colors(
                thumbColor = scheme.secondary,
                activeTrackColor = scheme.secondary
            ),
            modifier = Modifier.height(22.dp)
        )

        // 3. Quick Sync Offset & Reset Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    onClick = { onUpdateConfig { it.copy(syncOffsetMs = (it.syncOffsetMs - 100L).coerceIn(-5000L, 5000L)) } },
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White.copy(alpha = 0.1f)
                ) {
                    Text("-100ms", style = MaterialTheme.typography.labelSmall, color = Color.White, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
                Text(
                    text = "${if (config.syncOffsetMs >= 0) "+" else ""}${config.syncOffsetMs}ms",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.tertiary
                )
                Surface(
                    onClick = { onUpdateConfig { it.copy(syncOffsetMs = (it.syncOffsetMs + 100L).coerceIn(-5000L, 5000L)) } },
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White.copy(alpha = 0.1f)
                ) {
                    Text("+100ms", style = MaterialTheme.typography.labelSmall, color = Color.White, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(
                    onClick = onResetPosition,
                    modifier = Modifier.height(28.dp)
                ) {
                    Text("Center", style = MaterialTheme.typography.labelSmall, color = scheme.primary)
                }
                Surface(
                    onClick = { onUpdateConfig { it.copy(enabled = !it.enabled) } },
                    shape = RoundedCornerShape(8.dp),
                    color = if (config.enabled) scheme.primary.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = if (config.enabled) "ON" else "OFF",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (config.enabled) scheme.primary else Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
