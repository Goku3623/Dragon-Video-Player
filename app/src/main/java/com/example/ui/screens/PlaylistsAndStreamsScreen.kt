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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.PlaylistEntity
import com.example.data.local.StreamBookmarkEntity
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
 * Dedicated Network Stream Screen with Video Stream Bookmarks and Recent Streams.
 */
@Composable
fun NetworkStreamScreenContent(
    savedStreams: List<VideoEntity>,
    streamBookmarks: List<StreamBookmarkEntity>,
    onBackToMore: () -> Unit,
    onLaunchNetworkStream: (title: String, url: String, protocol: String) -> Unit,
    onSaveStreamBookmark: (title: String, url: String, protocol: String) -> Unit,
    onPlayBookmarkedStream: (StreamBookmarkEntity) -> Unit,
    onRemoveStreamBookmark: (StreamBookmarkEntity) -> Unit,
    onPlaySavedStream: (VideoEntity) -> Unit,
    onResetRecentStreams: () -> Unit,
    onRemoveStream: (VideoEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var customTitle by remember { mutableStateOf("") }
    var customUrl by remember { mutableStateOf("") }
    var selectedProtocol by remember { mutableStateOf("HLS (.m3u8)") }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var showAddBookmarkDialog by remember { mutableStateOf(false) }
    var dialogBookmarkTitle by remember { mutableStateOf("") }
    var dialogBookmarkUrl by remember { mutableStateOf("") }
    var dialogBookmarkProtocol by remember { mutableStateOf("HLS (.m3u8)") }

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
                subtitle = "Play & bookmark HLS (.m3u8), DASH (.mpd), RTSP & HTTP video links",
                onBack = onBackToMore
            )
        }

        // Direct Stream URL Player & Save Bookmark Card
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
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
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
                    if (customTitle.isNotEmpty() || customUrl.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                customTitle = ""
                                customUrl = ""
                            },
                            modifier = Modifier.testTag("clear_stream_fields_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reset Fields", modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Clear")
                        }
                    }
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("stream_title_input")
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            if (customUrl.isNotBlank()) {
                                onLaunchNetworkStream(customTitle, customUrl, selectedProtocol)
                            }
                        },
                        enabled = customUrl.isNotBlank(),
                        modifier = Modifier
                            .weight(1.35f)
                            .testTag("play_network_stream_button")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Connect & Play")
                    }

                    OutlinedButton(
                        onClick = {
                            if (customUrl.isNotBlank()) {
                                onSaveStreamBookmark(customTitle, customUrl, selectedProtocol)
                            }
                        },
                        enabled = customUrl.isNotBlank(),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("bookmark_stream_url_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.BookmarkAdd,
                            contentDescription = "Save Video Link Bookmark",
                            tint = scheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Bookmark")
                    }
                }
            }
        }

        // Video Bookmarks Section Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bookmarks,
                        contentDescription = null,
                        tint = scheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "Video Stream Bookmarks",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Text(
                            text = if (streamBookmarks.isEmpty()) {
                                "Save video links for instant 1-tap streaming"
                            } else {
                                "${streamBookmarks.size} bookmarked video link(s)"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedButton(
                    onClick = {
                        dialogBookmarkTitle = customTitle
                        dialogBookmarkUrl = customUrl
                        dialogBookmarkProtocol = selectedProtocol
                        showAddBookmarkDialog = true
                    },
                    modifier = Modifier.testTag("open_add_bookmark_dialog_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.BookmarkAdd,
                        contentDescription = "New Video Bookmark",
                        tint = scheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text("Add Link")
                }
            }
        }

        if (streamBookmarks.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .dragonGlassSurface(elevated = false)
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = null,
                        tint = scheme.primary.copy(alpha = 0.72f),
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "No Video Bookmarks Saved Yet",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White
                    )
                    Text(
                        text = "Enter a stream link above and tap 'Bookmark' or 'Add Link' to save your favorite video URLs for quick streaming.",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(streamBookmarks, key = { "bookmark_${it.id}" }) { bookmark ->
                Surface(
                    onClick = { onPlayBookmarkedStream(bookmark) },
                    shape = RoundedCornerShape(14.dp),
                    color = Color.Transparent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .dragonGlassSurface(elevated = true, accentGlow = true)
                        .testTag("stream_bookmark_item_${bookmark.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(scheme.primary.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bookmark,
                                    contentDescription = null,
                                    tint = scheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = bookmark.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${bookmark.protocol} • ${bookmark.url}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = scheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                onClick = { onPlayBookmarkedStream(bookmark) },
                                shape = RoundedCornerShape(10.dp),
                                color = scheme.primary,
                                modifier = Modifier.testTag("play_bookmark_button_${bookmark.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Stream bookmarked link",
                                        tint = Color.Black,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Stream",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                }
                            }

                            Surface(
                                onClick = { onRemoveStreamBookmark(bookmark) },
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFFF4D6D).copy(alpha = 0.16f),
                                modifier = Modifier.testTag("remove_bookmark_button_${bookmark.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Remove bookmark link",
                                        tint = Color(0xFFFF4D6D),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Remove",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFFF4D6D)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Recent Network Streams Section Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Recent Network Streams",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Text(
                        text = if (savedStreams.isEmpty()) "No saved stream history" else "${savedStreams.size} recent stream(s)",
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant
                    )
                }

                if (savedStreams.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { showResetConfirmDialog = true },
                        modifier = Modifier.testTag("reset_recent_streams_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Reset Recent Network Streams",
                            tint = scheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Reset Recent")
                    }
                }
            }
        }

        if (savedStreams.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .dragonGlassSurface(elevated = false)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.WifiTethering,
                        contentDescription = null,
                        tint = scheme.primary.copy(alpha = 0.7f),
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = "No Recent Network Streams",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Text(
                        text = "Streams you play will appear here for quick 1-tap reconnection.",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(savedStreams, key = { it.id }) { streamVideo ->
                val isAlreadyBookmarked = streamBookmarks.any {
                    it.url.equals(streamVideo.uri, ignoreCase = true)
                }
                Surface(
                    onClick = { onPlaySavedStream(streamVideo) },
                    shape = RoundedCornerShape(14.dp),
                    color = Color.Transparent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .dragonGlassSurface(elevated = false)
                        .testTag("recent_stream_item_${streamVideo.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = streamVideo.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${streamVideo.streamProtocol} • ${streamVideo.uri}",
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    onSaveStreamBookmark(
                                        streamVideo.title,
                                        streamVideo.uri,
                                        streamVideo.streamProtocol.ifBlank { "HLS (.m3u8)" }
                                    )
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("bookmark_recent_stream_${streamVideo.id}")
                            ) {
                                Icon(
                                    imageVector = if (isAlreadyBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkAdd,
                                    contentDescription = "Bookmark this stream link",
                                    tint = if (isAlreadyBookmarked) scheme.primary else scheme.onSurfaceVariant,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                            IconButton(
                                onClick = { onPlaySavedStream(streamVideo) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play stream",
                                    tint = scheme.primary
                                )
                            }
                            IconButton(
                                onClick = { onRemoveStream(streamVideo) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("remove_stream_button_${streamVideo.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove from recent streams",
                                    tint = scheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddBookmarkDialog) {
        AlertDialog(
            onDismissRequest = { showAddBookmarkDialog = false },
            title = { Text("Save Video Stream Bookmark") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = dialogBookmarkTitle,
                        onValueChange = { dialogBookmarkTitle = it },
                        label = { Text("Bookmark Name") },
                        placeholder = { Text("e.g., 4K Cinema Stream") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_bookmark_title_input")
                    )
                    OutlinedTextField(
                        value = dialogBookmarkUrl,
                        onValueChange = { dialogBookmarkUrl = it },
                        label = { Text("Video Stream URL") },
                        placeholder = { Text("https://example.com/stream.m3u8") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_bookmark_url_input")
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        protocols.forEach { protocol ->
                            FilterChip(
                                selected = dialogBookmarkProtocol == protocol,
                                onClick = { dialogBookmarkProtocol = protocol },
                                label = { Text(protocol) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (dialogBookmarkUrl.isNotBlank()) {
                            onSaveStreamBookmark(
                                dialogBookmarkTitle,
                                dialogBookmarkUrl,
                                dialogBookmarkProtocol
                            )
                            showAddBookmarkDialog = false
                        }
                    },
                    enabled = dialogBookmarkUrl.isNotBlank(),
                    modifier = Modifier.testTag("confirm_save_bookmark_button")
                ) {
                    Text("Save Bookmark")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddBookmarkDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text("Reset Recent Network Streams?") },
            text = {
                Text("This will clear all ${savedStreams.size} saved network stream URLs from your history.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirmDialog = false
                        onResetRecentStreams()
                    },
                    modifier = Modifier.testTag("confirm_reset_streams_button")
                ) {
                    Text("Reset All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Compact Codec & Decoder Engine Screen inside More.
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
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            MoreSubScreenHeader(
                title = "Codec & Decoder",
                subtitle = "Compact HW+ / FFmpeg / libvpx / dav1d / Opus pipeline",
                onBack = onBackToMore
            )
        }

        // Compact Segmented 3-Pill Decoder Selector
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .dragonGlassSurface(elevated = true)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
                            imageVector = Icons.Default.Memory,
                            contentDescription = null,
                            tint = scheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Active Decoder Pipeline",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                    }
                    Text(
                        text = decoderMode.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.primary
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DecoderMode.entries.forEach { mode ->
                        val selected = decoderMode == mode
                        Surface(
                            onClick = { onSelectDecoderMode(mode) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) scheme.primary else Color.White.copy(alpha = 0.06f),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("decoder_mode_${mode.name.lowercase()}")
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = mode.badge,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) Color.Black else Color.White
                                )
                                Text(
                                    text = mode.label.substringBefore(" "),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) Color.Black.copy(alpha = 0.78f) else scheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                Text(
                    text = decoderMode.description,
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant
                )
            }
        }

        // Compact 2-Column Bundled Codec & Container Grid
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeveloperBoard,
                    contentDescription = null,
                    tint = scheme.tertiary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Bundled Codec Matrix (${codecSpecs.size} Active)",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White
                )
            }
        }

        items(codecSpecs.chunked(2)) { rowPair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowPair.forEach { spec ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .dragonGlassSurface(elevated = false)
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = spec.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (spec.isHardwareAccelerated) scheme.primary else scheme.tertiary
                            )
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (spec.isHardwareAccelerated) scheme.primary else scheme.tertiary,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        Text(
                            text = spec.name,
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = spec.engine,
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
                if (rowPair.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
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
