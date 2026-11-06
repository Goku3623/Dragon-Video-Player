package com.example.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.domain.model.AudioChannelMode
import com.example.domain.model.AudioEngineConfig
import com.example.domain.model.EqualizerPreset
import com.example.domain.model.SubtitleColorChoice
import com.example.domain.model.SubtitleFontChoice
import com.example.domain.model.SubtitleStyleConfig
import com.example.player.SubtitleParserEngine
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.dragonGlassSurface

@Composable
fun SubtitleCustomizerSheet(
    config: SubtitleStyleConfig,
    onUpdateConfig: ((SubtitleStyleConfig) -> SubtitleStyleConfig) -> Unit,
    onLoadExternalSubtitleFile: () -> Unit,
    onDismiss: () -> Unit
) {
    val tracks = SubtitleParserEngine.getAvailableSubtitleTracks(config.externalSubtitleName)
    val scheme = MaterialTheme.colorScheme

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .dragonGlassSurface(elevated = true, accentGlow = true)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Subtitles, contentDescription = null, tint = scheme.primary)
                    Text(
                        text = "Subtitle Studio & Sync",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            // Enable toggle + Load External (.srt, .vtt, .ass, .ssa, .sub)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Render Subtitles", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    Switch(
                        checked = config.enabled,
                        onCheckedChange = { enabled -> onUpdateConfig { it.copy(enabled = enabled) } }
                    )
                }
                OutlinedButton(
                    onClick = onLoadExternalSubtitleFile,
                    modifier = Modifier.testTag("load_external_subtitle_button")
                ) {
                    Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Load .SRT/.ASS/.VTT", style = MaterialTheme.typography.labelMedium)
                }
            }

            // Live Subtitle Preview Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF06060A))
                    .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(12.dp))
                    .padding(vertical = 16.dp, horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = config.backgroundOpacity))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "\"Across the obsidian skyline, Dragon fire awakens.\"",
                        color = config.textColorChoice.color,
                        fontSize = config.fontSizeSp.sp,
                        fontFamily = config.fontChoice.toFontFamily(),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Active Track Selection
            Text("Subtitle Track (.srt / .vtt / .ass / .ssa / .sub)", style = MaterialTheme.typography.labelLarge, color = scheme.primary)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                tracks.forEach { track ->
                    val selected = config.selectedTrackId == track.id
                    Surface(
                        onClick = { onUpdateConfig { it.copy(selectedTrackId = track.id, enabled = true) } },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selected) scheme.primary.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.06f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(track.label, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                            Text(
                                text = track.format.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = scheme.tertiary
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

            // Millisecond Sync Offset Slider (-5000ms to +5000ms)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Subtitle Sync Offset", style = MaterialTheme.typography.labelLarge, color = Color.White)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = { onUpdateConfig { it.copy(syncOffsetMs = (it.syncOffsetMs - 100L).coerceIn(-5000L, 5000L)) } },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "-100ms", tint = Color.White)
                    }
                    Text(
                        text = "${if (config.syncOffsetMs >= 0) "+" else ""}${config.syncOffsetMs} ms",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = scheme.primary
                    )
                    IconButton(
                        onClick = { onUpdateConfig { it.copy(syncOffsetMs = (it.syncOffsetMs + 100L).coerceIn(-5000L, 5000L)) } },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "+100ms", tint = Color.White)
                    }
                }
            }
            Slider(
                value = config.syncOffsetMs.toFloat(),
                onValueChange = { v -> onUpdateConfig { it.copy(syncOffsetMs = (v / 50f).toLong() * 50L) } },
                valueRange = -5000f..5000f
            )

            // Font Family Selection
            Text("Font Family", style = MaterialTheme.typography.labelLarge, color = Color.White)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SubtitleFontChoice.entries.forEach { font ->
                    FilterChip(
                        selected = config.fontChoice == font,
                        onClick = { onUpdateConfig { it.copy(fontChoice = font) } },
                        label = { Text(font.label) }
                    )
                }
            }

            // Font Size & Outline & Background Opacity Sliders
            Text("Font Size: ${config.fontSizeSp.toInt()} sp", style = MaterialTheme.typography.labelMedium, color = Color.White)
            Slider(
                value = config.fontSizeSp,
                onValueChange = { s -> onUpdateConfig { it.copy(fontSizeSp = s) } },
                valueRange = 14f..34f
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Outline: ${"%.1f".format(config.outlineWidthDp)} dp", style = MaterialTheme.typography.labelSmall, color = Color.White)
                    Slider(
                        value = config.outlineWidthDp,
                        onValueChange = { o -> onUpdateConfig { it.copy(outlineWidthDp = o) } },
                        valueRange = 0f..4f
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("BG Box: ${(config.backgroundOpacity * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = Color.White)
                    Slider(
                        value = config.backgroundOpacity,
                        onValueChange = { b -> onUpdateConfig { it.copy(backgroundOpacity = b) } },
                        valueRange = 0f..0.95f
                    )
                }
            }

            // Text Color Swatches
            Text("Subtitle Color", style = MaterialTheme.typography.labelLarge, color = Color.White)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SubtitleColorChoice.entries.forEach { choice ->
                    val selected = config.textColorChoice == choice
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(choice.color)
                            .border(
                                width = if (selected) 3.dp else 1.dp,
                                color = if (selected) scheme.primary else Color.White.copy(alpha = 0.4f),
                                shape = CircleShape
                            )
                            .clickable { onUpdateConfig { it.copy(textColorChoice = choice) } }
                    )
                }
            }

            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Apply Subtitle Settings")
            }
        }
    }
}

@Composable
fun AudioAndEqualizerSheet(
    config: AudioEngineConfig,
    videoCodec: String,
    audioCodec: String,
    onUpdateConfig: ((AudioEngineConfig) -> AudioEngineConfig) -> Unit,
    onDismiss: () -> Unit
) {
    val tracks = SubtitleParserEngine.getAvailableAudioTracks(videoCodec, audioCodec)
    val scheme = MaterialTheme.colorScheme

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .dragonGlassSurface(elevated = true, accentGlow = true)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.GraphicEq, contentDescription = null, tint = scheme.primary)
                    Text(
                        text = "Audio & Equalizer Studio",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            // Multi-Track Audio Channel Switcher
            Text("Audio Stream Track", style = MaterialTheme.typography.labelLarge, color = scheme.primary)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                tracks.forEach { track ->
                    val selected = config.selectedTrackId == track.id
                    Surface(
                        onClick = { onUpdateConfig { it.copy(selectedTrackId = track.id) } },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selected) scheme.primary.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.06f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Text(track.label, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                            Text(
                                text = "${track.channels} • ${track.sampleRate} • ${track.language}",
                                style = MaterialTheme.typography.labelSmall,
                                color = scheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Channel Output Mode
            Text("Channel Routing & Spatial Mode", style = MaterialTheme.typography.labelLarge, color = Color.White)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AudioChannelMode.entries.forEach { mode ->
                    FilterChip(
                        selected = config.channelMode == mode,
                        onClick = { onUpdateConfig { it.copy(channelMode = mode) } },
                        label = { Text(mode.label) }
                    )
                }
            }

            // 200% Volume Boost Amplifier Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Hardware + DSP Volume Boost", style = MaterialTheme.typography.labelLarge, color = Color.White)
                Text(
                    text = "${config.volumeBoostPercent}%",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = if (config.volumeBoostPercent > 100) scheme.primary else scheme.secondary
                )
            }
            Slider(
                value = config.volumeBoostPercent.toFloat(),
                onValueChange = { v -> onUpdateConfig { it.copy(volumeBoostPercent = v.toInt()) } },
                valueRange = 0f..200f
            )

            // Audio Delay Sync Slider (-2000ms to +2000ms)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Audio Delay Sync", style = MaterialTheme.typography.labelLarge, color = Color.White)
                Text(
                    text = "${if (config.audioDelayMs >= 0) "+" else ""}${config.audioDelayMs} ms",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = scheme.tertiary
                )
            }
            Slider(
                value = config.audioDelayMs.toFloat(),
                onValueChange = { d -> onUpdateConfig { it.copy(audioDelayMs = (d / 25f).toLong() * 25L) } },
                valueRange = -2000f..2000f
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

            // Equalizer Presets & 5-Band Visualizer
            Text("5-Band Equalizer Presets", style = MaterialTheme.typography.labelLarge, color = scheme.primary)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EqualizerPreset.entries.forEach { preset ->
                    FilterChip(
                        selected = config.equalizerPreset == preset,
                        onClick = {
                            onUpdateConfig {
                                it.copy(
                                    equalizerPreset = preset,
                                    customBandGainsDb = preset.bandGainsDb
                                )
                            }
                        },
                        label = { Text(preset.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = scheme.primary,
                            selectedLabelColor = Color.Black
                        )
                    )
                }
            }

            // 5-Band Gain Bars (60Hz, 230Hz, 910Hz, 3.6kHz, 14kHz)
            val bandLabels = listOf("60Hz", "230Hz", "910Hz", "3.6kHz", "14kHz")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.35f))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                config.customBandGainsDb.forEachIndexed { idx, gainDb ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${if (gainDb >= 0) "+" else ""}${"%.1f".format(gainDb)}dB",
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.primary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = bandLabels.getOrElse(idx) { "Band" },
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Bass Boost Strength Slider (0 - 1000)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Sub-Bass Boost Engine", style = MaterialTheme.typography.labelLarge, color = Color.White)
                Text(
                    text = "${(config.bassBoostStrength / 10)}%",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = scheme.primary
                )
            }
            Slider(
                value = config.bassBoostStrength.toFloat(),
                onValueChange = { b -> onUpdateConfig { it.copy(bassBoostStrength = b.toInt()) } },
                valueRange = 0f..1000f
            )

            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Done")
            }
        }
    }
}
