package com.example.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.local.VideoEntity
import com.example.domain.model.AspectRatioMode
import com.example.domain.model.DecoderMode
import com.example.player.StorageScannerHelper
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.dragonGlassSurface

@Composable
fun DragonPlayerHudOverlay(
    video: VideoEntity,
    isHudVisible: Boolean,
    isHudLocked: Boolean,
    isLandscapeFullscreen: Boolean,
    isFloatingSubtitleOpen: Boolean,
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    bufferedPercentage: Int,
    playbackSpeed: Float,
    aspectRatioMode: AspectRatioMode,
    decoderMode: DecoderMode,
    backgroundAudioEnabled: Boolean,
    volumeBoostPercent: Int,
    onClosePlayer: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekRelative: (Long) -> Unit,
    onStepFrame: (forward: Boolean) -> Unit,
    onPlayPrevious: () -> Unit,
    onPlayNext: () -> Unit,
    onCycleAspectRatio: () -> Unit,
    onToggleLandscapeFullscreen: () -> Unit,
    onToggleFloatingSubtitle: () -> Unit,
    onSelectSpeed: (Float) -> Unit,
    onCycleDecoderMode: () -> Unit,
    onToggleBackgroundAudio: () -> Unit,
    onEnterPipMode: () -> Unit,
    onCaptureScreenshot: () -> Unit,
    onOpenSubtitleStudio: () -> Unit,
    onOpenAudioStudio: () -> Unit,
    onDeleteVideoToTrash: () -> Unit,
    onToggleHudLock: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showThreeDotMenuDialog by remember { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme

    Box(modifier = modifier.fillMaxSize()) {
        // When HUD is locked: show only the floating Lock icon at the upper side
        AnimatedVisibility(
            visible = isHudVisible && isHudLocked,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = 12.dp, end = 16.dp)
        ) {
            FloatingUpperIconCircle(
                icon = Icons.Default.Lock,
                contentDescription = "Unlock Controls",
                tint = scheme.primary,
                onClick = onToggleHudLock,
                testTag = "unlock_hud_button"
            )
        }

        // Upper Side Minimal Floating Icons ONLY: Rotate Icon, Mini Display (PiP), Lock Icon
        AnimatedVisibility(
            visible = isHudVisible && !isHudLocked,
            enter = fadeIn() + slideInVertically { -it / 2 },
            exit = fadeOut() + slideOutVertically { -it / 2 },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = 12.dp, end = 16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Rotate Icon (Landscape Fullscreen / Portrait toggle)
                FloatingUpperIconCircle(
                    icon = Icons.Default.ScreenRotation,
                    contentDescription = "Rotate Screen",
                    tint = if (isLandscapeFullscreen) scheme.primary else Color.White,
                    onClick = onToggleLandscapeFullscreen,
                    testTag = "screen_rotate_button"
                )

                // 2. Mini Display Icon (Picture-in-Picture mode)
                FloatingUpperIconCircle(
                    icon = Icons.Default.PictureInPictureAlt,
                    contentDescription = "Mini Display",
                    tint = Color.White,
                    onClick = onEnterPipMode,
                    testTag = "pip_mode_button"
                )

                // 3. Lock Icon (Lock player touch controls)
                FloatingUpperIconCircle(
                    icon = Icons.Default.LockOpen,
                    contentDescription = "Lock Controls",
                    tint = Color.White,
                    onClick = onToggleHudLock,
                    testTag = "lock_hud_button"
                )
            }
        }

        // Compact Bottom Panel: Slim Timeline Row + Compact Transport & 3-Dot Menu Row
        AnimatedVisibility(
            visible = isHudVisible && !isHudLocked,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .dragonGlassSurface(cornerRadiusOverride = 20.dp, elevated = true, accentGlow = true)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Compact Single-Line Timeline Scrubber Row: [01:24] ----o-------- [12:14]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = StorageScannerHelper.formatDurationMs(currentPositionMs),
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.primary
                    )

                    Slider(
                        value = currentPositionMs.toFloat().coerceIn(0f, durationMs.coerceAtLeast(1L).toFloat()),
                        onValueChange = { onSeekTo(it.toLong()) },
                        valueRange = 0f..durationMs.coerceAtLeast(1L).toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = scheme.primary,
                            activeTrackColor = scheme.primary,
                            inactiveTrackColor = Color.White.copy(alpha = 0.22f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(22.dp)
                            .testTag("timeline_seek_slider")
                    )

                    Text(
                        text = StorageScannerHelper.formatDurationMs(durationMs),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White
                    )
                }

                // Compact Transport & Action Row (No Landscape Full button here; detailed options moved to 3-dot menu)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Floating Subtitle Resize Toggle Icon
                    IconButton(
                        onClick = onToggleFloatingSubtitle,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("hud_floating_subtitle_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Subtitles,
                            contentDescription = "Floating Subtitle Resize Control",
                            tint = if (isFloatingSubtitleOpen) scheme.primary else Color.White,
                            modifier = Modifier.size(21.dp)
                        )
                    }

                    // Core Transport Controls
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = onPlayPrevious,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "Previous video",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        IconButton(
                            onClick = { onSeekRelative(-10_000L) },
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("rewind_10s_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Replay10,
                                contentDescription = "Rewind 10 seconds",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Compact Center Play/Pause Button
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(scheme.primary, scheme.secondary)
                                    )
                                )
                                .clickable { onTogglePlayPause() }
                                .testTag("play_pause_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.Black,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        IconButton(
                            onClick = { onSeekRelative(10_000L) },
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("forward_10s_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Forward10,
                                contentDescription = "Forward 10 seconds",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        IconButton(
                            onClick = onPlayNext,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Next video",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // Right-side Aspect Ratio + 3-Dot More Options Menu
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        IconButton(
                            onClick = onCycleAspectRatio,
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("hud_aspect_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AspectRatio,
                                contentDescription = "Aspect Ratio (${aspectRatioMode.label})",
                                tint = if (aspectRatioMode != AspectRatioMode.FIT) scheme.primary else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = { showThreeDotMenuDialog = true },
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("hud_three_dot_menu_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More player options",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Three-Dot Detailed Options Sheet / Modal
    if (showThreeDotMenuDialog) {
        PlayerThreeDotOptionsDialog(
            video = video,
            playbackSpeed = playbackSpeed,
            decoderMode = decoderMode,
            backgroundAudioEnabled = backgroundAudioEnabled,
            volumeBoostPercent = volumeBoostPercent,
            isFloatingSubtitleOpen = isFloatingSubtitleOpen,
            onSelectSpeed = onSelectSpeed,
            onStepFrame = onStepFrame,
            onToggleFloatingSubtitle = {
                showThreeDotMenuDialog = false
                onToggleFloatingSubtitle()
            },
            onOpenSubtitleStudio = {
                showThreeDotMenuDialog = false
                onOpenSubtitleStudio()
            },
            onOpenAudioStudio = {
                showThreeDotMenuDialog = false
                onOpenAudioStudio()
            },
            onCaptureScreenshot = {
                showThreeDotMenuDialog = false
                onCaptureScreenshot()
            },
            onToggleBackgroundAudio = onToggleBackgroundAudio,
            onCycleDecoderMode = onCycleDecoderMode,
            onDeleteVideoToTrash = {
                showThreeDotMenuDialog = false
                onDeleteVideoToTrash()
            },
            onClosePlayer = {
                showThreeDotMenuDialog = false
                onClosePlayer()
            },
            onDismiss = { showThreeDotMenuDialog = false }
        )
    }
}

@Composable
private fun FloatingUpperIconCircle(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.Transparent,
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .size(44.dp)
            .dragonGlassSurface(cornerRadiusOverride = 22.dp, elevated = true, accentGlow = true)
            .testTag(testTag)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun PlayerThreeDotOptionsDialog(
    video: VideoEntity,
    playbackSpeed: Float,
    decoderMode: DecoderMode,
    backgroundAudioEnabled: Boolean,
    volumeBoostPercent: Int,
    isFloatingSubtitleOpen: Boolean,
    onSelectSpeed: (Float) -> Unit,
    onStepFrame: (forward: Boolean) -> Unit,
    onToggleFloatingSubtitle: () -> Unit,
    onOpenSubtitleStudio: () -> Unit,
    onOpenAudioStudio: () -> Unit,
    onCaptureScreenshot: () -> Unit,
    onToggleBackgroundAudio: () -> Unit,
    onCycleDecoderMode: () -> Unit,
    onDeleteVideoToTrash: () -> Unit,
    onClosePlayer: () -> Unit,
    onDismiss: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val presetSpeeds = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 3.0f, 4.0f)

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .dragonGlassSurface(cornerRadiusOverride = 22.dp, elevated = true, accentGlow = true)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with Video Title & Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Player Options",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                    Text(
                        text = "${video.resolutionBadge} • ${video.containerFormat} • ${video.videoCodec}",
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.primary
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close menu", tint = Color.White)
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

            // 1. Playback Speed Selector (0.25x - 4.0x)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Speed, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(18.dp))
                        Text("Playback Speed", style = MaterialTheme.typography.labelLarge, color = Color.White)
                    }
                    Text(
                        text = "%.2fx".format(playbackSpeed),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = scheme.primary
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetSpeeds.forEach { spd ->
                        val selected = kotlin.math.abs(playbackSpeed - spd) < 0.03f
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectSpeed(spd) },
                            label = { Text("${spd}x", style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = scheme.primary,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }
            }

            // 2. Frame-by-Frame Stepping Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Frame-by-Frame Step", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        onClick = { onStepFrame(false) },
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = "-1 Frame",
                            style = MaterialTheme.typography.labelMedium,
                            color = scheme.tertiary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                    Surface(
                        onClick = { onStepFrame(true) },
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = "+1 Frame",
                            style = MaterialTheme.typography.labelMedium,
                            color = scheme.tertiary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // 3. Detailed Action Rows
            ThreeDotMenuRowItem(
                icon = Icons.Default.Subtitles,
                title = if (isFloatingSubtitleOpen) "Hide Floating Subtitle Resize" else "Floating Subtitle Resize Panel",
                subtitle = "Drag & resize subtitle text/width live over video",
                badge = "RESIZE",
                onClick = onToggleFloatingSubtitle
            )

            ThreeDotMenuRowItem(
                icon = Icons.Default.ClosedCaption,
                title = "Subtitle Studio & External File",
                subtitle = "Tracks, fonts, colors, sync offset & .SRT/.ASS/.VTT loader",
                badge = "STUDIO",
                onClick = onOpenSubtitleStudio
            )

            ThreeDotMenuRowItem(
                icon = Icons.Default.Equalizer,
                title = "Audio & Equalizer Studio",
                subtitle = "Multi-track audio, 5-band EQ, Bass Boost & delay sync",
                badge = "$volumeBoostPercent%",
                onClick = onOpenAudioStudio
            )

            ThreeDotMenuRowItem(
                icon = Icons.Default.CameraAlt,
                title = "Capture High-Res Screenshot",
                subtitle = "Save current video frame to gallery",
                badge = "PNG",
                onClick = onCaptureScreenshot
            )

            ThreeDotMenuRowItem(
                icon = Icons.Default.Memory,
                title = "Decoder Mode: ${decoderMode.label}",
                subtitle = "Tap to switch HW+ / HW / Software FFmpeg",
                badge = decoderMode.badge,
                onClick = onCycleDecoderMode
            )

            ThreeDotMenuRowItem(
                icon = Icons.Default.DeleteOutline,
                title = "Delete Video",
                subtitle = "Move this video to Trash Bin",
                badge = "TRASH",
                onClick = onDeleteVideoToTrash
            )

            // 4. Background Audio Switch + Exit Player Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Headphones, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(18.dp))
                    Text("Background Audio", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                }
                Switch(
                    checked = backgroundAudioEnabled,
                    onCheckedChange = { onToggleBackgroundAudio() }
                )
            }

            Surface(
                onClick = onClosePlayer,
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.08f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 14.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Close Video Player", style = MaterialTheme.typography.labelLarge, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun ThreeDotMenuRowItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badge: String,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.06f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = Color.White)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(scheme.primary.copy(alpha = 0.2f))
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Text(badge, style = MaterialTheme.typography.labelSmall, color = scheme.primary)
            }
        }
    }
}
