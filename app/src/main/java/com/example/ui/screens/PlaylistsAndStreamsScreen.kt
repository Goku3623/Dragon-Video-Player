package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.local.PlaylistEntity
import com.example.data.local.VideoEntity
import com.example.domain.model.CodecCapabilitySpec
import com.example.domain.model.DecoderMode
import com.example.player.StorageScannerHelper
import com.example.ui.theme.dragonGlassSurface
import com.example.ui.theme.dragonScaleMesh

@Composable
fun PlaylistsScreenContent(
    playlists: List<PlaylistEntity>,
    allVideos: List<VideoEntity>,
    onCreatePlaylist: (name: String, description: String) -> Unit,
    onDeletePlaylist: (Long) -> Unit,
    onPlayVideoQueue: (VideoEntity, List<VideoEntity>) -> Unit,
    modifier: Modifier = Modifier
) {
    var newName by remember { mutableStateOf("") }
    var newDesc by remember { mutableStateOf("") }
    var showCreateForm by remember { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("playlists_lazy_column"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .dragonGlassSurface(elevated = true, accentGlow = true)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "CUSTOM MEDIA QUEUES",
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.primary
                    )
                    Text(
                        text = "Dragon Playlists",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White
                    )
                    Text(
                        text = "Continuous gapless playback across your videos & streams",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = { showCreateForm = !showCreateForm },
                    modifier = Modifier.testTag("toggle_create_playlist_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("New Playlist")
                }
            }
        }

        if (showCreateForm) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .dragonGlassSurface(elevated = true)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Create New Playlist", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Playlist Name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("playlist_name_input")
                    )
                    OutlinedTextField(
                        value = newDesc,
                        onValueChange = { newDesc = it },
                        label = { Text("Description (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                if (newName.isNotBlank()) {
                                    onCreatePlaylist(newName, newDesc)
                                    newName = ""
                                    newDesc = ""
                                    showCreateForm = false
                                }
                            },
                            modifier = Modifier.testTag("confirm_create_playlist_button")
                        ) {
                            Text("Create Queue")
                        }
                    }
                }
            }
        }

        if (playlists.isEmpty() && !showCreateForm) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .dragonGlassSurface(elevated = false)
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                        contentDescription = null,
                        tint = scheme.primary,
                        modifier = Modifier.size(46.dp)
                    )
                    Text(
                        text = "No Playlists Yet",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                    Text(
                        text = "Tap 'New Playlist' to organize your favorite videos into custom playback queues.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(playlists, key = { it.id }) { playlist ->
                val trackIds = playlist.getVideoIdList()
                val playlistVideos = allVideos.filter { it.id in trackIds }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .dragonGlassSurface(elevated = false, accentGlow = true)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = playlist.name,
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White
                            )
                            Text(
                                text = "${playlist.description} • ${playlistVideos.size} tracks",
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (playlistVideos.isNotEmpty()) {
                                Button(
                                    onClick = { onPlayVideoQueue(playlistVideos.first(), playlistVideos) }
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.PlaylistPlay, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Play All")
                                }
                            }
                            IconButton(onClick = { onDeletePlaylist(playlist.id) }) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete playlist",
                                    tint = scheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (playlistVideos.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            playlistVideos.forEachIndexed { idx, video ->
                                Surface(
                                    onClick = { onPlayVideoQueue(video, playlistVideos) },
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color.White.copy(alpha = 0.06f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${idx + 1}. ${video.title}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = StorageScannerHelper.formatDurationMs(video.durationMs),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = scheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dedicated Network Stream Screen (separate from Codec section, accessed inside More).
 */
@Composable
fun NetworkStreamScreenContent(
    savedStreams: List<VideoEntity>,
    onBackToMore: () -> Unit,
    onLaunchNetworkStream: (title: String, url: String, protocol: String) -> Unit,
    onPlaySavedStream: (VideoEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var customTitle by remember { mutableStateOf("") }
    var customUrl by remember { mutableStateOf("") }
    var selectedProtocol by remember { mutableStateOf("HLS (.m3u8)") }
    val protocols = listOf("HLS (.m3u8)", "DASH (.mpd)", "SmoothStreaming", "RTSP", "HTTP Direct")
    val scheme = MaterialTheme.colorScheme

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("network_stream_lazy_column"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            MoreSubScreenHeader(
                title = "Network Stream",
                subtitle = "Play HLS (.m3u8), DASH (.mpd), SmoothStreaming, RTSP & HTTP links",
                onBack = onBackToMore
            )
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .dragonGlassSurface(elevated = true, accentGlow = true)
                    .dragonScaleMesh(scaleAlpha = 0.09f)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.WifiTethering, contentDescription = null, tint = scheme.primary)
                    Text(
                        text = "Direct Stream URL Player",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                }

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    protocols.forEach { protocol ->
                        FilterChip(
                            selected = selectedProtocol == protocol,
                            onClick = { selectedProtocol = protocol },
                            label = { Text(protocol) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = scheme.primary,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = customTitle,
                    onValueChange = { customTitle = it },
                    label = { Text("Stream Name (Optional)") },
                    placeholder = { Text("e.g., Live Sports Channel / Media Server") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = customUrl,
                    onValueChange = { customUrl = it },
                    label = { Text("Stream URL (.m3u8, .mpd, rtsp://, https://)") },
                    placeholder = { Text("https://example.com/live/master.m3u8") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("stream_url_input")
                )

                Button(
                    onClick = {
                        if (customUrl.isNotBlank()) {
                            onLaunchNetworkStream(customTitle, customUrl, selectedProtocol)
                        }
                    },
                    enabled = customUrl.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("play_network_stream_button")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Connect & Play Stream")
                }
            }
        }

        if (savedStreams.isNotEmpty()) {
            item {
                Text(
                    text = "Recent Network Streams",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }
            items(savedStreams, key = { it.id }) { streamVideo ->
                Surface(
                    onClick = { onPlaySavedStream(streamVideo) },
                    shape = RoundedCornerShape(14.dp),
                    color = Color.Transparent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .dragonGlassSurface(elevated = false)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(streamVideo.title, style = MaterialTheme.typography.titleMedium, color = Color.White)
                            Text(
                                text = "${streamVideo.streamProtocol} • ${streamVideo.uri}",
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play stream", tint = scheme.primary)
                    }
                }
            }
        }
    }
}

/**
 * Dedicated Codec & Decoder Engine Screen (separate from Network Stream, accessed inside More).
 */
@Composable
fun CodecEngineScreenContent(
    decoderMode: DecoderMode,
    codecSpecs: List<CodecCapabilitySpec>,
    onBackToMore: () -> Unit,
    onSelectDecoderMode: (DecoderMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("codec_engine_lazy_column"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            MoreSubScreenHeader(
                title = "Codec & Decoder Engine",
                subtitle = "Hardware acceleration & bundled FFmpeg / libvpx / libdav1d / Opus software decoders",
                onBack = onBackToMore
            )
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .dragonGlassSurface(elevated = true)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Memory, contentDescription = null, tint = scheme.primary)
                    Text(
                        text = "Decoder Pipeline Selection",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                }

                DecoderMode.entries.forEach { mode ->
                    val selected = decoderMode == mode
                    Surface(
                        onClick = { onSelectDecoderMode(mode) },
                        shape = RoundedCornerShape(14.dp),
                        color = if (selected) scheme.primary.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.05f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("decoder_mode_${mode.name.lowercase()}")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) scheme.primary else Color.White.copy(alpha = 0.14f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = mode.badge,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selected) Color.Black else Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(mode.label, style = MaterialTheme.typography.titleMedium, color = Color.White)
                                Text(mode.description, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Icon(Icons.Default.DeveloperBoard, contentDescription = null, tint = scheme.tertiary)
                Text(
                    text = "Bundled Codec & Format Matrix",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
            }
        }

        items(codecSpecs) { spec ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .dragonGlassSurface(elevated = false)
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (spec.isHardwareAccelerated) scheme.primary else scheme.tertiary,
                    modifier = Modifier.size(22.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(spec.name, style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Text(spec.category, style = MaterialTheme.typography.labelSmall, color = scheme.primary)
                    }
                    Text(spec.engine, style = MaterialTheme.typography.labelMedium, color = scheme.tertiary)
                    Text(spec.details, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun MoreSubScreenHeader(
    title: String,
    subtitle: String,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .dragonGlassSurface(elevated = true, accentGlow = true)
            .padding(horizontal = 10.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.testTag("more_subscreen_back_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back to More",
                tint = Color.White
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
