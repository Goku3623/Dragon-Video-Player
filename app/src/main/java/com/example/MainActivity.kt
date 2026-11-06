package com.example

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.DecoderMode
import com.example.player.StorageScannerHelper
import com.example.ui.player.AudioAndEqualizerSheet
import com.example.ui.player.DragonBackgroundAudioNotificationPanel
import com.example.ui.player.DragonPlayerHudOverlay
import com.example.ui.player.DragonSystemAudioNotificationHelper
import com.example.ui.player.GestureVideoSurface
import com.example.ui.player.SubtitleCustomizerSheet
import com.example.ui.screens.CodecEngineScreenContent
import com.example.ui.screens.DragonIntroAnimationOverlay
import com.example.ui.screens.FoldersScreenContent
import com.example.ui.screens.LibraryScreenContent
import com.example.ui.screens.MoreHubScreenContent
import com.example.ui.screens.NetworkStreamScreenContent
import com.example.ui.screens.PlaylistsScreenContent
import com.example.ui.screens.PrivateVaultScreenContent
import com.example.ui.screens.ThemeCustomizerScreenContent
import com.example.ui.screens.TrashBinScreenContent
import com.example.ui.theme.DragonVideoPlayerTheme
import com.example.ui.theme.dragonGlassSurface
import com.example.ui.theme.dragonScaleMesh
import com.example.ui.viewmodel.DragonNavTab
import com.example.ui.viewmodel.DragonPlayerViewModel
import com.example.ui.viewmodel.MoreSubSection
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private val viewModel: DragonPlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Keep app brightness identical to device brightness on launch
        val params = window.attributes
        params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE

        // Set display to device default maximum refresh rate (e.g. 120Hz/90Hz/144Hz) for fluid animation
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            runCatching {
                val dm = getSystemService(android.content.Context.DISPLAY_SERVICE) as? android.hardware.display.DisplayManager
                val defaultDisplay = dm?.getDisplay(android.view.Display.DEFAULT_DISPLAY)
                val maxMode = defaultDisplay?.supportedModes?.maxByOrNull { it.refreshRate }
                if (maxMode != null) {
                    params.preferredDisplayModeId = maxMode.modeId
                }
            }
        }

        window.attributes = params
        viewModel.syncBrightnessWithDevice()
        handleIncomingMediaIntent(intent)

        setContent {
            val themeConfig by viewModel.themeConfig.collectAsStateWithLifecycle()
            DragonVideoPlayerTheme(themeConfig = themeConfig) {
                DragonVideoPlayerRoot(
                    viewModel = viewModel,
                    onRequestPipMode = { enterDragonPipMode() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.syncBrightnessWithDevice()
        val state = viewModel.playerState.value
        if (!state.isPlayerOverlayOpen || !state.isBrightnessOverridden) {
            val params = window.attributes
            params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            window.attributes = params
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingMediaIntent(intent)
    }

    private fun handleIncomingMediaIntent(intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_VIEW -> {
                intent.data?.let { uri ->
                    viewModel.onSingleVideoPicked(uri)
                }
            }
            DragonSystemAudioNotificationHelper.ACTION_BG_EXPAND_PLAYER -> viewModel.resumePlayerOverlayFromBackground()
            DragonSystemAudioNotificationHelper.ACTION_BG_PLAY_PAUSE -> viewModel.togglePlayPause()
            DragonSystemAudioNotificationHelper.ACTION_BG_REWIND -> viewModel.seekRelative(-10_000L)
            DragonSystemAudioNotificationHelper.ACTION_BG_FORWARD -> viewModel.seekRelative(10_000L)
            DragonSystemAudioNotificationHelper.ACTION_BG_NEXT -> viewModel.playNextInQueue()
            DragonSystemAudioNotificationHelper.ACTION_BG_STOP -> viewModel.stopBackgroundAudioSession()
        }
    }

    private fun enterDragonPipMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            runCatching {
                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .build()
                enterPictureInPictureMode(params)
            }
        } else {
            viewModel.showBannerMessage("Picture-in-Picture requires Android 8.0+")
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        val state = viewModel.playerState.value
        if (state.isPlayerOverlayOpen && state.isPlaying && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            enterDragonPipMode()
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        viewModel.setPipModeState(isInPictureInPictureMode)
    }
}

@Composable
fun DragonVideoPlayerRoot(
    viewModel: DragonPlayerViewModel,
    onRequestPipMode: () -> Unit
) {
    val context = LocalContext.current
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val moreSubSection by viewModel.moreSubSection.collectAsStateWithLifecycle()
    val videos by viewModel.filteredVideos.collectAsStateWithLifecycle()
    val folders by viewModel.folderSummaries.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val vaultVideos by viewModel.vaultVideos.collectAsStateWithLifecycle()
    val trashedVideos by viewModel.trashedVideos.collectAsStateWithLifecycle()
    val savedNetworkStreams by viewModel.savedNetworkStreams.collectAsStateWithLifecycle()
    val streamBookmarks by viewModel.streamBookmarks.collectAsStateWithLifecycle()
    val pendingDeleteIntentSender by viewModel.pendingDeleteIntentSender.collectAsStateWithLifecycle()
    val hasStoragePermission by viewModel.hasStoragePermission.collectAsStateWithLifecycle()
    val isScanningStorage by viewModel.isScanningStorage.collectAsStateWithLifecycle()
    val isVaultUnlocked by viewModel.isVaultUnlocked.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()
    val sortAscending by viewModel.sortAscending.collectAsStateWithLifecycle()
    val libraryViewStyle by viewModel.libraryViewStyle.collectAsStateWithLifecycle()
    val libraryCardSize by viewModel.libraryCardSize.collectAsStateWithLifecycle()
    val folderViewStyle by viewModel.folderViewStyle.collectAsStateWithLifecycle()
    val folderCardSize by viewModel.folderCardSize.collectAsStateWithLifecycle()
    val folderSortOption by viewModel.folderSortOption.collectAsStateWithLifecycle()
    val folderSortAscending by viewModel.folderSortAscending.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategoryFilter.collectAsStateWithLifecycle()
    val selectedFolder by viewModel.selectedFolderFilter.collectAsStateWithLifecycle()
    val decoderMode by viewModel.decoderMode.collectAsStateWithLifecycle()
    val bgAudioEnabled by viewModel.backgroundAudioEnabled.collectAsStateWithLifecycle()
    val subtitleConfig by viewModel.subtitleConfig.collectAsStateWithLifecycle()
    val audioConfig by viewModel.audioConfig.collectAsStateWithLifecycle()
    val themeConfig by viewModel.themeConfig.collectAsStateWithLifecycle()
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val bannerMessage by viewModel.statusBannerMessage.collectAsStateWithLifecycle()

    // Apply Landscape Fullscreen rotation & immersive system bars when requested
    LaunchedEffect(playerState.isPlayerOverlayOpen, playerState.isLandscapeFullscreen) {
        val activity = context as? Activity ?: return@LaunchedEffect
        val window = activity.window
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        if (playerState.isPlayerOverlayOpen && playerState.isLandscapeFullscreen) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            insetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    // Keep window brightness identical to device brightness unless the user actively swipes brightness during video playback
    LaunchedEffect(
        playerState.isPlayerOverlayOpen,
        playerState.isBrightnessOverridden,
        playerState.screenBrightness
    ) {
        val activity = context as? Activity ?: return@LaunchedEffect
        val window = activity.window
        val params = window.attributes
        params.screenBrightness = if (playerState.isPlayerOverlayOpen && playerState.isBrightnessOverridden) {
            playerState.screenBrightness.coerceIn(0.01f, 1.0f)
        } else {
            WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        }
        window.attributes = params
    }

    // Synchronize the Theme-Adaptive Custom RemoteViews System Notification when video is playing as Background Audio Mode
    LaunchedEffect(
        playerState.isPlayerOverlayOpen,
        playerState.activeVideo,
        playerState.isPlaying,
        bgAudioEnabled,
        themeConfig,
        audioConfig.volumeBoostPercent
    ) {
        viewModel.refreshSystemNotificationNow()
    }

    // Runtime Storage / Media Permission Launcher (Requested automatically on first launch)
    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        viewModel.onStoragePermissionsResult(results)
    }

    var showDragonIntro by rememberSaveable { mutableStateOf(true) }
    var didPromptInitialStoragePermission by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refreshStoragePermissionState()
        if (showDragonIntro) {
            delay(2000L)
            showDragonIntro = false
        }
        val needsNotificationPerm = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        if ((!StorageScannerHelper.hasStoragePermission(context) || needsNotificationPerm) && !didPromptInitialStoragePermission) {
            didPromptInitialStoragePermission = true
            storagePermissionLauncher.launch(StorageScannerHelper.getRequiredStoragePermissions())
        }
    }

    // Zero-Permission Photo/Video Picker & SAF Folder / Subtitle Pickers
    val pickVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) viewModel.onSingleVideoPicked(uri)
    }

    val safFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        if (treeUri != null) viewModel.onSafFolderPicked(treeUri)
    }

    val subtitleFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { subUri ->
        if (subUri != null) viewModel.importExternalSubtitleUri(subUri)
    }

    // System MediaStore / Scoped Storage delete confirmation permission launcher
    val deletePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        viewModel.clearPendingDeleteIntentSender()
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.showBannerMessage("File permanently deleted from device storage")
        }
    }

    LaunchedEffect(pendingDeleteIntentSender) {
        val intentSender = pendingDeleteIntentSender ?: return@LaunchedEffect
        runCatching {
            deletePermissionLauncher.launch(
                IntentSenderRequest.Builder(intentSender).build()
            )
        }.onFailure {
            viewModel.clearPendingDeleteIntentSender()
        }
    }

    var isHudVisible by remember { mutableStateOf(true) }
    var hudInteractionTick by remember { mutableIntStateOf(0) }
    var isFloatingSubtitleOpen by remember { mutableStateOf(false) }
    var showSubtitleSheet by remember { mutableStateOf(false) }
    var showAudioSheet by remember { mutableStateOf(false) }

    // Auto-hide HUD after 4.5 seconds of inactivity during active video playback
    LaunchedEffect(isHudVisible, playerState.isPlaying, hudInteractionTick, showSubtitleSheet, showAudioSheet, isFloatingSubtitleOpen) {
        if (isHudVisible && playerState.isPlaying && !showSubtitleSheet && !showAudioSheet && !isFloatingSubtitleOpen) {
            delay(4500L)
            isHudVisible = false
        }
    }

    // BackHandler for Fullscreen Player, More Sub-sections, & Secondary Navigation Tabs
    BackHandler(
        enabled = playerState.isPlayerOverlayOpen ||
            (currentTab == DragonNavTab.MORE && moreSubSection != MoreSubSection.HUB) ||
            currentTab != DragonNavTab.LIBRARY
    ) {
        when {
            isFloatingSubtitleOpen -> isFloatingSubtitleOpen = false
            playerState.isPlayerOverlayOpen -> viewModel.closePlayerOverlay()
            currentTab == DragonNavTab.MORE && moreSubSection != MoreSubSection.HUB ->
                viewModel.openMoreSubSection(MoreSubSection.HUB)
            else -> viewModel.selectTab(DragonNavTab.LIBRARY)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (playerState.isPlayerOverlayOpen && playerState.activeVideo != null) {
            val activeVideo = playerState.activeVideo!!
            Box(modifier = Modifier.fillMaxSize()) {
                GestureVideoSurface(
                    exoPlayer = viewModel.exoPlayer,
                    aspectRatioMode = playerState.aspectRatioMode,
                    zoomScale = playerState.zoomScale,
                    panOffsetX = playerState.panOffsetX,
                    panOffsetY = playerState.panOffsetY,
                    isHudLocked = playerState.isHudLocked,
                    screenBrightness = playerState.screenBrightness,
                    isBrightnessOverridden = playerState.isBrightnessOverridden,
                    volumeBoostPercent = audioConfig.volumeBoostPercent,
                    currentPositionMs = playerState.currentPositionMs,
                    durationMs = playerState.durationMs,
                    isLongPress2xActive = playerState.isLongPress2xActive,
                    activeSubtitleCue = playerState.activeSubtitleCue,
                    subtitleStyle = subtitleConfig,
                    isFloatingSubtitleControlOpen = isFloatingSubtitleOpen,
                    onSingleTapToggleHud = {
                        isHudVisible = !isHudVisible
                        hudInteractionTick++
                    },
                    onDoubleTapSeek = { delta ->
                        viewModel.seekRelative(delta)
                        hudInteractionTick++
                    },
                    onLongPressBurstChanged = { active ->
                        viewModel.onLongPressSpeedBurst(active)
                    },
                    onBrightnessChanged = { b -> viewModel.updateScreenBrightness(b) },
                    onVolumeBoostChanged = { boost -> viewModel.setVolumeBoostPercent(boost) },
                    onSeekToPosition = { pos -> viewModel.seekToPosition(pos) },
                    onZoomAndPanChanged = { scale, dx, dy -> viewModel.updateZoomAndPan(scale, dx, dy) },
                    onResetZoom = { viewModel.resetZoomAndPan() },
                    onUpdateSubtitleStyle = { viewModel.updateSubtitleStyle(it) },
                    onCloseFloatingSubtitleControl = { isFloatingSubtitleOpen = false },
                    onOpenFullSubtitleSheet = {
                        isFloatingSubtitleOpen = false
                        showSubtitleSheet = true
                    }
                )

                if (!playerState.isInPipMode) {
                    DragonPlayerHudOverlay(
                        video = activeVideo,
                        isHudVisible = isHudVisible,
                        isHudLocked = playerState.isHudLocked,
                        isLandscapeFullscreen = playerState.isLandscapeFullscreen,
                        isFloatingSubtitleOpen = isFloatingSubtitleOpen,
                        isPlaying = playerState.isPlaying,
                        currentPositionMs = playerState.currentPositionMs,
                        durationMs = playerState.durationMs,
                        bufferedPercentage = playerState.bufferedPercentage,
                        playbackSpeed = playerState.playbackSpeed,
                        loopMode = playerState.loopMode,
                        loopAbStartMs = playerState.loopAbStartMs,
                        loopAbEndMs = playerState.loopAbEndMs,
                        aspectRatioMode = playerState.aspectRatioMode,
                        decoderMode = decoderMode,
                        backgroundAudioEnabled = bgAudioEnabled,
                        volumeBoostPercent = audioConfig.volumeBoostPercent,
                        onClosePlayer = { viewModel.closePlayerOverlay() },
                        onTogglePlayPause = {
                            viewModel.togglePlayPause()
                            hudInteractionTick++
                        },
                        onSeekTo = { pos ->
                            viewModel.seekToPosition(pos)
                            hudInteractionTick++
                        },
                        onSeekRelative = { delta ->
                            viewModel.seekRelative(delta)
                            hudInteractionTick++
                        },
                        onStepFrame = { forward ->
                            viewModel.stepSingleFrame(forward)
                            hudInteractionTick++
                        },
                        onPlayPrevious = { viewModel.playPreviousInQueue() },
                        onPlayNext = { viewModel.playNextInQueue() },
                        onSelectLoopMode = { mode ->
                            viewModel.setLoopMode(mode)
                            hudInteractionTick++
                        },
                        onCycleLoopMode = {
                            viewModel.cycleLoopMode()
                            hudInteractionTick++
                        },
                        onSetAbLoopPointA = {
                            viewModel.setAbLoopPointA()
                            hudInteractionTick++
                        },
                        onSetAbLoopPointB = {
                            viewModel.setAbLoopPointB()
                            hudInteractionTick++
                        },
                        onClearAbLoopPoints = {
                            viewModel.clearAbLoopPoints()
                            hudInteractionTick++
                        },
                        onCycleAspectRatio = {
                            viewModel.cycleAspectRatio()
                            hudInteractionTick++
                        },
                        onToggleLandscapeFullscreen = {
                            viewModel.toggleLandscapeFullscreen()
                            hudInteractionTick++
                        },
                        onToggleFloatingSubtitle = {
                            isFloatingSubtitleOpen = !isFloatingSubtitleOpen
                            hudInteractionTick++
                        },
                        onSelectSpeed = { spd ->
                            viewModel.setPlaybackSpeed(spd)
                            hudInteractionTick++
                        },
                        onCycleDecoderMode = {
                            val modes = DecoderMode.entries
                            val next = modes[(modes.indexOf(decoderMode) + 1) % modes.size]
                            viewModel.setDecoderMode(next)
                            hudInteractionTick++
                        },
                        onToggleBackgroundAudio = {
                            viewModel.setBackgroundAudio(!bgAudioEnabled)
                            hudInteractionTick++
                        },
                        onEnterPipMode = onRequestPipMode,
                        onCaptureScreenshot = {
                            viewModel.captureCurrentFrameToGallery()
                            hudInteractionTick++
                        },
                        onOpenSubtitleStudio = { showSubtitleSheet = true },
                        onOpenAudioStudio = { showAudioSheet = true },
                        onDeleteVideoToTrash = { viewModel.moveVideoToTrash(activeVideo) },
                        onToggleHudLock = {
                            viewModel.toggleHudLock()
                            isHudVisible = true
                            hudInteractionTick++
                        }
                    )
                }
            }

            if (showSubtitleSheet) {
                SubtitleCustomizerSheet(
                    config = subtitleConfig,
                    onUpdateConfig = { viewModel.updateSubtitleStyle(it) },
                    onLoadExternalSubtitleFile = {
                        subtitleFileLauncher.launch(arrayOf("*/*"))
                    },
                    onDismiss = { showSubtitleSheet = false }
                )
            }

            if (showAudioSheet) {
                AudioAndEqualizerSheet(
                    config = audioConfig,
                    videoCodec = activeVideo.videoCodec,
                    audioCodec = activeVideo.audioCodec,
                    onUpdateConfig = { viewModel.updateAudioEngine(it) },
                    onDismiss = { showAudioSheet = false }
                )
            }
        } else {
            // Main App Scaffold with 4 Primary Navigation Tabs: Library, Folders, Playlists, More
            Scaffold(
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                containerColor = MaterialTheme.colorScheme.background,
                topBar = {
                    DragonTopAppBar(
                        activePresetTitle = themeConfig.preset.title,
                        decoderBadge = decoderMode.badge,
                        onOpenThemeStudio = { viewModel.openMoreSubSection(MoreSubSection.THEME_STUDIO) },
                        onOpenVault = { viewModel.openMoreSubSection(MoreSubSection.VAULT) }
                    )
                },
                bottomBar = {
                    Column {
                        // Compact Theme-Adaptive Notification Control Panel shown whenever a video is active
                        val bgVideo = playerState.activeVideo
                        if (!playerState.isPlayerOverlayOpen && bgVideo != null) {
                            DragonBackgroundAudioNotificationPanel(
                                video = bgVideo,
                                isPlaying = playerState.isPlaying,
                                currentPositionMs = playerState.currentPositionMs,
                                durationMs = playerState.durationMs,
                                volumeBoostPercent = audioConfig.volumeBoostPercent,
                                onExpandToVideoPlayer = viewModel::resumePlayerOverlayFromBackground,
                                onTogglePlayPause = viewModel::togglePlayPause,
                                onSeekTo = viewModel::seekToPosition,
                                onSeekRelative = viewModel::seekRelative,
                                onPlayPrevious = { viewModel.playPreviousInQueue(keepInBackgroundIfClosed = true) },
                                onPlayNext = { viewModel.playNextInQueue(keepInBackgroundIfClosed = true) },
                                onCycleNotifStyle = viewModel::cycleNotificationPanelStyle,
                                onStopBackgroundSession = viewModel::stopBackgroundAudioSession
                            )
                        }

                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                            tonalElevation = 8.dp
                        ) {
                            val navItems = listOf(
                                Triple(DragonNavTab.LIBRARY, "Library", Icons.Default.VideoLibrary),
                                Triple(DragonNavTab.FOLDERS, "Folders", Icons.Default.Folder),
                                Triple(DragonNavTab.PLAYLISTS, "Playlists", Icons.AutoMirrored.Filled.QueueMusic),
                                Triple(DragonNavTab.MORE, "More", Icons.Default.MoreHoriz)
                            )
                            navItems.forEach { (tab, label, icon) ->
                                NavigationBarItem(
                                    selected = currentTab == tab,
                                    onClick = { viewModel.selectTab(tab) },
                                    icon = { Icon(icon, contentDescription = label) },
                                    label = {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color.Black,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (currentTab) {
                        DragonNavTab.LIBRARY -> LibraryScreenContent(
                            videos = videos,
                            playlists = playlists,
                            searchQuery = searchQuery,
                            sortOption = sortOption,
                            sortAscending = sortAscending,
                            viewStyle = libraryViewStyle,
                            cardSize = libraryCardSize,
                            selectedCategory = selectedCategory,
                            selectedFolderFilter = selectedFolder,
                            decoderMode = decoderMode,
                            hasStoragePermission = hasStoragePermission,
                            isScanningStorage = isScanningStorage,
                            onSearchQueryChange = viewModel::setSearchQuery,
                            onSortOptionSelect = viewModel::setSortOption,
                            onViewStyleSelect = viewModel::setLibraryViewStyle,
                            onCardSizeSelect = viewModel::setLibraryCardSize,
                            onCategorySelect = viewModel::setCategoryFilter,
                            onClearFolderFilter = { viewModel.setFolderFilter(null) },
                            onPlayVideo = { video -> viewModel.playVideo(video, videos) },
                            onToggleFavorite = viewModel::toggleFavorite,
                            onMoveToVault = viewModel::toggleVaultForVideo,
                            onDeleteToTrash = viewModel::moveVideoToTrash,
                            onToggleVideoInPlaylist = viewModel::toggleVideoInPlaylist,
                            onRequestStoragePermission = {
                                storagePermissionLauncher.launch(
                                    StorageScannerHelper.getRequiredStoragePermissions()
                                )
                            },
                            onScanStorage = {
                                if (StorageScannerHelper.hasStoragePermission(context)) {
                                    viewModel.scanDeviceStorage()
                                } else {
                                    storagePermissionLauncher.launch(
                                        StorageScannerHelper.getRequiredStoragePermissions()
                                    )
                                }
                            },
                            onPickSafFolder = { safFolderLauncher.launch(null) },
                            onPickSingleVideo = {
                                pickVideoLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                )
                            },
                            onOpenStreamSection = {
                                viewModel.openMoreSubSection(MoreSubSection.NETWORK_STREAM)
                            }
                        )

                        DragonNavTab.FOLDERS -> FoldersScreenContent(
                            folders = folders,
                            hasStoragePermission = hasStoragePermission,
                            folderViewStyle = folderViewStyle,
                            folderCardSize = folderCardSize,
                            folderSortOption = folderSortOption,
                            folderSortAscending = folderSortAscending,
                            onSelectFolder = { folderName -> viewModel.setFolderFilter(folderName) },
                            onSelectFolderViewStyle = viewModel::setFolderViewStyle,
                            onSelectFolderCardSize = viewModel::setFolderCardSize,
                            onSelectFolderSortOption = viewModel::setFolderSortOption,
                            onRequestStoragePermission = {
                                storagePermissionLauncher.launch(
                                    StorageScannerHelper.getRequiredStoragePermissions()
                                )
                            },
                            onScanStorage = {
                                if (StorageScannerHelper.hasStoragePermission(context)) {
                                    viewModel.scanDeviceStorage()
                                } else {
                                    storagePermissionLauncher.launch(
                                        StorageScannerHelper.getRequiredStoragePermissions()
                                    )
                                }
                            },
                            onMountSafFolder = { safFolderLauncher.launch(null) }
                        )

                        DragonNavTab.PLAYLISTS -> PlaylistsScreenContent(
                            playlists = playlists,
                            allVideos = videos,
                            onCreatePlaylist = { name, desc -> viewModel.createPlaylist(name, desc) },
                            onDeletePlaylist = viewModel::deletePlaylist,
                            onPlayVideoQueue = { video, queue -> viewModel.playVideo(video, queue) }
                        )

                        DragonNavTab.MORE -> {
                            when (moreSubSection) {
                                MoreSubSection.HUB -> MoreHubScreenContent(
                                    activeThemeTitle = themeConfig.preset.title,
                                    decoderMode = decoderMode,
                                    vaultVideoCount = vaultVideos.size,
                                    trashVideoCount = trashedVideos.size,
                                    backgroundAudioEnabled = bgAudioEnabled,
                                    onOpenSubSection = viewModel::openMoreSubSection,
                                    onSelectDecoderMode = viewModel::setDecoderMode,
                                    onToggleBackgroundAudio = viewModel::setBackgroundAudio
                                )

                                MoreSubSection.NETWORK_STREAM -> NetworkStreamScreenContent(
                                    savedStreams = savedNetworkStreams,
                                    streamBookmarks = streamBookmarks,
                                    onBackToMore = { viewModel.openMoreSubSection(MoreSubSection.HUB) },
                                    onLaunchNetworkStream = viewModel::addAndPlayNetworkStream,
                                    onSaveStreamBookmark = viewModel::saveStreamBookmark,
                                    onPlayBookmarkedStream = viewModel::playBookmarkedStream,
                                    onRemoveStreamBookmark = viewModel::removeStreamBookmark,
                                    onPlaySavedStream = { streamVideo -> viewModel.playVideo(streamVideo) },
                                    onResetRecentStreams = viewModel::resetRecentNetworkStreams,
                                    onRemoveStream = viewModel::removeSingleNetworkStream
                                )

                                MoreSubSection.TRASH_BIN -> TrashBinScreenContent(
                                    trashedVideos = trashedVideos,
                                    onBackToMore = { viewModel.openMoreSubSection(MoreSubSection.HUB) },
                                    onRestoreVideo = viewModel::restoreVideoFromTrash,
                                    onDeleteForever = viewModel::deleteVideoForever,
                                    onRestoreAll = viewModel::restoreAllFromTrash,
                                    onEmptyTrashForever = viewModel::emptyTrashBinForever
                                )

                                MoreSubSection.VAULT -> PrivateVaultScreenContent(
                                    isUnlocked = isVaultUnlocked,
                                    vaultVideos = vaultVideos,
                                    onBackToMore = { viewModel.openMoreSubSection(MoreSubSection.HUB) },
                                    onAttemptUnlock = viewModel::attemptUnlockVault,
                                    onLockVault = viewModel::lockVault,
                                    onUpdatePin = viewModel::updateVaultPin,
                                    onPlayVaultVideo = { video -> viewModel.playVideo(video, vaultVideos) },
                                    onRestoreToLibrary = viewModel::toggleVaultForVideo,
                                    onDeleteVaultVideoToTrash = viewModel::moveVideoToTrash
                                )

                                MoreSubSection.THEME_STUDIO -> ThemeCustomizerScreenContent(
                                    themeConfig = themeConfig,
                                    onBackToMore = { viewModel.openMoreSubSection(MoreSubSection.HUB) },
                                    onSelectPreset = viewModel::setThemePreset,
                                    onUpdateTheme = viewModel::updateTheme
                                )

                                MoreSubSection.CODEC_ENGINE -> CodecEngineScreenContent(
                                    decoderMode = decoderMode,
                                    codecSpecs = viewModel.codecSpecs,
                                    onBackToMore = { viewModel.openMoreSubSection(MoreSubSection.HUB) },
                                    onSelectDecoderMode = viewModel::setDecoderMode
                                )
                            }
                        }
                    }
                }
            }
        }

        // Global Floating Glassmorphic Status Toast / Notification Banner
        AnimatedVisibility(
            visible = bannerMessage != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = 68.dp, start = 20.dp, end = 20.dp)
        ) {
            bannerMessage?.let { msg ->
                Surface(
                    onClick = { viewModel.dismissBannerMessage() },
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Transparent,
                    modifier = Modifier.dragonGlassSurface(elevated = true, accentGlow = true)
                ) {
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                    )
                }
            }
        }

        // 2-Second Red Dragon Splash Animation Overlay on App Launch
        AnimatedVisibility(
            visible = showDragonIntro,
            enter = fadeIn(animationSpec = tween(250)),
            exit = fadeOut(animationSpec = tween(420)),
            modifier = Modifier.fillMaxSize()
        ) {
            DragonIntroAnimationOverlay()
        }
    }
}

@Composable
private fun DragonTopAppBar(
    activePresetTitle: String,
    decoderBadge: String,
    onOpenThemeStudio: () -> Unit,
    onOpenVault: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(scheme.surface.copy(alpha = 0.90f))
            .dragonScaleMesh(scaleAlpha = 0.06f)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(scheme.primary, scheme.secondary)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column {
                Text(
                    text = "DRAGON VIDEO PLAYER",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "$activePresetTitle • Engine $decoderBadge",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.primary
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Surface(
                onClick = onOpenThemeStudio,
                shape = RoundedCornerShape(12.dp),
                color = scheme.primary.copy(alpha = 0.16f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "Theme Customizer",
                        tint = scheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text("Theme", style = MaterialTheme.typography.labelMedium, color = Color.White)
                }
            }
            IconButton(
                onClick = onOpenVault,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Private Vault",
                    tint = scheme.tertiary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
