package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import android.provider.Settings
import androidx.annotation.OptIn
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.ts.DefaultTsPayloadReaderFactory
import com.example.player.DragonStreamPlaylistHelper
import com.example.player.DragonStreamPlaylistHelper.toVideoEntity
import com.example.data.local.DragonDatabase
import com.example.data.local.PlaylistEntity
import com.example.data.local.StreamBookmarkEntity
import com.example.data.local.VideoEntity
import com.example.data.repository.DragonRepository
import com.example.domain.model.AspectRatioMode
import com.example.domain.model.AudioEngineConfig
import com.example.domain.model.CodecCapabilitySpec
import com.example.domain.model.DecoderMode
import com.example.domain.model.FolderSortOption
import com.example.domain.model.MediaCardSize
import com.example.domain.model.MediaViewStyle
import com.example.domain.model.PlayerLoopMode
import com.example.domain.model.SubtitleCueItem
import com.example.domain.model.SubtitleStyleConfig
import com.example.domain.model.VideoFolderSummary
import com.example.domain.model.VideoSortOption
import com.example.player.DragonAudioProcessorManager
import com.example.player.DragonPlaybackService
import com.example.player.StorageScannerHelper
import com.example.player.SubtitleParserEngine
import com.example.ui.player.DragonSystemAudioNotificationHelper
import com.example.ui.theme.DragonNotifPanelStyle
import com.example.ui.theme.DragonThemeConfig
import com.example.ui.theme.DragonThemePreset
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class DragonNavTab(val label: String) {
    LIBRARY("Library"),
    FOLDERS("Folders"),
    PLAYLISTS("Playlists"),
    MORE("More")
}

enum class MoreSubSection(val title: String) {
    HUB("More"),
    NETWORK_STREAM("Network Stream"),
    TRASH_BIN("Trash Bin"),
    VAULT("Dragon Vault"),
    THEME_STUDIO("Theme Studio"),
    CODEC_ENGINE("Codec & Decoder Engine")
}

data class PlayerRuntimeState(
    val activeVideo: VideoEntity? = null,
    val isPlayerOverlayOpen: Boolean = false,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 1L,
    val bufferedPercentage: Int = 0,
    val playbackSpeed: Float = 1.0f,
    val isLongPress2xActive: Boolean = false,
    val speedBeforeLongPress: Float = 1.0f,
    val loopMode: PlayerLoopMode = PlayerLoopMode.OFF,
    val loopAbStartMs: Long = 0L,
    val loopAbEndMs: Long? = null,
    val aspectRatioMode: AspectRatioMode = AspectRatioMode.FIT,
    val zoomScale: Float = 1.0f,
    val panOffsetX: Float = 0f,
    val panOffsetY: Float = 0f,
    val isHudLocked: Boolean = false,
    val isLandscapeFullscreen: Boolean = false,
    val screenBrightness: Float = 0.50f,
    val isBrightnessOverridden: Boolean = false,
    val activeSubtitleCue: String? = null,
    val isInPipMode: Boolean = false,
    val activePlaylistQueue: List<VideoEntity> = emptyList(),
    val streamPlaylist: List<VideoEntity> = emptyList(),
    val playerErrorText: String? = null
)

fun readDeviceBrightnessFraction(context: Context): Float {
    return runCatching {
        val raw = Settings.System.getInt(
            context.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS
        )
        (raw / 255f).coerceIn(0.01f, 1.0f)
    }.getOrDefault(0.50f)
}

@OptIn(UnstableApi::class)
class DragonPlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val database = DragonDatabase.getInstance(application)
    private val repository = DragonRepository(application, database.dragonDao())
    private val audioProcessor = DragonAudioProcessorManager()

    val exoPlayer: ExoPlayer = buildDragonExoPlayer(application)

    private val _currentTab = MutableStateFlow(DragonNavTab.LIBRARY)
    val currentTab: StateFlow<DragonNavTab> = _currentTab.asStateFlow()

    private val _moreSubSection = MutableStateFlow(MoreSubSection.HUB)
    val moreSubSection: StateFlow<MoreSubSection> = _moreSubSection.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOption = MutableStateFlow(repository.getInitialVideoSortOption())
    val sortOption: StateFlow<VideoSortOption> = _sortOption.asStateFlow()

    private val _sortAscending = MutableStateFlow(repository.getInitialVideoSortAscending())
    val sortAscending: StateFlow<Boolean> = _sortAscending.asStateFlow()

    private val _libraryViewStyle = MutableStateFlow(repository.getInitialLibraryViewStyle())
    val libraryViewStyle: StateFlow<MediaViewStyle> = _libraryViewStyle.asStateFlow()

    private val _libraryCardSize = MutableStateFlow(repository.getInitialLibraryCardSize())
    val libraryCardSize: StateFlow<MediaCardSize> = _libraryCardSize.asStateFlow()

    private val _folderViewStyle = MutableStateFlow(repository.getInitialFolderViewStyle())
    val folderViewStyle: StateFlow<MediaViewStyle> = _folderViewStyle.asStateFlow()

    private val _folderCardSize = MutableStateFlow(repository.getInitialFolderCardSize())
    val folderCardSize: StateFlow<MediaCardSize> = _folderCardSize.asStateFlow()

    private val _folderSortOption = MutableStateFlow(repository.getInitialFolderSortOption())
    val folderSortOption: StateFlow<FolderSortOption> = _folderSortOption.asStateFlow()

    private val _folderSortAscending = MutableStateFlow(repository.getInitialFolderSortAscending())
    val folderSortAscending: StateFlow<Boolean> = _folderSortAscending.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow("All")
    val selectedCategoryFilter: StateFlow<String> = _selectedCategoryFilter.asStateFlow()

    private val _selectedFolderFilter = MutableStateFlow<String?>(null)
    val selectedFolderFilter: StateFlow<String?> = _selectedFolderFilter.asStateFlow()

    private val _isVaultUnlocked = MutableStateFlow(false)
    val isVaultUnlocked: StateFlow<Boolean> = _isVaultUnlocked.asStateFlow()

    private val _hasStoragePermission = MutableStateFlow(
        StorageScannerHelper.hasStoragePermission(application)
    )
    val hasStoragePermission: StateFlow<Boolean> = _hasStoragePermission.asStateFlow()

    private val _isScanningStorage = MutableStateFlow(false)
    val isScanningStorage: StateFlow<Boolean> = _isScanningStorage.asStateFlow()

    private val _statusBannerMessage = MutableStateFlow<String?>(null)
    val statusBannerMessage: StateFlow<String?> = _statusBannerMessage.asStateFlow()

    private val _playerState = MutableStateFlow(
        PlayerRuntimeState(
            screenBrightness = readDeviceBrightnessFraction(application),
            isBrightnessOverridden = false
        )
    )
    val playerState: StateFlow<PlayerRuntimeState> = _playerState.asStateFlow()

    private var loadedExternalCues: List<SubtitleCueItem> = emptyList()
    private var notifAnimPhase: Float = 0f
    private var notifTickCounter: Int = 0

    val themeConfig: StateFlow<DragonThemeConfig> = repository.themeConfigFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        DragonThemeConfig()
    )

    val decoderMode: StateFlow<DecoderMode> = repository.decoderModeFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        DecoderMode.AUTO_HW_PLUS
    )

    val backgroundAudioEnabled: StateFlow<Boolean> = repository.backgroundAudioEnabledFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        true
    )

    val vaultPin: StateFlow<String> = repository.vaultPinFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        "1234"
    )

    val subtitleConfig: StateFlow<SubtitleStyleConfig> = repository.subtitleConfigFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        SubtitleStyleConfig()
    )

    val audioConfig: StateFlow<AudioEngineConfig> = repository.audioConfigFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        AudioEngineConfig()
    )

    private val _pendingDeleteIntentSender = MutableStateFlow<IntentSender?>(null)
    val pendingDeleteIntentSender: StateFlow<IntentSender?> = _pendingDeleteIntentSender.asStateFlow()

    val playlists: StateFlow<List<PlaylistEntity>> = repository.playlistsFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val vaultVideos: StateFlow<List<VideoEntity>> = repository.vaultVideosFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val trashedVideos: StateFlow<List<VideoEntity>> = repository.trashedVideosFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val savedNetworkStreams: StateFlow<List<VideoEntity>> = repository.networkStreamsFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val streamBookmarks: StateFlow<List<StreamBookmarkEntity>> = repository.streamBookmarksFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val filteredVideos: StateFlow<List<VideoEntity>> = combine(
        repository.publicVideosFlow,
        _searchQuery,
        _sortOption,
        _sortAscending,
        combine(_selectedCategoryFilter, _selectedFolderFilter) { cat, folder -> cat to folder }
    ) { videos, query, sort, asc, (category, folder) ->
        val q = query.trim().lowercase()
        val filtered = videos.filter { video ->
            val matchesQuery = q.isEmpty() ||
                video.title.lowercase().contains(q) ||
                video.folderName.lowercase().contains(q) ||
                video.videoCodec.lowercase().contains(q) ||
                video.containerFormat.lowercase().contains(q)

            val matchesCategory = when (category) {
                "4K UHD" -> video.width >= 3840 || video.height >= 2160
                "MKV / HEVC" -> video.containerFormat.contains("MKV", true) || video.videoCodec.contains("HEVC", true)
                "AV1 / VP9" -> video.videoCodec.contains("AV1", true) || video.videoCodec.contains("VP9", true)
                "Streams" -> video.isNetworkStream
                "Favorites" -> video.isFavorite
                else -> true
            }

            val matchesFolder = folder == null || video.folderName.equals(folder, ignoreCase = true)
            matchesQuery && matchesCategory && matchesFolder
        }

        val sorted = when (sort) {
            VideoSortOption.NAME -> filtered.sortedBy { it.title.lowercase() }
            VideoSortOption.DATE -> filtered.sortedBy { it.dateAddedEpochSec }
            VideoSortOption.SIZE -> filtered.sortedBy { it.sizeBytes }
            VideoSortOption.DURATION -> filtered.sortedBy { it.durationMs }
        }
        if (asc) sorted else sorted.reversed()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val folderSummaries: StateFlow<List<VideoFolderSummary>> = combine(
        repository.publicVideosFlow,
        _searchQuery,
        _folderSortOption,
        _folderSortAscending
    ) { videos, query, sortOpt, asc ->
        val q = query.trim().lowercase()
        val summaries = videos.groupBy { it.folderName }
            .map { (folderName, items) ->
                VideoFolderSummary(
                    folderName = folderName,
                    folderPath = "/storage/emulated/0/$folderName",
                    videoCount = items.size,
                    totalSizeBytes = items.sumOf { it.sizeBytes },
                    totalDurationMs = items.sumOf { it.durationMs },
                    previewVideos = items.take(3).map { it.title },
                    leadVideo = items.firstOrNull()
                )
            }
            .filter { q.isEmpty() || it.folderName.lowercase().contains(q) }

        val sorted = when (sortOpt) {
            FolderSortOption.VIDEO_COUNT -> summaries.sortedBy { it.videoCount }
            FolderSortOption.NAME -> summaries.sortedBy { it.folderName.lowercase() }
            FolderSortOption.TOTAL_SIZE -> summaries.sortedBy { it.totalSizeBytes }
            FolderSortOption.TOTAL_DURATION -> summaries.sortedBy { it.totalDurationMs }
        }
        if (asc) sorted else sorted.reversed()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val codecSpecs: List<CodecCapabilitySpec> = repository.getCodecCapabilitySpecs()

    init {
        // Silently scan real device storage on startup if permission is already granted
        viewModelScope.launch {
            val granted = StorageScannerHelper.hasStoragePermission(getApplication())
            _hasStoragePermission.value = granted
            if (granted) {
                _isScanningStorage.value = true
                val discovered = StorageScannerHelper.scanMediaStoreVideos(getApplication())
                discovered.forEach { repository.insertOrUpdateVideo(it) }
                _isScanningStorage.value = false
            }
        }

        // Register background BroadcastReceiver listener so clicking buttons in the
        // Android System Notification Panel executes actions directly WITHOUT opening the app.
        DragonSystemAudioNotificationHelper.registerActionListener(
            object : DragonSystemAudioNotificationHelper.BackgroundActionListener {
                override fun onTogglePlayPause() {
                    togglePlayPause()
                    refreshSystemNotificationNow()
                }

                override fun onSeekRelative(deltaMs: Long) {
                    seekRelative(deltaMs)
                    refreshSystemNotificationNow()
                }

                override fun onSeekFraction(fraction: Float) {
                    val dur = _playerState.value.durationMs.coerceAtLeast(1L)
                    seekToPosition((dur * fraction.coerceIn(0f, 1f)).toLong())
                    refreshSystemNotificationNow()
                }

                override fun onPlayPrevious() {
                    playPreviousInQueue(keepInBackgroundIfClosed = true)
                    refreshSystemNotificationNow()
                }

                override fun onPlayNext() {
                    playNextInQueue(keepInBackgroundIfClosed = true)
                    refreshSystemNotificationNow()
                }

                override fun onCycleStyle() {
                    val styles = DragonNotifPanelStyle.entries
                    val current = themeConfig.value.notifPanelStyle
                    val next = styles[(styles.indexOf(current) + 1) % styles.size]
                    updateTheme { it.copy(notifPanelStyle = next) }
                    viewModelScope.launch {
                        delay(60L)
                        refreshSystemNotificationNow()
                    }
                }

                override fun onStopBackgroundAudio() {
                    stopBackgroundAudioSession()
                }
            }
        )

        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playerState.update { it.copy(isPlaying = isPlaying) }
                refreshSystemNotificationNow()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                val dur = exoPlayer.duration.takeIf { it > 0 } ?: _playerState.value.activeVideo?.durationMs ?: 1L
                _playerState.update {
                    it.copy(
                        isBuffering = playbackState == Player.STATE_BUFFERING,
                        durationMs = dur
                    )
                }
                if (playbackState == Player.STATE_READY) {
                    audioProcessor.attachToPlayer(exoPlayer, audioConfig.value)
                } else if (playbackState == Player.STATE_ENDED) {
                    handlePlaybackEndedForLoopMode()
                }
                refreshSystemNotificationNow()
            }

            override fun onPlayerError(error: PlaybackException) {
                if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                    exoPlayer.seekToDefaultPosition()
                    exoPlayer.prepare()
                } else {
                    _playerState.update {
                        it.copy(
                            playerErrorText = "Playback notice (${error.errorCodeName}): ${error.message ?: "Reconnecting"}"
                        )
                    }
                }
            }
        })

        // Continuous position, A-B loop check, subtitle cue & live notification ticker (every 120ms)
        // Active whenever a video is loaded in foreground or in background
        viewModelScope.launch {
            while (isActive) {
                val state = _playerState.value
                val activeVideo = state.activeVideo
                if (activeVideo != null) {
                    var pos = exoPlayer.currentPosition.coerceAtLeast(0L)
                    val dur = exoPlayer.duration.takeIf { it > 0L } ?: activeVideo.durationMs.coerceAtLeast(1L)

                    // Enforce A-B Segment Loop if active
                    if (state.loopMode == PlayerLoopMode.AB_REPEAT) {
                        val abStart = state.loopAbStartMs.coerceIn(0L, (dur - 500L).coerceAtLeast(0L))
                        val abEnd = state.loopAbEndMs
                        if (abEnd != null && abEnd > abStart + 300L && pos >= abEnd) {
                            exoPlayer.seekTo(abStart)
                            pos = abStart
                        }
                    }

                    val buffered = exoPlayer.bufferedPercentage.coerceIn(0, 100)
                    val subCfg = subtitleConfig.value
                    val cue = if (state.isPlayerOverlayOpen && subCfg.enabled) {
                        SubtitleParserEngine.resolveCueForPosition(
                            positionMs = pos,
                            syncOffsetMs = subCfg.syncOffsetMs,
                            trackId = subCfg.selectedTrackId,
                            videoTitle = activeVideo.title,
                            externalCues = loadedExternalCues
                        )
                    } else null

                    _playerState.update {
                        it.copy(
                            currentPositionMs = pos,
                            durationMs = dur,
                            bufferedPercentage = buffered,
                            activeSubtitleCue = cue
                        )
                    }

                    // Periodically save resume position
                    if (exoPlayer.isPlaying && pos > 1000L && (pos / 1000L) % 4L == 0L) {
                        repository.updateResumePosition(activeVideo.id, pos)
                    }
                }
                delay(120L)
            }
        }

        // Fluid, stutter-free Animation Loop for the Notification Panel at the device's default max refresh rate
        // Runs whenever a video is playing in EITHER foreground or background!
        viewModelScope.launch {
            var lastTimeNs = System.nanoTime()
            val twoPi = (2.0 * Math.PI).toFloat()
            // Dynamically query the device's maximum hardware refresh rate (e.g., 60Hz, 90Hz, 120Hz, 144Hz)
            val maxRefreshHz = DragonSystemAudioNotificationHelper.getDeviceMaxRefreshRateHz(getApplication())
            // Calculate frame delay matching max refresh rate (e.g. 120Hz -> ~8ms, 144Hz -> ~6ms, 90Hz -> ~11ms, 60Hz -> ~16ms)
            val frameDelayMs = (1000f / maxRefreshHz).toLong().coerceIn(4L, 20L)

            while (isActive) {
                val state = _playerState.value
                val isVideoPlaying = state.activeVideo != null && exoPlayer.isPlaying

                val nowNs = System.nanoTime()
                val dtSec = ((nowNs - lastTimeNs) / 1_000_000_000f).coerceIn(0.002f, 0.05f)
                lastTimeNs = nowNs

                if (isVideoPlaying) {
                    notifAnimPhase = (notifAnimPhase + dtSec * 3.4f) % twoPi
                    refreshSystemNotificationNow()
                    delay(frameDelayMs)
                } else {
                    delay(120L)
                }
            }
        }
    }

    /**
     * Shows or updates the custom Notification Panel every time a video is active
     * in either the foreground player or background playback mode.
     */
    fun refreshSystemNotificationNow() {
        val state = _playerState.value
        val activeVideo = state.activeVideo
        if (activeVideo != null) {
            DragonSystemAudioNotificationHelper.showOrUpdateBackgroundNotification(
                context = getApplication(),
                video = activeVideo,
                isPlaying = exoPlayer.isPlaying,
                currentPositionMs = exoPlayer.currentPosition.coerceAtLeast(state.currentPositionMs),
                durationMs = (exoPlayer.duration.takeIf { it > 0L } ?: state.durationMs).coerceAtLeast(1L),
                volumeBoostPercent = audioConfig.value.volumeBoostPercent,
                themeConfig = themeConfig.value,
                animPhase = notifAnimPhase
            )
        } else {
            DragonSystemAudioNotificationHelper.cancelBackgroundNotification(getApplication())
        }
    }

    private fun buildDragonExoPlayer(app: Application): ExoPlayer {
        val extractorsFactory = DefaultExtractorsFactory()
            .setTsExtractorFlags(DefaultTsPayloadReaderFactory.FLAG_ALLOW_NON_IDR_KEYFRAMES)
            .setConstantBitrateSeekingEnabled(true)

        val renderersFactory = DefaultRenderersFactory(app)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
            .setEnableDecoderFallback(true)

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(15_000, 50_000, 1_500, 3_000)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        // Robust HTTP DataSource with realistic User-Agent, redirects, and custom headers
        // so online live streams and IPTV servers don't return 403 Forbidden or redirect errors!
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(DragonStreamPlaylistHelper.STREAM_USER_AGENT)
            .setConnectTimeoutMs(18_000)
            .setReadTimeoutMs(25_000)
            .setAllowCrossProtocolRedirects(true)
            .setKeepPostFor302Redirects(true)
            .setDefaultRequestProperties(
                mapOf(
                    "Accept" to "*/*",
                    "Accept-Encoding" to "gzip, deflate",
                    "Connection" to "keep-alive"
                )
            )

        val upstreamDataSourceFactory = DefaultDataSource.Factory(app, httpDataSourceFactory)
        val mediaSourceFactory = DefaultMediaSourceFactory(upstreamDataSourceFactory, extractorsFactory)

        return ExoPlayer.Builder(app, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
    }

    fun selectTab(tab: DragonNavTab) {
        _currentTab.value = tab
        if (tab != DragonNavTab.MORE) {
            _moreSubSection.value = MoreSubSection.HUB
        }
    }

    fun openMoreSubSection(subSection: MoreSubSection) {
        _currentTab.value = DragonNavTab.MORE
        _moreSubSection.value = subSection
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortOption(option: VideoSortOption) {
        val newAsc = if (_sortOption.value == option) {
            !_sortAscending.value
        } else {
            option == VideoSortOption.NAME
        }
        _sortOption.value = option
        _sortAscending.value = newAsc
        viewModelScope.launch {
            repository.saveVideoSort(option, newAsc)
        }
    }

    fun setLibraryViewStyle(style: MediaViewStyle) {
        _libraryViewStyle.value = style
        viewModelScope.launch {
            repository.saveLibraryViewStyle(style)
        }
    }

    fun setLibraryCardSize(size: MediaCardSize) {
        _libraryCardSize.value = size
        viewModelScope.launch {
            repository.saveLibraryCardSize(size)
        }
    }

    fun setFolderViewStyle(style: MediaViewStyle) {
        _folderViewStyle.value = style
        viewModelScope.launch {
            repository.saveFolderViewStyle(style)
        }
    }

    fun setFolderCardSize(size: MediaCardSize) {
        _folderCardSize.value = size
        viewModelScope.launch {
            repository.saveFolderCardSize(size)
        }
    }

    fun setFolderSortOption(option: FolderSortOption) {
        val newAsc = if (_folderSortOption.value == option) {
            !_folderSortAscending.value
        } else {
            option == FolderSortOption.NAME
        }
        _folderSortOption.value = option
        _folderSortAscending.value = newAsc
        viewModelScope.launch {
            repository.saveFolderSort(option, newAsc)
        }
    }

    fun setCategoryFilter(category: String) {
        _selectedCategoryFilter.value = category
    }

    fun setFolderFilter(folderName: String?) {
        _selectedFolderFilter.value = folderName
        if (folderName != null) {
            _currentTab.value = DragonNavTab.LIBRARY
        }
    }

    fun showBannerMessage(message: String) {
        _statusBannerMessage.value = message
        viewModelScope.launch {
            delay(3600L)
            if (_statusBannerMessage.value == message) {
                _statusBannerMessage.value = null
            }
        }
    }

    fun dismissBannerMessage() {
        _statusBannerMessage.value = null
    }

    /**
     * Reopens the fullscreen video player overlay when a video is already active in background audio mode,
     * continuing playback seamlessly from the exact current position without restarting.
     */
    fun resumePlayerOverlayFromBackground() {
        val currentActive = _playerState.value.activeVideo ?: return
        val livePos = exoPlayer.currentPosition.coerceAtLeast(_playerState.value.currentPositionMs)
        val deviceBrightness = readDeviceBrightnessFraction(getApplication())
        _playerState.update {
            it.copy(
                activeVideo = currentActive.copy(lastPositionMs = livePos),
                isPlayerOverlayOpen = true,
                currentPositionMs = livePos,
                screenBrightness = if (it.isBrightnessOverridden) it.screenBrightness else deviceBrightness
            )
        }
        if (!exoPlayer.isPlaying && exoPlayer.playbackState != Player.STATE_ENDED) {
            exoPlayer.play()
        }
    }

    fun playVideo(video: VideoEntity, queue: List<VideoEntity> = emptyList(), startFromBeginning: Boolean = false) {
        val currentActive = _playerState.value.activeVideo
        // If this exact video is already loaded in ExoPlayer (e.g. playing in background audio mode),
        // simply reopen the player overlay and continue seamlessly without resetting MediaItem!
        if (!startFromBeginning && currentActive != null && currentActive.id == video.id && exoPlayer.mediaItemCount > 0) {
            resumePlayerOverlayFromBackground()
            return
        }

        val effectiveQueue = if (queue.isEmpty()) filteredVideos.value.ifEmpty { listOf(video) } else queue
        val resumePos = if (startFromBeginning || video.lastPositionMs >= video.durationMs - 3000L) 0L else video.lastPositionMs

        if (!video.externalSubtitleUri.isNullOrBlank()) {
            loadedExternalCues = SubtitleParserEngine.parseSubtitleUri(
                getApplication(),
                Uri.parse(video.externalSubtitleUri)
            )
        } else {
            loadedExternalCues = emptyList()
        }

        val deviceBrightness = readDeviceBrightnessFraction(getApplication())

        val effectiveStreamPlaylist = when {
            queue.isNotEmpty() && queue.all { it.isNetworkStream } -> queue
            _playerState.value.streamPlaylist.any { it.id == video.id || it.uri == video.uri } -> _playerState.value.streamPlaylist
            video.isNetworkStream -> listOf(video)
            else -> emptyList()
        }

        _playerState.update {
            it.copy(
                activeVideo = video.copy(lastPositionMs = resumePos),
                isPlayerOverlayOpen = true,
                currentPositionMs = resumePos,
                durationMs = video.durationMs.coerceAtLeast(1L),
                activePlaylistQueue = effectiveQueue,
                streamPlaylist = effectiveStreamPlaylist,
                playerErrorText = null,
                zoomScale = 1f,
                panOffsetX = 0f,
                panOffsetY = 0f,
                screenBrightness = deviceBrightness,
                isBrightnessOverridden = false
            )
        }

        // For online streams, auto-scan in background if playlist has not been populated with channels/variants yet
        if (video.isNetworkStream && effectiveStreamPlaylist.size <= 1 && (video.uri.startsWith("http://", ignoreCase = true) || video.uri.startsWith("https://", ignoreCase = true))) {
            viewModelScope.launch {
                val remoteParsed = DragonStreamPlaylistHelper.fetchAndParseRemoteM3u(video.uri, video.title)
                if (remoteParsed.size > 1) {
                    val remoteEntities = remoteParsed.mapIndexed { idx, item ->
                        item.toVideoEntity(id = System.currentTimeMillis() + 1000L + idx)
                    }
                    _playerState.update { state ->
                        state.copy(
                            streamPlaylist = remoteEntities,
                            activePlaylistQueue = remoteEntities
                        )
                    }
                    showBannerMessage("Stream playlist loaded: ${remoteEntities.size} channels found")
                }
            }
        }

        try {
            val uriStr = video.uri.lowercase()
            val proto = video.streamProtocol.uppercase()
            val isHls = uriStr.contains(".m3u8") || uriStr.contains("m3u8") || proto.contains("HLS") || proto.contains("M3U")
            val isDash = uriStr.contains(".mpd") || proto.contains("DASH")
            val isSs = uriStr.contains(".ism") || proto.contains("SMOOTH")
            val isRtsp = uriStr.startsWith("rtsp://") || proto.contains("RTSP")

            val mimeType = when {
                isHls -> MimeTypes.APPLICATION_M3U8
                isDash -> MimeTypes.APPLICATION_MPD
                isSs -> MimeTypes.APPLICATION_SS
                isRtsp -> MimeTypes.APPLICATION_RTSP
                else -> null
            }

            val mediaItemBuilder = MediaItem.Builder()
                .setUri(Uri.parse(video.uri))
                .setMediaId(video.id.toString())
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(video.title)
                        .setSubtitle("${video.containerFormat} • ${video.videoCodec}")
                        .build()
                )

            if (mimeType != null) {
                mediaItemBuilder.setMimeType(mimeType)
            }

            if (video.isNetworkStream) {
                mediaItemBuilder.setLiveConfiguration(
                    MediaItem.LiveConfiguration.Builder()
                        .setMaxPlaybackSpeed(1.02f)
                        .setMinPlaybackSpeed(0.98f)
                        .setTargetOffsetMs(3000L)
                        .build()
                )
            }

            val mediaItem = mediaItemBuilder.build()

            exoPlayer.setMediaItem(mediaItem, resumePos)
            applyLoopModeToExoPlayer(_playerState.value.loopMode, effectiveQueue.size)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
            exoPlayer.setPlaybackSpeed(_playerState.value.playbackSpeed)
            audioProcessor.attachToPlayer(exoPlayer, audioConfig.value)
        } catch (e: Exception) {
            _playerState.update {
                it.copy(playerErrorText = "Streaming error (${e.localizedMessage ?: "ready"})")
            }
        }

        // Immediately display the Notification Panel for foreground & background playback
        refreshSystemNotificationNow()

        // Pre-cache the actual video frame thumbnail in memory and refresh notification bitmap
        viewModelScope.launch {
            StorageScannerHelper.loadVideoThumbnailBitmap(getApplication(), video)
            refreshSystemNotificationNow()
        }

        runCatching {
            val app = getApplication<Application>()
            app.startService(Intent(app, DragonPlaybackService::class.java))
        }
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            if (exoPlayer.playbackState == Player.STATE_ENDED) {
                exoPlayer.seekTo(0L)
            }
            exoPlayer.play()
        }
        refreshSystemNotificationNow()
    }

    fun closePlayerOverlay() {
        val currentVideo = _playerState.value.activeVideo
        val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
        if (currentVideo != null && pos > 0L) {
            viewModelScope.launch {
                repository.updateResumePosition(currentVideo.id, pos)
            }
        }
        val keepBgAudio = backgroundAudioEnabled.value
        if (!keepBgAudio) {
            exoPlayer.pause()
        }
        val deviceBrightness = readDeviceBrightnessFraction(getApplication())
        _playerState.update {
            it.copy(
                activeVideo = it.activeVideo?.copy(lastPositionMs = pos),
                currentPositionMs = pos,
                isPlayerOverlayOpen = false,
                isLongPress2xActive = false,
                isLandscapeFullscreen = false,
                screenBrightness = deviceBrightness,
                isBrightnessOverridden = false
            )
        }
        refreshSystemNotificationNow()
    }

    fun stopBackgroundAudioSession() {
        val currentVideo = _playerState.value.activeVideo
        val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
        if (currentVideo != null && pos > 0L) {
            viewModelScope.launch {
                repository.updateResumePosition(currentVideo.id, pos)
            }
        }
        exoPlayer.pause()
        exoPlayer.stop()
        DragonSystemAudioNotificationHelper.cancelBackgroundNotification(getApplication())
        _playerState.update {
            it.copy(
                activeVideo = null,
                isPlayerOverlayOpen = false,
                isPlaying = false,
                isLandscapeFullscreen = false
            )
        }
    }

    fun cycleNotificationPanelStyle() {
        val styles = DragonNotifPanelStyle.entries
        val current = themeConfig.value.notifPanelStyle
        val next = styles[(styles.indexOf(current) + 1) % styles.size]
        updateTheme { it.copy(notifPanelStyle = next) }
        showBannerMessage("Notification Deck Style: ${next.label}")
    }

    fun toggleLandscapeFullscreen() {
        val next = !_playerState.value.isLandscapeFullscreen
        _playerState.update { it.copy(isLandscapeFullscreen = next) }
        showBannerMessage(if (next) "Rotated to Landscape Full Screen" else "Returned to Portrait Orientation")
    }

    fun seekToPosition(targetMs: Long) {
        val dur = _playerState.value.durationMs.coerceAtLeast(1L)
        val clamped = targetMs.coerceIn(0L, dur)
        exoPlayer.seekTo(clamped)
        _playerState.update { it.copy(currentPositionMs = clamped) }
    }

    fun seekRelative(deltaMs: Long) {
        seekToPosition(_playerState.value.currentPositionMs + deltaMs)
    }

    /**
     * Frame-by-frame stepping control (-1 frame or +1 frame based on video's FPS).
     */
    fun stepSingleFrame(forward: Boolean) {
        exoPlayer.pause()
        val fps = (_playerState.value.activeVideo?.frameRate ?: 30f).coerceIn(24f, 120f)
        val frameDurationMs = (1000f / fps).toLong().coerceAtLeast(16L)
        val delta = if (forward) frameDurationMs else -frameDurationMs
        seekRelative(delta)
        showBannerMessage(if (forward) "Step +1 Frame (+${frameDurationMs}ms)" else "Step -1 Frame (-${frameDurationMs}ms)")
    }

    fun setPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.25f, 4.0f)
        exoPlayer.playbackParameters = PlaybackParameters(clamped)
        _playerState.update { it.copy(playbackSpeed = clamped, speedBeforeLongPress = clamped) }
    }

    fun onLongPressSpeedBurst(active: Boolean) {
        if (active && !_playerState.value.isLongPress2xActive) {
            val prev = _playerState.value.playbackSpeed
            exoPlayer.playbackParameters = PlaybackParameters(2.0f)
            _playerState.update {
                it.copy(
                    isLongPress2xActive = true,
                    speedBeforeLongPress = prev,
                    playbackSpeed = 2.0f
                )
            }
        } else if (!active && _playerState.value.isLongPress2xActive) {
            val restore = _playerState.value.speedBeforeLongPress
            exoPlayer.playbackParameters = PlaybackParameters(restore)
            _playerState.update {
                it.copy(
                    isLongPress2xActive = false,
                    playbackSpeed = restore
                )
            }
        }
    }

    private fun applyLoopModeToExoPlayer(mode: PlayerLoopMode, queueSize: Int = _playerState.value.activePlaylistQueue.size) {
        exoPlayer.repeatMode = when (mode) {
            PlayerLoopMode.LOOP_ONE, PlayerLoopMode.AB_REPEAT -> Player.REPEAT_MODE_ONE
            PlayerLoopMode.LOOP_ALL -> if (queueSize <= 1) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
            PlayerLoopMode.SHUFFLE_LOOP, PlayerLoopMode.OFF -> Player.REPEAT_MODE_OFF
        }
    }

    private fun handlePlaybackEndedForLoopMode() {
        val state = _playerState.value
        when (state.loopMode) {
            PlayerLoopMode.LOOP_ONE -> {
                exoPlayer.seekTo(0L)
                exoPlayer.play()
            }
            PlayerLoopMode.LOOP_ALL -> {
                if (state.activePlaylistQueue.size > 1) {
                    playNextInQueue(keepInBackgroundIfClosed = true)
                } else {
                    exoPlayer.seekTo(0L)
                    exoPlayer.play()
                }
            }
            PlayerLoopMode.SHUFFLE_LOOP -> {
                val queue = state.activePlaylistQueue
                if (queue.size > 1) {
                    val currentId = state.activeVideo?.id
                    val candidates = queue.filter { it.id != currentId }.ifEmpty { queue }
                    val randomNext = candidates.random()
                    val wasClosed = !state.isPlayerOverlayOpen
                    playVideo(randomNext, queue, startFromBeginning = true)
                    if (wasClosed) {
                        _playerState.update { it.copy(isPlayerOverlayOpen = false) }
                        refreshSystemNotificationNow()
                    }
                } else {
                    exoPlayer.seekTo(0L)
                    exoPlayer.play()
                }
            }
            PlayerLoopMode.AB_REPEAT -> {
                val startMs = state.loopAbStartMs.coerceAtLeast(0L)
                exoPlayer.seekTo(startMs)
                exoPlayer.play()
            }
            PlayerLoopMode.OFF -> Unit
        }
    }

    fun setLoopMode(mode: PlayerLoopMode) {
        val dur = _playerState.value.durationMs.coerceAtLeast(1000L)
        val pos = exoPlayer.currentPosition.coerceIn(0L, (dur - 500L).coerceAtLeast(0L))
        _playerState.update { state ->
            val abStart = if (mode == PlayerLoopMode.AB_REPEAT && state.loopAbEndMs == null) {
                pos
            } else {
                state.loopAbStartMs
            }
            val abEnd = if (mode == PlayerLoopMode.AB_REPEAT && state.loopAbEndMs == null) {
                (pos + 15_000L).coerceAtMost(dur).let { if (it <= abStart + 500L) dur else it }
            } else {
                state.loopAbEndMs
            }
            state.copy(
                loopMode = mode,
                loopAbStartMs = abStart,
                loopAbEndMs = abEnd
            )
        }
        applyLoopModeToExoPlayer(mode)
        showBannerMessage("Loop Mode: ${mode.label} (${mode.shortBadge})")
    }

    fun cycleLoopMode() {
        val modes = PlayerLoopMode.entries
        val current = _playerState.value.loopMode
        val next = modes[(modes.indexOf(current) + 1) % modes.size]
        setLoopMode(next)
    }

    fun setAbLoopPointA(positionMs: Long = exoPlayer.currentPosition) {
        val dur = _playerState.value.durationMs.coerceAtLeast(1000L)
        val clampedA = positionMs.coerceIn(0L, (dur - 1000L).coerceAtLeast(0L))
        _playerState.update { state ->
            val currentB = state.loopAbEndMs
            val effectiveB = if (currentB == null || currentB <= clampedA + 500L) {
                (clampedA + 15_000L).coerceAtMost(dur)
            } else {
                currentB
            }
            state.copy(
                loopMode = PlayerLoopMode.AB_REPEAT,
                loopAbStartMs = clampedA,
                loopAbEndMs = effectiveB
            )
        }
        applyLoopModeToExoPlayer(PlayerLoopMode.AB_REPEAT)
        showBannerMessage("Loop Point A set at ${StorageScannerHelper.formatDurationMs(clampedA)}")
    }

    fun setAbLoopPointB(positionMs: Long = exoPlayer.currentPosition) {
        val dur = _playerState.value.durationMs.coerceAtLeast(1000L)
        val startA = _playerState.value.loopAbStartMs.coerceIn(0L, (dur - 1000L).coerceAtLeast(0L))
        val clampedB = positionMs.coerceIn(startA + 500L, dur)
        _playerState.update { state ->
            state.copy(
                loopMode = PlayerLoopMode.AB_REPEAT,
                loopAbStartMs = startA,
                loopAbEndMs = clampedB
            )
        }
        applyLoopModeToExoPlayer(PlayerLoopMode.AB_REPEAT)
        showBannerMessage("Loop Point B set at ${StorageScannerHelper.formatDurationMs(clampedB)}")
    }

    fun clearAbLoopPoints() {
        _playerState.update { state ->
            state.copy(
                loopMode = if (state.loopMode == PlayerLoopMode.AB_REPEAT) PlayerLoopMode.OFF else state.loopMode,
                loopAbStartMs = 0L,
                loopAbEndMs = null
            )
        }
        applyLoopModeToExoPlayer(_playerState.value.loopMode)
        showBannerMessage("A–B Loop segment cleared")
    }

    fun cycleAspectRatio() {
        val modes = AspectRatioMode.entries
        val nextIndex = (modes.indexOf(_playerState.value.aspectRatioMode) + 1) % modes.size
        val nextMode = modes[nextIndex]
        _playerState.update { it.copy(aspectRatioMode = nextMode) }
        showBannerMessage("Aspect Ratio: ${nextMode.label}")
    }

    fun updateZoomAndPan(scaleMultiplier: Float, panDeltaX: Float, panDeltaY: Float) {
        _playerState.update { state ->
            val newScale = (state.zoomScale * scaleMultiplier).coerceIn(0.5f, 4.0f)
            val maxPan = 600f * newScale
            val newX = if (newScale <= 1.02f) 0f else (state.panOffsetX + panDeltaX).coerceIn(-maxPan, maxPan)
            val newY = if (newScale <= 1.02f) 0f else (state.panOffsetY + panDeltaY).coerceIn(-maxPan, maxPan)
            state.copy(zoomScale = newScale, panOffsetX = newX, panOffsetY = newY)
        }
    }

    fun resetZoomAndPan() {
        _playerState.update { it.copy(zoomScale = 1.0f, panOffsetX = 0f, panOffsetY = 0f) }
    }

    fun toggleHudLock() {
        _playerState.update { it.copy(isHudLocked = !it.isHudLocked) }
    }

    fun syncBrightnessWithDevice() {
        val deviceBrightness = readDeviceBrightnessFraction(getApplication())
        _playerState.update {
            if (it.isPlayerOverlayOpen && it.isBrightnessOverridden) {
                it
            } else {
                it.copy(
                    screenBrightness = deviceBrightness,
                    isBrightnessOverridden = false
                )
            }
        }
    }

    fun updateScreenBrightness(newBrightness: Float) {
        _playerState.update {
            it.copy(
                screenBrightness = newBrightness.coerceIn(0.01f, 1.0f),
                isBrightnessOverridden = true
            )
        }
    }

    fun playNextInQueue(keepInBackgroundIfClosed: Boolean = false) {
        val state = _playerState.value
        val current = state.activeVideo ?: return
        val wasOverlayClosed = !state.isPlayerOverlayOpen && keepInBackgroundIfClosed
        val queue = if (state.streamPlaylist.size > 1) state.streamPlaylist else state.activePlaylistQueue
        if (queue.size <= 1) return
        val idx = queue.indexOfFirst { it.id == current.id || it.uri == current.uri }
        val nextIdx = if (idx >= 0) (idx + 1) % queue.size else 0
        val nextVideo = queue[nextIdx]
        playVideo(nextVideo, queue, startFromBeginning = true)
        if (wasOverlayClosed) {
            _playerState.update { it.copy(isPlayerOverlayOpen = false) }
            refreshSystemNotificationNow()
        }
    }

    fun playPreviousInQueue(keepInBackgroundIfClosed: Boolean = false) {
        val state = _playerState.value
        val current = state.activeVideo ?: return
        val wasOverlayClosed = !state.isPlayerOverlayOpen && keepInBackgroundIfClosed
        val queue = if (state.streamPlaylist.size > 1) state.streamPlaylist else state.activePlaylistQueue
        if (queue.size <= 1) {
            seekToPosition(0L)
            return
        }
        val idx = queue.indexOfFirst { it.id == current.id || it.uri == current.uri }
        val prevIdx = if (idx <= 0) queue.lastIndex else idx - 1
        playVideo(queue[prevIdx], queue, startFromBeginning = true)
        if (wasOverlayClosed) {
            _playerState.update { it.copy(isPlayerOverlayOpen = false) }
            refreshSystemNotificationNow()
        }
    }

    fun captureCurrentFrameToGallery() {
        val currentVideo = _playerState.value.activeVideo ?: return
        val pos = _playerState.value.currentPositionMs
        viewModelScope.launch {
            val resultMsg = StorageScannerHelper.captureAndSaveVideoFrame(getApplication(), currentVideo, pos)
            showBannerMessage(resultMsg)
        }
    }

    fun setPipModeState(inPip: Boolean) {
        _playerState.update { it.copy(isInPipMode = inPip) }
    }

    // Subtitle & Audio Studio updates
    fun updateSubtitleStyle(transform: (SubtitleStyleConfig) -> SubtitleStyleConfig) {
        val updated = transform(subtitleConfig.value)
        viewModelScope.launch {
            repository.updateSubtitleConfig(updated)
        }
    }

    fun importExternalSubtitleUri(uri: Uri) {
        val app = getApplication<Application>()
        val cues = SubtitleParserEngine.parseSubtitleUri(app, uri)
        loadedExternalCues = cues
        val name = uri.lastPathSegment?.substringAfterLast('/') ?: "External_Subtitles.srt"
        val ext = name.substringAfterLast('.', "srt")
        val active = _playerState.value.activeVideo
        viewModelScope.launch {
            if (active != null) {
                repository.attachExternalSubtitle(active.id, uri.toString(), ext)
            }
            val updated = subtitleConfig.value.copy(
                enabled = true,
                selectedTrackId = "external_custom",
                externalSubtitleName = name
            )
            repository.updateSubtitleConfig(updated)
            showBannerMessage("Loaded external subtitle ($name • ${cues.size} cues)")
        }
    }

    fun updateAudioEngine(transform: (AudioEngineConfig) -> AudioEngineConfig) {
        val updated = transform(audioConfig.value)
        audioProcessor.applyConfig(exoPlayer, updated)
        viewModelScope.launch {
            repository.updateAudioConfig(updated)
        }
    }

    fun setVolumeBoostPercent(boostPercent: Int) {
        val clamped = boostPercent.coerceIn(0, 200)
        updateAudioEngine { it.copy(volumeBoostPercent = clamped) }
    }

    // Storage Scanning, Runtime Permission & File Picker Actions
    fun refreshStoragePermissionState() {
        val granted = StorageScannerHelper.hasStoragePermission(getApplication())
        _hasStoragePermission.value = granted
    }

    fun onStoragePermissionsResult(results: Map<String, Boolean>) {
        val granted = StorageScannerHelper.hasStoragePermission(getApplication()) ||
            results.values.any { it }
        _hasStoragePermission.value = granted
        if (granted) {
            scanDeviceStorage(fromPermissionGrant = true)
        } else {
            showBannerMessage("Allow Storage Permission anytime to auto-import all local videos")
        }
    }

    fun scanDeviceStorage(fromPermissionGrant: Boolean = false) {
        viewModelScope.launch {
            _isScanningStorage.value = true
            _hasStoragePermission.value = StorageScannerHelper.hasStoragePermission(getApplication())
            val discovered = StorageScannerHelper.scanMediaStoreVideos(getApplication())
            discovered.forEach { repository.insertOrUpdateVideo(it) }
            _isScanningStorage.value = false
            showBannerMessage(
                when {
                    discovered.isNotEmpty() ->
                        "Auto-imported ${discovered.size} local video file(s) from device storage"
                    fromPermissionGrant ->
                        "Storage permission granted • Ready to import or open local video files"
                    else ->
                        "No videos found in MediaStore • Use 'Open Video' or 'Mount Folder' to add files"
                }
            )
        }
    }

    fun onSafFolderPicked(treeUri: Uri) {
        val app = getApplication<Application>()
        runCatching {
            app.contentResolver.takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }
        viewModelScope.launch {
            showBannerMessage("Scanning folder for video files…")
            val videos = StorageScannerHelper.scanSafFolderTree(app, treeUri)
            videos.forEach { repository.insertOrUpdateVideo(it) }
            showBannerMessage("Imported ${videos.size} video files from folder")
        }
    }

    fun onSingleVideoPicked(uri: Uri) {
        val app = getApplication<Application>()
        runCatching {
            app.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        viewModelScope.launch {
            val video = StorageScannerHelper.extractVideoEntityFromUri(app, uri)
            val id = repository.insertOrUpdateVideo(video)
            val playEntity = video.copy(id = id)
            playVideo(playEntity)
        }
    }

    fun addAndPlayNetworkStream(title: String, url: String, protocol: String) {
        val cleanInput = url.trim()
        if (cleanInput.isEmpty()) return
        val effectiveTitle = title.trim().ifEmpty {
            "Network Stream (${protocol.uppercase()})"
        }

        // 1. Direct multi-link or pasted M3U text detection
        val directParsed = DragonStreamPlaylistHelper.parseM3uOrMultipleUrls(
            content = cleanInput,
            baseUrl = null,
            defaultTitle = effectiveTitle
        )

        if (directParsed.size > 1) {
            val entities = directParsed.mapIndexed { idx, item ->
                item.toVideoEntity(id = System.currentTimeMillis() + idx)
            }
            _playerState.update { it.copy(streamPlaylist = entities) }
            viewModelScope.launch {
                entities.forEach { repository.insertOrUpdateVideo(it) }
            }
            showBannerMessage("Loaded playlist with ${entities.size} stream channels")
            playVideo(entities.first(), entities)
            return
        }

        // 2. Single URL provided (e.g. online HLS/M3U8 or stream link)
        viewModelScope.launch {
            val entity = VideoEntity(
                title = effectiveTitle,
                uri = cleanInput,
                folderName = "Network Streams",
                durationMs = 1800_000L,
                sizeBytes = 0L,
                dateAddedEpochSec = System.currentTimeMillis() / 1000L,
                width = 1920,
                height = 1080,
                containerFormat = protocol.uppercase(),
                videoCodec = "Adaptive Stream ($protocol)",
                audioCodec = "AAC / EAC3 / Opus",
                frameRate = 60f,
                bitrateKbps = 12000,
                isNetworkStream = true,
                streamProtocol = protocol.uppercase()
            )
            val id = repository.insertOrUpdateVideo(entity)
            val savedEntity = entity.copy(id = id)

            _playerState.update { it.copy(streamPlaylist = listOf(savedEntity)) }
            playVideo(savedEntity, listOf(savedEntity))

            // 3. Asynchronously inspect remote .m3u / .m3u8 link or adaptive streams for multiple channels or variants
            if (cleanInput.startsWith("http://", ignoreCase = true) || cleanInput.startsWith("https://", ignoreCase = true)) {
                val remoteParsed = DragonStreamPlaylistHelper.fetchAndParseRemoteM3u(cleanInput, effectiveTitle)
                if (remoteParsed.size > 1) {
                    val remoteEntities = remoteParsed.mapIndexed { idx, item ->
                        item.toVideoEntity(id = System.currentTimeMillis() + 1000L + idx)
                    }
                    _playerState.update { state ->
                        state.copy(
                            streamPlaylist = remoteEntities,
                            activePlaylistQueue = remoteEntities
                        )
                    }
                    showBannerMessage("Stream playlist ready: ${remoteEntities.size} channels found")
                }
            }
        }
    }

    fun rescanActiveStreamPlaylist() {
        val currentVideo = _playerState.value.activeVideo ?: return
        if (!currentVideo.isNetworkStream) return
        showBannerMessage("Scanning stream links & variants…")
        viewModelScope.launch {
            val remoteParsed = DragonStreamPlaylistHelper.fetchAndParseRemoteM3u(currentVideo.uri, currentVideo.title)
            if (remoteParsed.size > 1) {
                val remoteEntities = remoteParsed.mapIndexed { idx, item ->
                    item.toVideoEntity(id = System.currentTimeMillis() + 1000L + idx)
                }
                _playerState.update { state ->
                    state.copy(
                        streamPlaylist = remoteEntities,
                        activePlaylistQueue = remoteEntities
                    )
                }
                showBannerMessage("Playlist updated: ${remoteEntities.size} channels found")
            } else if (remoteParsed.size == 1) {
                val singleEntity = remoteParsed.first().toVideoEntity(currentVideo.id)
                _playerState.update { state ->
                    state.copy(streamPlaylist = listOf(singleEntity))
                }
                showBannerMessage("1 active stream channel verified")
            } else {
                showBannerMessage("No multiple streams detected in this link")
            }
        }
    }

    fun switchStreamFromPlaylist(selectedStream: VideoEntity) {
        val currentPlaylist = _playerState.value.streamPlaylist.ifEmpty { _playerState.value.activePlaylistQueue }
        playVideo(selectedStream, currentPlaylist)
        showBannerMessage("Switched to stream: ${selectedStream.title}")
    }

    fun resetRecentNetworkStreams() {
        if (_playerState.value.activeVideo?.isNetworkStream == true) {
            exoPlayer.stop()
            _playerState.update {
                it.copy(
                    activeVideo = null,
                    isPlayerOverlayOpen = false,
                    isPlaying = false,
                    isLandscapeFullscreen = false
                )
            }
        }
        viewModelScope.launch {
            repository.clearAllNetworkStreams()
            showBannerMessage("Recent network streams reset")
        }
    }

    fun removeSingleNetworkStream(streamVideo: VideoEntity) {
        if (_playerState.value.activeVideo?.id == streamVideo.id) {
            exoPlayer.stop()
            _playerState.update {
                it.copy(
                    activeVideo = null,
                    isPlayerOverlayOpen = false,
                    isPlaying = false,
                    isLandscapeFullscreen = false
                )
            }
        }
        viewModelScope.launch {
            repository.deleteVideoForever(streamVideo.id)
            showBannerMessage("Removed stream '${streamVideo.title}'")
        }
    }

    fun saveStreamBookmark(title: String, url: String, protocol: String) {
        val cleanUrl = url.trim()
        if (cleanUrl.isEmpty()) return
        viewModelScope.launch {
            repository.saveStreamBookmark(title, cleanUrl, protocol)
            showBannerMessage("Bookmarked stream link: ${title.ifBlank { cleanUrl.take(28) }}")
        }
    }

    fun removeStreamBookmark(bookmark: StreamBookmarkEntity) {
        viewModelScope.launch {
            repository.removeStreamBookmark(bookmark.id)
            showBannerMessage("Removed bookmark '${bookmark.title}'")
        }
    }

    fun playBookmarkedStream(bookmark: StreamBookmarkEntity) {
        addAndPlayNetworkStream(bookmark.title, bookmark.url, bookmark.protocol)
    }

    // Trash Bin & Permanent Delete Actions
    fun moveVideoToTrash(video: VideoEntity) {
        if (_playerState.value.activeVideo?.id == video.id) {
            exoPlayer.stop()
            _playerState.update {
                it.copy(
                    activeVideo = null,
                    isPlayerOverlayOpen = false,
                    isPlaying = false,
                    isLandscapeFullscreen = false
                )
            }
        }
        viewModelScope.launch {
            repository.moveVideoToTrash(video.id)
            showBannerMessage("Moved '${video.title}' to Trash Bin")
        }
    }

    fun restoreVideoFromTrash(video: VideoEntity) {
        viewModelScope.launch {
            repository.restoreVideoFromTrash(video.id)
            showBannerMessage("Restored '${video.title}' from Trash Bin")
        }
    }

    fun restoreAllFromTrash() {
        viewModelScope.launch {
            repository.restoreAllVideosFromTrash()
            showBannerMessage("All videos restored from Trash Bin")
        }
    }

    fun deleteVideoForever(video: VideoEntity) {
        viewModelScope.launch {
            val intentSender = StorageScannerHelper.deletePhysicalVideoFiles(
                getApplication(),
                listOf(video)
            )
            repository.deleteVideoForever(video.id)
            if (intentSender != null) {
                _pendingDeleteIntentSender.value = intentSender
            } else {
                showBannerMessage("Permanently deleted '${video.title}'")
            }
        }
    }

    fun emptyTrashBinForever() {
        val currentTrashed = trashedVideos.value
        if (currentTrashed.isEmpty()) return
        viewModelScope.launch {
            val intentSender = StorageScannerHelper.deletePhysicalVideoFiles(
                getApplication(),
                currentTrashed
            )
            repository.emptyTrashBinForever()
            if (intentSender != null) {
                _pendingDeleteIntentSender.value = intentSender
            } else {
                showBannerMessage("Trash Bin emptied permanently")
            }
        }
    }

    fun clearPendingDeleteIntentSender() {
        _pendingDeleteIntentSender.value = null
    }

    // Favorites, Playlists & Vault
    fun toggleFavorite(video: VideoEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(video)
        }
    }

    fun toggleVaultForVideo(video: VideoEntity) {
        viewModelScope.launch {
            repository.toggleVaultStatus(video)
            showBannerMessage(
                if (!video.isVaultHidden) "Moved '${video.title}' to Private Dragon Vault"
                else "Restored '${video.title}' to Public Library"
            )
        }
    }

    fun attemptUnlockVault(enteredPin: String): Boolean {
        val expected = vaultPin.value
        return if (enteredPin == expected) {
            _isVaultUnlocked.value = true
            showBannerMessage("Dragon Vault Unlocked")
            true
        } else {
            showBannerMessage("Incorrect Vault PIN (Default PIN is 1234)")
            false
        }
    }

    fun lockVault() {
        _isVaultUnlocked.value = false
    }

    fun updateVaultPin(newPin: String) {
        if (newPin.length >= 4) {
            viewModelScope.launch {
                repository.setVaultPin(newPin)
                showBannerMessage("Vault PIN updated")
            }
        }
    }

    fun createPlaylist(name: String, description: String, initialVideoIds: List<Long> = emptyList()) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.createPlaylist(name, description, initialVideoIds)
            showBannerMessage("Created playlist '${name.trim()}'")
        }
    }

    fun toggleVideoInPlaylist(playlist: PlaylistEntity, videoId: Long) {
        viewModelScope.launch {
            repository.toggleVideoInPlaylist(playlist, videoId)
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
            showBannerMessage("Playlist deleted")
        }
    }

    // Theme & Decoder settings
    fun updateTheme(transform: (DragonThemeConfig) -> DragonThemeConfig) {
        viewModelScope.launch {
            repository.updateThemeConfig(transform, themeConfig.value)
        }
    }

    fun setThemePreset(preset: DragonThemePreset) {
        updateTheme { it.copy(preset = preset, useCustomAccent = false) }
    }

    fun setDecoderMode(mode: DecoderMode) {
        viewModelScope.launch {
            repository.setDecoderMode(mode)
            showBannerMessage("Decoder Engine: ${mode.label} (${mode.badge})")
        }
    }

    fun setBackgroundAudio(enabled: Boolean) {
        viewModelScope.launch {
            repository.setBackgroundAudioEnabled(enabled)
            showBannerMessage(if (enabled) "Background Audio Playback Enabled" else "Background Audio Disabled")
        }
    }

    override fun onCleared() {
        audioProcessor.releaseEffects()
        exoPlayer.release()
        super.onCleared()
    }
}
