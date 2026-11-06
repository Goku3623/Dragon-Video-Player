package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.local.VideoEntity
import com.example.domain.model.DecoderMode
import com.example.player.StorageScannerHelper
import com.example.ui.theme.DragonThemeConfig
import com.example.ui.theme.DragonThemePreset
import com.example.ui.theme.dragonGlassSurface
import com.example.ui.theme.dragonScaleMesh
import com.example.ui.theme.hueToColor
import com.example.ui.viewmodel.MoreSubSection

/**
 * The "More" Hub Screen positioned after Playlists.
 * Houses dedicated cards for Network Stream (separate from Codec section),
 * Trash Bin, Private Dragon Vault, Dynamic Theme Studio, Codec & Decoder Engine,
 * and Background Audio toggle.
 */
@Composable
fun MoreHubScreenContent(
    activeThemeTitle: String,
    decoderMode: DecoderMode,
    vaultVideoCount: Int,
    trashVideoCount: Int,
    backgroundAudioEnabled: Boolean,
    onOpenSubSection: (MoreSubSection) -> Unit,
    onToggleBackgroundAudio: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("more_hub_lazy_column"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .dragonGlassSurface(elevated = true, accentGlow = true)
                    .dragonScaleMesh(scaleAlpha = 0.09f)
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "DRAGON CONTROL CENTER",
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.primary
                )
                Text(
                    text = "More & Studio Tools",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White
                )
                Text(
                    text = "Access Network Streaming, Trash Bin, Private Vault, Dynamic Theme Customizer, and Codec Engine settings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant
                )
            }
        }

        // 1. Network Stream Section Card (Separate from Codec section)
        item {
            MoreFeatureSectionCard(
                icon = Icons.Default.WifiTethering,
                title = "Network Stream",
                badge = "HLS • DASH • RTSP",
                description = "Play direct .m3u8, .mpd, SmoothStreaming, RTSP, and HTTP/HTTPS video streams",
                accentColor = scheme.primary,
                onClick = { onOpenSubSection(MoreSubSection.NETWORK_STREAM) },
                testTag = "more_card_network_stream"
            )
        }

        // 2. Trash Bin Section Card
        item {
            MoreFeatureSectionCard(
                icon = Icons.Default.DeleteOutline,
                title = "Trash Bin",
                badge = if (trashVideoCount > 0) "$trashVideoCount Deleted" else "Empty",
                description = "View deleted videos — restore them back to your library or delete them forever",
                accentColor = scheme.primary,
                onClick = { onOpenSubSection(MoreSubSection.TRASH_BIN) },
                testTag = "more_card_trash_bin"
            )
        }

        // 3. Private Dragon Vault Section Card
        item {
            MoreFeatureSectionCard(
                icon = Icons.Default.Lock,
                title = "Dragon Vault",
                badge = if (vaultVideoCount > 0) "$vaultVideoCount Hidden" else "PIN Protected",
                description = "Private PIN-locked folder vault to hide sensitive videos from public folders",
                accentColor = scheme.tertiary,
                onClick = { onOpenSubSection(MoreSubSection.VAULT) },
                testTag = "more_card_vault"
            )
        }

        // 4. Dynamic Theme Studio Section Card
        item {
            MoreFeatureSectionCard(
                icon = Icons.Default.Palette,
                title = "Theme Studio",
                badge = activeThemeTitle,
                description = "Switch Dragon Flame, Abyssal Drake, Emerald Wyrm & Material You with live glow & blur sliders",
                accentColor = scheme.secondary,
                onClick = { onOpenSubSection(MoreSubSection.THEME_STUDIO) },
                testTag = "more_card_theme_studio"
            )
        }

        // 5. Codec & Decoder Engine Section Card (Separate from Network Stream)
        item {
            MoreFeatureSectionCard(
                icon = Icons.Default.DeveloperBoard,
                title = "Codec & Decoder Engine",
                badge = "Engine ${decoderMode.badge}",
                description = "Configure Hardware+ acceleration and bundled FFmpeg, libvpx, libdav1d, Opus & FLAC decoders",
                accentColor = scheme.primary,
                onClick = { onOpenSubSection(MoreSubSection.CODEC_ENGINE) },
                testTag = "more_card_codec_engine"
            )
        }

        // 6. Background Audio Playback Quick Setting
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .dragonGlassSurface(elevated = false)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(scheme.primary.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Headphones,
                            contentDescription = null,
                            tint = scheme.primary
                        )
                    }
                    Column {
                        Text(
                            text = "Background Audio Playback",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Text(
                            text = "Continue audio playback when minimizing player or locking screen",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = backgroundAudioEnabled,
                    onCheckedChange = onToggleBackgroundAudio
                )
            }
        }
    }
}

@Composable
private fun MoreFeatureSectionCard(
    icon: ImageVector,
    title: String,
    badge: String,
    description: String,
    accentColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .dragonGlassSurface(elevated = false, accentGlow = true)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accentColor.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(28.dp)
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
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(accentColor.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = badge,
                            style = MaterialTheme.typography.labelSmall,
                            color = accentColor
                        )
                    }
                }
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Open $title",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Dedicated Trash Bin Screen inside the More Section.
 * Allows restoring deleted videos back to the Library or deleting them forever from storage.
 */
@Composable
fun TrashBinScreenContent(
    trashedVideos: List<VideoEntity>,
    onBackToMore: () -> Unit,
    onRestoreVideo: (VideoEntity) -> Unit,
    onDeleteForever: (VideoEntity) -> Unit,
    onRestoreAll: () -> Unit,
    onEmptyTrashForever: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    var confirmDeleteVideo by remember { mutableStateOf<VideoEntity?>(null) }
    var confirmEmptyAll by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("trash_bin_lazy_column"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            MoreSubScreenHeader(
                title = "Trash Bin",
                subtitle = "Restore deleted videos or permanently erase them from storage",
                onBack = onBackToMore
            )
        }

        if (trashedVideos.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .dragonGlassSurface(elevated = true, accentGlow = true)
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${trashedVideos.size} Deleted Video(s)",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Text(
                            text = "Total size: ${StorageScannerHelper.formatSizeBytes(trashedVideos.sumOf { it.sizeBytes })}",
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.primary
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onRestoreAll,
                            modifier = Modifier.testTag("restore_all_trash_button")
                        ) {
                            Icon(Icons.Default.RestoreFromTrash, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Restore All")
                        }
                        Button(
                            onClick = { confirmEmptyAll = true },
                            colors = ButtonDefaults.buttonColors(containerColor = scheme.primary),
                            modifier = Modifier.testTag("empty_trash_forever_button")
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Empty")
                        }
                    }
                }
            }
        }

        if (trashedVideos.isEmpty()) {
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
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = null,
                        tint = scheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Trash Bin is Empty",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                    Text(
                        text = "Deleted videos will be safely stored here before permanent removal.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(trashedVideos, key = { it.id }) { video ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .dragonGlassSurface(elevated = false)
                        .padding(12.dp)
                        .testTag("trashed_video_card_${video.id}"),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ActualVideoThumbnailBox(
                        video = video,
                        modifier = Modifier.size(width = 116.dp, height = 74.dp)
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = video.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            maxLines = 1
                        )
                        Text(
                            text = "${video.folderName} • ${StorageScannerHelper.formatSizeBytes(video.sizeBytes)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                            maxLines = 1
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { onRestoreVideo(video) },
                                modifier = Modifier.testTag("restore_video_button_${video.id}")
                            ) {
                                Icon(Icons.Default.RestoreFromTrash, contentDescription = "Restore", modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Restore", style = MaterialTheme.typography.labelMedium)
                            }
                            Button(
                                onClick = { confirmDeleteVideo = video },
                                colors = ButtonDefaults.buttonColors(containerColor = scheme.primary),
                                modifier = Modifier.testTag("delete_forever_button_${video.id}")
                            ) {
                                Icon(Icons.Default.DeleteForever, contentDescription = "Delete Forever", modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Delete Forever", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }
    }

    // Single Video Permanent Deletion Confirmation Dialog
    confirmDeleteVideo?.let { target ->
        AlertDialog(
            onDismissRequest = { confirmDeleteVideo = null },
            title = { Text("Delete Video Forever?") },
            text = {
                Text("Are you sure you want to permanently delete '${target.title}'? This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val videoToDelete = target
                        confirmDeleteVideo = null
                        onDeleteForever(videoToDelete)
                    }
                ) {
                    Text("Delete Forever")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteVideo = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Empty Entire Trash Bin Confirmation Dialog
    if (confirmEmptyAll) {
        AlertDialog(
            onDismissRequest = { confirmEmptyAll = false },
            title = { Text("Empty Trash Bin Forever?") },
            text = {
                Text("Permanently delete all ${trashedVideos.size} video(s) in the Trash Bin from your device?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        confirmEmptyAll = false
                        onEmptyTrashForever()
                    }
                ) {
                    Text("Delete All Forever")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmEmptyAll = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun PrivateVaultScreenContent(
    isUnlocked: Boolean,
    vaultVideos: List<VideoEntity>,
    onBackToMore: () -> Unit,
    onAttemptUnlock: (String) -> Boolean,
    onLockVault: () -> Unit,
    onUpdatePin: (String) -> Unit,
    onPlayVaultVideo: (VideoEntity) -> Unit,
    onRestoreToLibrary: (VideoEntity) -> Unit,
    onDeleteVaultVideoToTrash: (VideoEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var enteredPin by remember { mutableStateOf("") }
    var newPinInput by remember { mutableStateOf("") }
    val scheme = MaterialTheme.colorScheme

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("vault_lazy_column"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            MoreSubScreenHeader(
                title = "Dragon Vault",
                subtitle = "PIN-protected private media folder",
                onBack = onBackToMore
            )
        }

        if (!isUnlocked) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .dragonGlassSurface(elevated = true, accentGlow = true)
                        .dragonScaleMesh(scaleAlpha = 0.1f)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(scheme.primary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Vault Lock",
                            tint = scheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Text(
                        text = "Private Dragon Vault",
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White
                    )
                    Text(
                        text = "Hidden videos are excluded from public folders and playlists. Enter your 4-digit PIN (Default: 1234).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = enteredPin,
                        onValueChange = { if (it.length <= 8) enteredPin = it },
                        label = { Text("Vault PIN (Default: 1234)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("vault_pin_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onAttemptUnlock(enteredPin) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("unlock_vault_submit_button")
                        ) {
                            Icon(Icons.Default.LockOpen, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Unlock Vault")
                        }
                        OutlinedButton(
                            onClick = {
                                enteredPin = "1234"
                                onAttemptUnlock("1234")
                            },
                            modifier = Modifier.testTag("quick_demo_unlock_button")
                        ) {
                            Text("Quick Unlock (1234)")
                        }
                    }
                }
            }
        } else {
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
                        Text("DRAGON VAULT UNLOCKED", style = MaterialTheme.typography.labelMedium, color = scheme.primary)
                        Text("${vaultVideos.size} Hidden Videos", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                    }
                    OutlinedButton(onClick = onLockVault) {
                        Icon(Icons.Default.Lock, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Lock Vault")
                    }
                }
            }

            if (vaultVideos.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .dragonGlassSurface(elevated = false)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Vault is Empty", style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Text(
                            text = "Tap the lock icon on any video card in your Library to move it to this private vault.",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(vaultVideos, key = { it.id }) { video ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .dragonGlassSurface(elevated = false)
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ActualVideoThumbnailBox(
                            video = video,
                            modifier = Modifier.size(width = 110.dp, height = 70.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(video.title, style = MaterialTheme.typography.titleMedium, color = Color.White, maxLines = 1)
                            Text(
                                text = "${video.resolutionBadge} • ${StorageScannerHelper.formatDurationMs(video.durationMs)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = scheme.primary
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 6.dp)
                            ) {
                                Button(onClick = { onPlayVaultVideo(video) }) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Play", modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Play")
                                }
                                OutlinedButton(onClick = { onRestoreToLibrary(video) }) {
                                    Text("Unhide")
                                }
                                IconButton(
                                    onClick = { onDeleteVaultVideoToTrash(video) },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Move to Trash Bin",
                                        tint = scheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .dragonGlassSurface(elevated = false)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Change Vault PIN", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newPinInput,
                            onValueChange = { if (it.length <= 8) newPinInput = it },
                            label = { Text("New 4+ Digit PIN") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                if (newPinInput.length >= 4) {
                                    onUpdatePin(newPinInput)
                                    newPinInput = ""
                                }
                            }
                        ) {
                            Text("Update PIN")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ThemeCustomizerScreenContent(
    themeConfig: DragonThemeConfig,
    onBackToMore: () -> Unit,
    onSelectPreset: (DragonThemePreset) -> Unit,
    onUpdateTheme: ((DragonThemeConfig) -> DragonThemeConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("theme_studio_lazy_column"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            MoreSubScreenHeader(
                title = "Theme Studio",
                subtitle = "Dynamic Dragon presets, neon glow, HUD border radius & backdrop blur",
                onBack = onBackToMore
            )
        }

        // Live HUD Preview Card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .dragonGlassSurface(elevated = true, accentGlow = true)
                    .dragonScaleMesh(scaleAlpha = 0.12f)
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .dragonGlassSurface(elevated = false, accentGlow = true)
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("LIVE HUD PREVIEW", style = MaterialTheme.typography.labelSmall, color = scheme.primary)
                            Text(
                                text = themeConfig.preset.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(themeConfig.hudCornerRadiusDp.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(scheme.primary, scheme.secondary)
                                    )
                                )
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Radius ${themeConfig.hudCornerRadiusDp.toInt()}dp • Glow ${(themeConfig.glowIntensity * 100).toInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 1. Dynamic Theme Presets
        item {
            Text(
                text = "Signature Dragon Presets",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
        }

        items(DragonThemePreset.entries) { preset ->
            val selected = themeConfig.preset == preset && !themeConfig.useCustomAccent
            Surface(
                onClick = { onSelectPreset(preset) },
                shape = RoundedCornerShape(themeConfig.hudCornerRadiusDp.dp),
                color = Color.Transparent,
                modifier = Modifier
                    .fillMaxWidth()
                    .dragonGlassSurface(elevated = selected, accentGlow = selected)
                    .testTag("theme_preset_${preset.name.lowercase()}")
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
                        listOf(preset.primaryAccent, preset.secondaryAccent, preset.tertiaryGlow).forEach { c ->
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .border(1.5.dp, Color.Black, CircleShape)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = preset.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Text(
                            text = preset.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant
                        )
                    }

                    if (selected) {
                        Text(
                            text = "ACTIVE",
                            style = MaterialTheme.typography.labelMedium,
                            color = scheme.primary
                        )
                    }
                }
            }
        }

        // 2. Real-Time Glassmorphism & Glow Sliders
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .dragonGlassSurface(elevated = true)
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = scheme.primary)
                    Text(
                        text = "Real-Time HUD & Shader Controls",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Custom Neon Accent Hue Override", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                    Switch(
                        checked = themeConfig.useCustomAccent,
                        onCheckedChange = { enabled -> onUpdateTheme { it.copy(useCustomAccent = enabled) } }
                    )
                }

                if (themeConfig.useCustomAccent) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(hueToColor(themeConfig.customAccentHue))
                        )
                        Slider(
                            value = themeConfig.customAccentHue,
                            onValueChange = { hue -> onUpdateTheme { it.copy(customAccentHue = hue) } },
                            valueRange = 0f..360f,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Text(
                    text = "Neon Glow Intensity: ${(themeConfig.glowIntensity * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White
                )
                Slider(
                    value = themeConfig.glowIntensity,
                    onValueChange = { g -> onUpdateTheme { it.copy(glowIntensity = g) } },
                    valueRange = 0.05f..1.0f,
                    modifier = Modifier.testTag("glow_intensity_slider")
                )

                Text(
                    text = "HUD & Card Border Radius: ${themeConfig.hudCornerRadiusDp.toInt()} dp",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White
                )
                Slider(
                    value = themeConfig.hudCornerRadiusDp,
                    onValueChange = { r -> onUpdateTheme { it.copy(hudCornerRadiusDp = r) } },
                    valueRange = 4f..32f,
                    modifier = Modifier.testTag("hud_corner_radius_slider")
                )

                Text(
                    text = "Backdrop Blur & Frosted Glass Strength: ${(themeConfig.backdropBlurStrength * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White
                )
                Slider(
                    value = themeConfig.backdropBlurStrength,
                    onValueChange = { b -> onUpdateTheme { it.copy(backdropBlurStrength = b) } },
                    valueRange = 0.2f..1.0f,
                    modifier = Modifier.testTag("backdrop_blur_slider")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Dragon-Scale Geometric Accent Motif", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                    Switch(
                        checked = themeConfig.showDragonScalePattern,
                        onCheckedChange = { show -> onUpdateTheme { it.copy(showDragonScalePattern = show) } }
                    )
                }
            }
        }
    }
}
