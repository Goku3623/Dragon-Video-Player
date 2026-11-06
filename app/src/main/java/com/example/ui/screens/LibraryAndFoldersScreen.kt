package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.AddToQueue
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.local.PlaylistEntity
import com.example.data.local.VideoEntity
import com.example.domain.model.DecoderMode
import com.example.domain.model.VideoFolderSummary
import com.example.domain.model.VideoSortOption
import com.example.player.StorageScannerHelper
import com.example.ui.theme.dragonGlassSurface
import com.example.ui.theme.dragonScaleMesh

@Composable
fun LibraryScreenContent(
    videos: List<VideoEntity>,
    playlists: List<PlaylistEntity>,
    searchQuery: String,
    sortOption: VideoSortOption,
    sortAscending: Boolean,
    selectedCategory: String,
    selectedFolderFilter: String?,
    decoderMode: DecoderMode,
    onSearchQueryChange: (String) -> Unit,
    onSortOptionSelect: (VideoSortOption) -> Unit,
    onCategorySelect: (String) -> Unit,
    onClearFolderFilter: () -> Unit,
    onPlayVideo: (VideoEntity) -> Unit,
    onToggleFavorite: (VideoEntity) -> Unit,
    onMoveToVault: (VideoEntity) -> Unit,
    onDeleteToTrash: (VideoEntity) -> Unit,
    onToggleVideoInPlaylist: (PlaylistEntity, Long) -> Unit,
    onScanStorage: () -> Unit,
    onPickSafFolder: () -> Unit,
    onPickSingleVideo: () -> Unit,
    onOpenStreamSection: () -> Unit,
    modifier: Modifier = Modifier
) {
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var playlistDialogVideo by remember { mutableStateOf<VideoEntity?>(null) }
    val scheme = MaterialTheme.colorScheme

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("library_lazy_column"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Clean Action Header Card
        item {
            DragonCleanActionCard(
                totalVideos = videos.size,
                decoderMode = decoderMode,
                onScanStorage = onScanStorage,
                onPickSafFolder = onPickSafFolder,
                onPickSingleVideo = onPickSingleVideo,
                onOpenStreamSection = onOpenStreamSection
            )
        }

        // 2. Search Bar & Sort Dropdown
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Search your videos, folders, codecs…", style = MaterialTheme.typography.bodyMedium) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = scheme.primary) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = scheme.primary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.16f),
                        focusedContainerColor = scheme.surface.copy(alpha = 0.7f),
                        unfocusedContainerColor = scheme.surface.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("library_search_input")
                )

                Box {
                    Surface(
                        onClick = { sortMenuExpanded = true },
                        shape = RoundedCornerShape(16.dp),
                        color = scheme.surfaceVariant.copy(alpha = 0.85f),
                        modifier = Modifier.testTag("sort_menu_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort", tint = scheme.primary)
                            Text(
                                text = "${sortOption.label} ${if (sortAscending) "↑" else "↓"}",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false }
                    ) {
                        VideoSortOption.entries.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = if (option == sortOption) "${option.label} (${if (sortAscending) "Asc" else "Desc"})"
                                        else option.label
                                    )
                                },
                                onClick = {
                                    onSortOptionSelect(option)
                                    sortMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // 3. Category & Active Folder Filter Chips
        item {
            val categories = listOf("All", "4K UHD", "MKV / HEVC", "AV1 / VP9", "Streams", "Favorites")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        val selected = selectedCategory == cat
                        FilterChip(
                            selected = selected,
                            onClick = { onCategorySelect(cat) },
                            label = { Text(cat, style = MaterialTheme.typography.labelLarge) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = scheme.primary,
                                selectedLabelColor = Color.Black
                            ),
                            modifier = Modifier.testTag("filter_chip_${cat.lowercase().replace(" ", "_")}")
                        )
                    }
                }

                if (selectedFolderFilter != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .dragonGlassSurface(cornerRadiusOverride = 12.dp)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.FolderSpecial, contentDescription = null, tint = scheme.primary)
                            Text(
                                text = "Folder: $selectedFolderFilter",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White
                            )
                        }
                        TextButton(onClick = onClearFolderFilter) {
                            Text("Show All Folders", color = scheme.primary)
                        }
                    }
                }
            }
        }

        // 4. Video Items List (with real extracted video thumbnails)
        if (videos.isEmpty()) {
            item {
                EmptyLibraryPlaceholder(
                    onScanStorage = onScanStorage,
                    onPickVideo = onPickSingleVideo,
                    onPickFolder = onPickSafFolder
                )
            }
        } else {
            items(videos, key = { it.id }) { video ->
                DragonVideoCard(
                    video = video,
                    onPlay = { onPlayVideo(video) },
                    onToggleFavorite = { onToggleFavorite(video) },
                    onMoveToVault = { onMoveToVault(video) },
                    onDeleteToTrash = { onDeleteToTrash(video) },
                    onAddToPlaylist = { playlistDialogVideo = video }
                )
            }
        }
    }

    // Add to Playlist Modal Dialog
    playlistDialogVideo?.let { targetVideo ->
        Dialog(onDismissRequest = { playlistDialogVideo = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .dragonGlassSurface(elevated = true, accentGlow = true)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Add to Playlist",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
                Text(
                    text = targetVideo.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (playlists.isEmpty()) {
                    Text(
                        text = "No playlists created yet. Open the Playlists tab to create one.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant
                    )
                } else {
                    playlists.forEach { playlist ->
                        val inPlaylist = playlist.getVideoIdList().contains(targetVideo.id)
                        Surface(
                            onClick = { onToggleVideoInPlaylist(playlist, targetVideo.id) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (inPlaylist) scheme.primary.copy(alpha = 0.24f) else Color.White.copy(alpha = 0.07f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(playlist.name, style = MaterialTheme.typography.titleMedium, color = Color.White)
                                Text(
                                    text = if (inPlaylist) "ADDED ✓" else "+ ADD",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (inPlaylist) scheme.primary else scheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                Button(
                    onClick = { playlistDialogVideo = null },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Done")
                }
            }
        }
    }
}

@Composable
private fun DragonCleanActionCard(
    totalVideos: Int,
    decoderMode: DecoderMode,
    onScanStorage: () -> Unit,
    onPickSafFolder: () -> Unit,
    onPickSingleVideo: () -> Unit,
    onOpenStreamSection: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .dragonGlassSurface(elevated = true, accentGlow = true)
            .dragonScaleMesh(scaleAlpha = 0.08f)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "MEDIA LIBRARY",
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.primary
                )
                Text(
                    text = if (totalVideos == 0) "Ready to Play" else "$totalVideos Videos Indexed",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(scheme.primary.copy(alpha = 0.2f))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Engine ${decoderMode.badge}",
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.primary
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickHeroActionChip(
                icon = Icons.Default.VideoFile,
                label = "Open Video File",
                onClick = onPickSingleVideo,
                testTag = "hero_open_file_button"
            )
            QuickHeroActionChip(
                icon = Icons.Default.CreateNewFolder,
                label = "Mount Folder / SD",
                onClick = onPickSafFolder,
                testTag = "hero_mount_folder_button"
            )
            QuickHeroActionChip(
                icon = Icons.Default.Radar,
                label = "Scan Storage",
                onClick = onScanStorage,
                testTag = "hero_scan_storage_button"
            )
            QuickHeroActionChip(
                icon = Icons.Default.WifiTethering,
                label = "Network Stream",
                onClick = onOpenStreamSection,
                testTag = "hero_stream_button"
            )
        }
    }
}

@Composable
private fun QuickHeroActionChip(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    testTag: String
) {
    AssistChip(
        onClick = onClick,
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        },
        label = { Text(label, style = MaterialTheme.typography.labelMedium, color = Color.White) },
        modifier = Modifier.testTag(testTag)
    )
}

@Composable
fun DragonVideoCard(
    video: VideoEntity,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    onMoveToVault: () -> Unit,
    onDeleteToTrash: () -> Unit,
    onAddToPlaylist: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .dragonGlassSurface(elevated = false, accentGlow = video.isFavorite)
            .clickable { onPlay() }
            .padding(12.dp)
            .testTag("video_card_${video.id}"),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Actual Video Frame Thumbnail Box
        ActualVideoThumbnailBox(
            video = video,
            modifier = Modifier
                .width(136.dp)
                .height(86.dp)
        )

        // Video Metadata & Quick Actions
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = video.title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${video.containerFormat} • ${video.videoCodec}",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.primary
                )
            }

            Text(
                text = buildString {
                    append(video.folderName)
                    if (video.sizeBytes > 0L) {
                        append(" • ")
                        append(StorageScannerHelper.formatSizeBytes(video.sizeBytes))
                    }
                    if (video.lastPositionMs > 1000L) {
                        append(" • Resume ")
                        append(StorageScannerHelper.formatDurationMs(video.lastPositionMs))
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (video.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Toggle favorite",
                        tint = if (video.isFavorite) scheme.primary else scheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onAddToPlaylist,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddToQueue,
                        contentDescription = "Add to playlist",
                        tint = scheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onMoveToVault,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Move to private vault",
                        tint = scheme.tertiary,
                        modifier = Modifier.size(17.dp)
                    )
                }
                IconButton(
                    onClick = onDeleteToTrash,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("delete_video_button_${video.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete video to Trash Bin",
                        tint = scheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Extracts and renders the actual frame thumbnail from the video file or stream URI
 * so each video is immediately recognizable.
 */
@Composable
fun ActualVideoThumbnailBox(
    video: VideoEntity,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    var thumbnailBitmap by remember(video.uri, video.id) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(video.uri, video.id) {
        thumbnailBitmap = StorageScannerHelper.loadVideoThumbnailBitmap(context, video)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        scheme.surfaceVariant,
                        Color(0xFF0B0A12)
                    )
                )
            )
    ) {
        val bmp = thumbnailBitmap
        if (bmp != null && !bmp.isRecycled) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = video.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Clean minimal video icon while frame extracts or for live audio/network stream
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Movie,
                    contentDescription = null,
                    tint = scheme.primary.copy(alpha = 0.45f),
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))
                    )
                )
        )

        // Subtle Center Play Badge
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(32.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play video",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }

        // Resolution Pill Top-Left
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(5.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(Color.Black.copy(alpha = 0.72f))
                .padding(horizontal = 5.dp, vertical = 2.dp)
        ) {
            Text(
                text = video.resolutionBadge,
                style = MaterialTheme.typography.labelSmall,
                color = scheme.primary
            )
        }

        // Duration Badge Bottom-Right
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(5.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(Color.Black.copy(alpha = 0.80f))
                .padding(horizontal = 5.dp, vertical = 2.dp)
        ) {
            Text(
                text = StorageScannerHelper.formatDurationMs(video.durationMs),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White
            )
        }

        // Resume Progress Bar
        if (video.resumeProgressFraction > 0.01f) {
            LinearProgressIndicator(
                progress = { video.resumeProgressFraction },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(4.dp),
                color = scheme.primary,
                trackColor = Color.White.copy(alpha = 0.25f)
            )
        }
    }
}

@Composable
fun FoldersScreenContent(
    folders: List<VideoFolderSummary>,
    onSelectFolder: (String) -> Unit,
    onScanStorage: () -> Unit,
    onMountSafFolder: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("folders_lazy_column"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
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
                        text = "VIDEO FOLDERS",
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.primary
                    )
                    Text(
                        text = "Storage & SD Explorer",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White
                    )
                    Text(
                        text = "Auto-grouped folders from your device & mounted directories",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant
                    )
                }
                Button(onClick = onMountSafFolder) {
                    Icon(Icons.Default.CreateNewFolder, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add Folder")
                }
            }
        }

        if (folders.isEmpty()) {
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
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = scheme.primary,
                        modifier = Modifier.size(46.dp)
                    )
                    Text(
                        text = "No Video Folders Indexed Yet",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                    Text(
                        text = "Mount a folder from your device or SD card, or scan local storage to group your videos by directory.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = onMountSafFolder) {
                            Text("Mount Folder")
                        }
                        OutlinedButton(onClick = onScanStorage) {
                            Text("Scan Device")
                        }
                    }
                }
            }
        } else {
            items(folders, key = { it.folderName }) { folder ->
                Surface(
                    onClick = { onSelectFolder(folder.folderName) },
                    shape = RoundedCornerShape(18.dp),
                    color = Color.Transparent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .dragonGlassSurface(elevated = false, accentGlow = false)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(scheme.primary.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = folder.folderName,
                                tint = scheme.primary,
                                modifier = Modifier.size(30.dp)
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = folder.folderName,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White
                                )
                                Text(
                                    text = "${folder.videoCount} videos",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = scheme.primary
                                )
                            }

                            Text(
                                text = "${StorageScannerHelper.formatSizeBytes(folder.totalSizeBytes)} • Total ${StorageScannerHelper.formatDurationMs(folder.totalDurationMs)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = scheme.tertiary
                            )

                            Text(
                                text = folder.previewVideos.joinToString(" • "),
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyLibraryPlaceholder(
    onScanStorage: () -> Unit,
    onPickVideo: () -> Unit,
    onPickFolder: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .dragonGlassSurface(elevated = false)
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Movie,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp)
        )
        Text(
            text = "Your Video Library is Clean",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White
        )
        Text(
            text = "Open a video file directly, mount a video folder from your storage, or scan your device.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = onPickVideo) {
                Text("Open Video")
            }
            OutlinedButton(onClick = onPickFolder) {
                Text("Mount Folder")
            }
            OutlinedButton(onClick = onScanStorage) {
                Text("Scan Device")
            }
        }
    }
}
