package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.AddToQueue
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewCompact
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.PlaylistEntity
import com.example.data.local.VideoEntity
import com.example.domain.model.DecoderMode
import com.example.domain.model.FolderSortOption
import com.example.domain.model.MediaCardSize
import com.example.domain.model.MediaViewStyle
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
    viewStyle: MediaViewStyle,
    cardSize: MediaCardSize,
    selectedCategory: String,
    selectedFolderFilter: String?,
    decoderMode: DecoderMode,
    hasStoragePermission: Boolean,
    isScanningStorage: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onSortOptionSelect: (VideoSortOption) -> Unit,
    onViewStyleSelect: (MediaViewStyle) -> Unit,
    onCardSizeSelect: (MediaCardSize) -> Unit,
    onCategorySelect: (String) -> Unit,
    onClearFolderFilter: () -> Unit,
    onPlayVideo: (VideoEntity) -> Unit,
    onToggleFavorite: (VideoEntity) -> Unit,
    onMoveToVault: (VideoEntity) -> Unit,
    onDeleteToTrash: (VideoEntity) -> Unit,
    onToggleVideoInPlaylist: (PlaylistEntity, Long) -> Unit,
    onRequestStoragePermission: () -> Unit,
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
        // 0. Storage Permission Quick-Grant Banner if permission not yet granted
        if (!hasStoragePermission) {
            item {
                StoragePermissionGrantBanner(
                    onGrantPermission = onRequestStoragePermission
                )
            }
        }

        // 1. Clean Action Header Card
        item {
            DragonCleanActionCard(
                totalVideos = videos.size,
                decoderMode = decoderMode,
                isScanningStorage = isScanningStorage,
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

        // 3. Viewing Styles & Size Selector Bar
        item {
            MediaViewStyleAndSizeBar(
                selectedStyle = viewStyle,
                selectedSize = cardSize,
                onSelectStyle = onViewStyleSelect,
                onSelectSize = onCardSizeSelect,
                testTagPrefix = "library"
            )
        }

        // 4. Category & Active Folder Filter Chips
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

        // 5. Video Items Rendered According to Viewing Style & Card Size
        if (videos.isEmpty()) {
            item {
                EmptyLibraryPlaceholder(
                    hasStoragePermission = hasStoragePermission,
                    onRequestStoragePermission = onRequestStoragePermission,
                    onScanStorage = onScanStorage,
                    onPickVideo = onPickSingleVideo,
                    onPickFolder = onPickSafFolder
                )
            }
        } else {
            when (viewStyle) {
                MediaViewStyle.DETAILED_LIST -> {
                    items(videos, key = { it.id }) { video ->
                        DragonVideoCard(
                            video = video,
                            cardSize = cardSize,
                            onPlay = { onPlayVideo(video) },
                            onToggleFavorite = { onToggleFavorite(video) },
                            onMoveToVault = { onMoveToVault(video) },
                            onDeleteToTrash = { onDeleteToTrash(video) },
                            onAddToPlaylist = { playlistDialogVideo = video }
                        )
                    }
                }

                MediaViewStyle.COMPACT_LIST -> {
                    items(videos, key = { it.id }) { video ->
                        DragonVideoCompactRow(
                            video = video,
                            cardSize = cardSize,
                            onPlay = { onPlayVideo(video) },
                            onToggleFavorite = { onToggleFavorite(video) },
                            onMoveToVault = { onMoveToVault(video) },
                            onDeleteToTrash = { onDeleteToTrash(video) },
                            onAddToPlaylist = { playlistDialogVideo = video }
                        )
                    }
                }

                MediaViewStyle.GRID_2_COLUMN -> {
                    val rows = videos.chunked(2)
                    items(rows, key = { row -> "grid2_${row.first().id}" }) { rowVideos ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowVideos.forEach { video ->
                                DragonVideoGridTile(
                                    video = video,
                                    columns = 2,
                                    cardSize = cardSize,
                                    onPlay = { onPlayVideo(video) },
                                    onToggleFavorite = { onToggleFavorite(video) },
                                    onMoveToVault = { onMoveToVault(video) },
                                    onDeleteToTrash = { onDeleteToTrash(video) },
                                    onAddToPlaylist = { playlistDialogVideo = video },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (rowVideos.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                MediaViewStyle.GRID_3_COLUMN -> {
                    val rows = videos.chunked(3)
                    items(rows, key = { row -> "grid3_${row.first().id}" }) { rowVideos ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowVideos.forEach { video ->
                                DragonVideoGridTile(
                                    video = video,
                                    columns = 3,
                                    cardSize = cardSize,
                                    onPlay = { onPlayVideo(video) },
                                    onToggleFavorite = { onToggleFavorite(video) },
                                    onMoveToVault = { onMoveToVault(video) },
                                    onDeleteToTrash = { onDeleteToTrash(video) },
                                    onAddToPlaylist = { playlistDialogVideo = video },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            repeat(3 - rowVideos.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                MediaViewStyle.SHOWCASE_BANNER -> {
                    items(videos, key = { it.id }) { video ->
                        DragonVideoShowcaseCard(
                            video = video,
                            cardSize = cardSize,
                            onPlay = { onPlayVideo(video) },
                            onToggleFavorite = { onToggleFavorite(video) },
                            onMoveToVault = { onMoveToVault(video) },
                            onDeleteToTrash = { onDeleteToTrash(video) },
                            onAddToPlaylist = { playlistDialogVideo = video }
                        )
                    }
                }
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
private fun StoragePermissionGrantBanner(
    onGrantPermission: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .dragonGlassSurface(elevated = true, accentGlow = true)
            .padding(14.dp)
            .testTag("storage_permission_banner"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(scheme.primary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = null,
                    tint = scheme.primary
                )
            }
            Column {
                Text(
                    text = "Fast Local Video Import",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Text(
                    text = "Grant storage access to auto-discover all videos & folders on your device",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Button(
            onClick = onGrantPermission,
            modifier = Modifier.testTag("grant_storage_permission_button")
        ) {
            Text("Allow Access")
        }
    }
}

@Composable
private fun DragonCleanActionCard(
    totalVideos: Int,
    decoderMode: DecoderMode,
    isScanningStorage: Boolean,
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
                    text = if (isScanningStorage) "SCANNING LOCAL STORAGE…" else "MEDIA LIBRARY",
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.primary
                )
                Text(
                    text = when {
                        isScanningStorage -> "Importing Local Videos…"
                        totalVideos == 0 -> "Ready to Play"
                        else -> "$totalVideos Videos Indexed"
                    },
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

        if (isScanningStorage) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = scheme.primary,
                trackColor = Color.White.copy(alpha = 0.15f)
            )
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
fun MediaViewStyleAndSizeBar(
    selectedStyle: MediaViewStyle,
    selectedSize: MediaCardSize,
    onSelectStyle: (MediaViewStyle) -> Unit,
    onSelectSize: (MediaCardSize) -> Unit,
    testTagPrefix: String
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .dragonGlassSurface(cornerRadiusOverride = 14.dp, elevated = false)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Viewing Style Selector Pills
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MediaViewStyle.entries.forEach { styleItem ->
                val active = selectedStyle == styleItem
                val icon = when (styleItem) {
                    MediaViewStyle.DETAILED_LIST -> Icons.AutoMirrored.Filled.ViewList
                    MediaViewStyle.COMPACT_LIST -> Icons.Default.ViewCompact
                    MediaViewStyle.GRID_2_COLUMN -> Icons.Default.GridView
                    MediaViewStyle.GRID_3_COLUMN -> Icons.Default.GridOn
                    MediaViewStyle.SHOWCASE_BANNER -> Icons.Default.ViewAgenda
                }
                Surface(
                    onClick = { onSelectStyle(styleItem) },
                    shape = RoundedCornerShape(10.dp),
                    color = if (active) scheme.primary else Color.White.copy(alpha = 0.06f),
                    modifier = Modifier.testTag("${testTagPrefix}_view_style_${styleItem.name.lowercase()}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = styleItem.label,
                            tint = if (active) Color.Black else scheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = styleItem.shortLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (active) Color.Black else Color.White
                        )
                    }
                }
            }
        }

        Spacer(Modifier.width(8.dp))

        // Size Scale Pills (S / M / L)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            MediaCardSize.entries.forEach { sizeItem ->
                val active = selectedSize == sizeItem
                Surface(
                    onClick = { onSelectSize(sizeItem) },
                    shape = RoundedCornerShape(8.dp),
                    color = if (active) scheme.secondary else Color.White.copy(alpha = 0.07f),
                    modifier = Modifier
                        .border(
                            width = 0.8.dp,
                            color = if (active) scheme.secondary else Color.White.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .testTag("${testTagPrefix}_card_size_${sizeItem.name.lowercase()}")
                ) {
                    Text(
                        text = sizeItem.shortLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (active) Color.Black else Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DragonVideoCard(
    video: VideoEntity,
    cardSize: MediaCardSize = MediaCardSize.MEDIUM,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    onMoveToVault: () -> Unit,
    onDeleteToTrash: () -> Unit,
    onAddToPlaylist: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val thumbW = (136f * cardSize.scaleFactor).dp
    val thumbH = (86f * cardSize.scaleFactor).dp
    val pad = (12f * cardSize.scaleFactor).coerceIn(8f, 16f).dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .dragonGlassSurface(elevated = false, accentGlow = video.isFavorite)
            .clickable { onPlay() }
            .padding(pad)
            .testTag("video_card_${video.id}"),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Actual Video Frame Thumbnail Box
        ActualVideoThumbnailBox(
            video = video,
            compactMode = cardSize == MediaCardSize.SMALL,
            modifier = Modifier
                .width(thumbW)
                .height(thumbH)
        )

        // Video Metadata & Quick Actions
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = video.title,
                style = if (cardSize == MediaCardSize.LARGE) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
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

@Composable
private fun DragonVideoCompactRow(
    video: VideoEntity,
    cardSize: MediaCardSize,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    onMoveToVault: () -> Unit,
    onDeleteToTrash: () -> Unit,
    onAddToPlaylist: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val thumbW = (76f * cardSize.scaleFactor).dp
    val thumbH = (48f * cardSize.scaleFactor).dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .dragonGlassSurface(cornerRadiusOverride = 12.dp, elevated = false, accentGlow = video.isFavorite)
            .clickable { onPlay() }
            .padding(horizontal = 10.dp, vertical = (7f * cardSize.scaleFactor).dp)
            .testTag("video_card_${video.id}"),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ActualVideoThumbnailBox(
            video = video,
            compactMode = true,
            modifier = Modifier
                .width(thumbW)
                .height(thumbH)
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = video.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${video.resolutionBadge} • ${StorageScannerHelper.formatDurationMs(video.durationMs)} • ${video.containerFormat}",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.primary,
                maxLines = 1
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onToggleFavorite, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = if (video.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Toggle favorite",
                    tint = if (video.isFavorite) scheme.primary else scheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
            IconButton(onClick = onAddToPlaylist, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Default.AddToQueue,
                    contentDescription = "Add to playlist",
                    tint = scheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
            IconButton(onClick = onMoveToVault, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Move to vault",
                    tint = scheme.tertiary,
                    modifier = Modifier.size(15.dp)
                )
            }
            IconButton(
                onClick = onDeleteToTrash,
                modifier = Modifier
                    .size(28.dp)
                    .testTag("delete_video_button_${video.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete video",
                    tint = scheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun DragonVideoGridTile(
    video: VideoEntity,
    columns: Int,
    cardSize: MediaCardSize,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    onMoveToVault: () -> Unit,
    onDeleteToTrash: () -> Unit,
    onAddToPlaylist: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val baseHeight = if (columns == 2) 110f else 78f
    val thumbH = (baseHeight * cardSize.scaleFactor).dp

    Column(
        modifier = modifier
            .dragonGlassSurface(cornerRadiusOverride = 14.dp, elevated = false, accentGlow = video.isFavorite)
            .clickable { onPlay() }
            .padding(if (columns == 2) 10.dp else 7.dp)
            .testTag("video_card_${video.id}"),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        ActualVideoThumbnailBox(
            video = video,
            compactMode = columns >= 3,
            modifier = Modifier
                .fillMaxWidth()
                .height(thumbH)
        )

        Text(
            text = video.title,
            style = if (columns == 2) MaterialTheme.typography.titleSmall else MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = if (columns == 2) "${video.containerFormat} • ${StorageScannerHelper.formatDurationMs(video.durationMs)}"
            else StorageScannerHelper.formatDurationMs(video.durationMs),
            style = MaterialTheme.typography.labelSmall,
            color = scheme.primary,
            maxLines = 1
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onToggleFavorite, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = if (video.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (video.isFavorite) scheme.primary else scheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
            }
            if (columns == 2) {
                IconButton(onClick = onAddToPlaylist, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Default.AddToQueue,
                        contentDescription = "Playlist",
                        tint = scheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                }
                IconButton(onClick = onMoveToVault, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Vault",
                        tint = scheme.tertiary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            IconButton(
                onClick = onDeleteToTrash,
                modifier = Modifier
                    .size(24.dp)
                    .testTag("delete_video_button_${video.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete",
                    tint = scheme.primary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun DragonVideoShowcaseCard(
    video: VideoEntity,
    cardSize: MediaCardSize,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    onMoveToVault: () -> Unit,
    onDeleteToTrash: () -> Unit,
    onAddToPlaylist: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val bannerHeight = (176f * cardSize.scaleFactor).dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .dragonGlassSurface(elevated = true, accentGlow = video.isFavorite)
            .clickable { onPlay() }
            .padding(12.dp)
            .testTag("video_card_${video.id}"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ActualVideoThumbnailBox(
            video = video,
            compactMode = false,
            modifier = Modifier
                .fillMaxWidth()
                .height(bannerHeight)
        )

        if (video.resumeProgressFraction > 0.01f) {
            LinearProgressIndicator(
                progress = { video.resumeProgressFraction },
                color = scheme.primary,
                trackColor = Color.White.copy(alpha = 0.14f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${video.folderName} • ${video.containerFormat} • ${video.videoCodec}",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.primary,
                    maxLines = 1
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onToggleFavorite, modifier = Modifier.size(30.dp)) {
                    Icon(
                        imageVector = if (video.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (video.isFavorite) scheme.primary else scheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onAddToPlaylist, modifier = Modifier.size(30.dp)) {
                    Icon(
                        imageVector = Icons.Default.AddToQueue,
                        contentDescription = "Playlist",
                        tint = scheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onMoveToVault, modifier = Modifier.size(30.dp)) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Vault",
                        tint = scheme.tertiary,
                        modifier = Modifier.size(17.dp)
                    )
                }
                IconButton(
                    onClick = onDeleteToTrash,
                    modifier = Modifier
                        .size(30.dp)
                        .testTag("delete_video_button_${video.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
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
    modifier: Modifier = Modifier,
    compactMode: Boolean = false
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    var thumbnailBitmap by remember(video.uri, video.id) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(video.uri, video.id) {
        thumbnailBitmap = StorageScannerHelper.loadVideoThumbnailBitmap(context, video)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(if (compactMode) 8.dp else 12.dp))
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
                    modifier = Modifier.size(if (compactMode) 18.dp else 32.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = if (compactMode) 0.45f else 0.65f))
                    )
                )
        )

        // Subtle Center Play Badge
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(if (compactMode) 18.dp else 32.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play video",
                tint = Color.White,
                modifier = Modifier.size(if (compactMode) 11.dp else 18.dp)
            )
        }

        if (!compactMode) {
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
        }
    }
}

@Composable
fun FoldersScreenContent(
    folders: List<VideoFolderSummary>,
    hasStoragePermission: Boolean,
    folderViewStyle: MediaViewStyle,
    folderCardSize: MediaCardSize,
    folderSortOption: FolderSortOption,
    folderSortAscending: Boolean,
    onSelectFolder: (String) -> Unit,
    onSelectFolderViewStyle: (MediaViewStyle) -> Unit,
    onSelectFolderCardSize: (MediaCardSize) -> Unit,
    onSelectFolderSortOption: (FolderSortOption) -> Unit,
    onRequestStoragePermission: () -> Unit,
    onScanStorage: () -> Unit,
    onMountSafFolder: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    var folderSortMenuExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("folders_lazy_column"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!hasStoragePermission) {
            item {
                StoragePermissionGrantBanner(
                    onGrantPermission = onRequestStoragePermission
                )
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .dragonGlassSurface(cornerRadiusOverride = 14.dp, elevated = false, accentGlow = true)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag("compact_storage_explorer_panel"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(scheme.primary.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = scheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Storage & SD Explorer",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1
                        )
                        Text(
                            text = "${folders.size} folder(s) indexed",
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Folder Sort Button
                    Box {
                        Surface(
                            onClick = { folderSortMenuExpanded = true },
                            shape = RoundedCornerShape(8.dp),
                            color = scheme.surfaceVariant.copy(alpha = 0.70f),
                            modifier = Modifier.testTag("folder_sort_menu_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Sort,
                                    contentDescription = "Sort Folders",
                                    tint = scheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "${folderSortOption.label} ${if (folderSortAscending) "↑" else "↓"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = folderSortMenuExpanded,
                            onDismissRequest = { folderSortMenuExpanded = false }
                        ) {
                            FolderSortOption.entries.forEach { opt ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = if (opt == folderSortOption) {
                                                "${opt.label} (${if (folderSortAscending) "Asc" else "Desc"})"
                                            } else {
                                                opt.label
                                            }
                                        )
                                    },
                                    onClick = {
                                        onSelectFolderSortOption(opt)
                                        folderSortMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Surface(
                        onClick = onMountSafFolder,
                        shape = RoundedCornerShape(8.dp),
                        color = scheme.primary,
                        modifier = Modifier.testTag("add_saf_folder_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CreateNewFolder,
                                contentDescription = "Add Folder",
                                tint = Color.Black,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Add",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                }
            }
        }

        // Folder Viewing Styles & Size Bar
        item {
            MediaViewStyleAndSizeBar(
                selectedStyle = folderViewStyle,
                selectedSize = folderCardSize,
                onSelectStyle = onSelectFolderViewStyle,
                onSelectSize = onSelectFolderCardSize,
                testTagPrefix = "folders"
            )
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
                        text = "Allow storage permission to auto-scan your device folders, or mount any directory from internal storage or SD card.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (!hasStoragePermission) {
                            Button(onClick = onRequestStoragePermission) {
                                Text("Grant Storage Access")
                            }
                        } else {
                            Button(onClick = onScanStorage) {
                                Text("Scan Device")
                            }
                        }
                        OutlinedButton(onClick = onMountSafFolder) {
                            Text("Mount Folder")
                        }
                    }
                }
            }
        } else {
            when (folderViewStyle) {
                MediaViewStyle.DETAILED_LIST -> {
                    items(folders, key = { it.folderName }) { folder ->
                        DragonFolderDetailedCard(
                            folder = folder,
                            cardSize = folderCardSize,
                            onClick = { onSelectFolder(folder.folderName) }
                        )
                    }
                }

                MediaViewStyle.COMPACT_LIST -> {
                    items(folders, key = { it.folderName }) { folder ->
                        DragonFolderCompactRow(
                            folder = folder,
                            cardSize = folderCardSize,
                            onClick = { onSelectFolder(folder.folderName) }
                        )
                    }
                }

                MediaViewStyle.GRID_2_COLUMN -> {
                    val rows = folders.chunked(2)
                    items(rows, key = { row -> "fgrid2_${row.first().folderName}" }) { rowFolders ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowFolders.forEach { folder ->
                                DragonFolderGridTile(
                                    folder = folder,
                                    columns = 2,
                                    cardSize = folderCardSize,
                                    onClick = { onSelectFolder(folder.folderName) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (rowFolders.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                MediaViewStyle.GRID_3_COLUMN -> {
                    val rows = folders.chunked(3)
                    items(rows, key = { row -> "fgrid3_${row.first().folderName}" }) { rowFolders ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowFolders.forEach { folder ->
                                DragonFolderGridTile(
                                    folder = folder,
                                    columns = 3,
                                    cardSize = folderCardSize,
                                    onClick = { onSelectFolder(folder.folderName) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            repeat(3 - rowFolders.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                MediaViewStyle.SHOWCASE_BANNER -> {
                    items(folders, key = { it.folderName }) { folder ->
                        DragonFolderShowcaseCard(
                            folder = folder,
                            cardSize = folderCardSize,
                            onClick = { onSelectFolder(folder.folderName) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DragonFolderDetailedCard(
    folder: VideoFolderSummary,
    cardSize: MediaCardSize,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val iconBoxSize = (54f * cardSize.scaleFactor).dp
    val iconSize = (30f * cardSize.scaleFactor).dp

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .dragonGlassSurface(elevated = false, accentGlow = false)
            .testTag("folder_card_${folder.folderName}")
    ) {
        Row(
            modifier = Modifier.padding((15f * cardSize.scaleFactor).coerceIn(10f, 20f).dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(iconBoxSize)
                    .clip(RoundedCornerShape(14.dp))
                    .background(scheme.primary.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = folder.folderName,
                    tint = scheme.primary,
                    modifier = Modifier.size(iconSize)
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
                        style = if (cardSize == MediaCardSize.LARGE) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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

@Composable
private fun DragonFolderCompactRow(
    folder: VideoFolderSummary,
    cardSize: MediaCardSize,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .dragonGlassSurface(cornerRadiusOverride = 12.dp, elevated = false)
            .testTag("folder_card_${folder.folderName}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = (9f * cardSize.scaleFactor).dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = folder.folderName,
                    tint = scheme.primary,
                    modifier = Modifier.size((22f * cardSize.scaleFactor).dp)
                )
                Text(
                    text = folder.folderName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = "${folder.videoCount} videos • ${StorageScannerHelper.formatSizeBytes(folder.totalSizeBytes)}",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.primary
            )
        }
    }
}

@Composable
private fun DragonFolderGridTile(
    folder: VideoFolderSummary,
    columns: Int,
    cardSize: MediaCardSize,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val thumbH = ((if (columns == 2) 96f else 68f) * cardSize.scaleFactor).dp

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
        modifier = modifier
            .dragonGlassSurface(cornerRadiusOverride = 16.dp, elevated = false)
            .testTag("folder_card_${folder.folderName}")
    ) {
        Column(
            modifier = Modifier.padding(if (columns == 2) 10.dp else 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(thumbH)
                    .clip(RoundedCornerShape(10.dp))
                    .background(scheme.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                if (folder.leadVideo != null) {
                    ActualVideoThumbnailBox(
                        video = folder.leadVideo,
                        compactMode = true,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.72f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = scheme.primary,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "${folder.videoCount}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Text(
                text = folder.folderName,
                style = if (columns == 2) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = if (columns == 2) {
                    "${folder.videoCount} videos • ${StorageScannerHelper.formatSizeBytes(folder.totalSizeBytes)}"
                } else {
                    StorageScannerHelper.formatSizeBytes(folder.totalSizeBytes)
                },
                style = MaterialTheme.typography.labelSmall,
                color = scheme.primary,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun DragonFolderShowcaseCard(
    folder: VideoFolderSummary,
    cardSize: MediaCardSize,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val bannerH = (152f * cardSize.scaleFactor).dp

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .dragonGlassSurface(elevated = true, accentGlow = true)
            .testTag("folder_card_${folder.folderName}")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(bannerH)
                    .clip(RoundedCornerShape(14.dp))
                    .background(scheme.primary.copy(alpha = 0.16f))
            ) {
                if (folder.leadVideo != null) {
                    ActualVideoThumbnailBox(
                        video = folder.leadVideo,
                        compactMode = false,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = scheme.primary,
                        modifier = Modifier
                            .size(56.dp)
                            .align(Alignment.Center)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = folder.folderName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1
                    )
                    Text(
                        text = folder.previewVideos.joinToString(" • "),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = "${folder.videoCount} videos • ${StorageScannerHelper.formatSizeBytes(folder.totalSizeBytes)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.primary
                )
            }
        }
    }
}

@Composable
private fun EmptyLibraryPlaceholder(
    hasStoragePermission: Boolean,
    onRequestStoragePermission: () -> Unit,
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
            text = if (!hasStoragePermission) {
                "Allow storage access to automatically import all local video files, or open a video file directly."
            } else {
                "Open a video file directly, mount a video folder from your storage, or scan your device."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (!hasStoragePermission) {
                Button(onClick = onRequestStoragePermission) {
                    Text("Allow Storage")
                }
            } else {
                Button(onClick = onScanStorage) {
                    Text("Scan Device")
                }
            }
            OutlinedButton(onClick = onPickVideo) {
                Text("Open Video")
            }
            OutlinedButton(onClick = onPickFolder) {
                Text("Mount Folder")
            }
        }
    }
}
