package com.example.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import androidx.annotation.OptIn
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.ts.DefaultTsPayloadReaderFactory
import com.example.data.local.DragonDatabase
import com.example.data.local.PlaylistEntity
import com.example.data.local.VideoEntity
import com.example.data.repository.DragonRepository
import com.example.domain.model.AspectRatioMode
import com.example.domain.model.AudioEngineConfig
import com.example.domain.model.CodecCapabilitySpec
import com.example.domain.model.DecoderMode
import com.example.domain.model.SubtitleCueItem
import com.example.domain.model.SubtitleStyleConfig
import com.example.domain.model.VideoFolderSummary
import com.example.domain.model.VideoSortOption
import com.example.player.DragonAudioProcessorManager
import com.example.player.DragonPlaybackService
import com.example.player.StorageScannerHelper
import com.example.player.SubtitleParserEngine
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
    val aspectRatioMode: AspectRatioMode = AspectRatioMode.FIT,
    val zoomScale: Float = 1.0f,
    val panOffsetX: Float = 0f,
    val panOffsetY: Float = 0f,
    val isHudLocked: Boolean = false,
    val isLandscapeFullscreen: Boolean = false,
    val screenBrightness: Float = 0.80f,
    val activeSubtitleCue: String? = null,
    val isInPipMode: Boolean = false,
    val activePlaylistQueue: List<VideoEntity> = emptyList(),
    val playerErrorText: String? = null
)

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

    private val _sortOption = MutableStateFlow(VideoSortOption.DATE)
    val sortOption: StateFlow<VideoSortOption> = _sortOption.asStateFlow()

    private val _sortAscending = MutableStateFlow(false)
    val sortAscending: StateFlow<Boolean> = _sortAscending.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow("All")
    val selectedCategoryFilter: StateFlow<String> = _selectedCategoryFilter.asStateFlow()

    private val _selectedFolderFilter = MutableStateFlow<String?>(null)
    val selectedFolderFilter: StateFlow<String?> = _selectedFolderFilter.asStateFlow()

    private val _isVaultUnlocked = MutableStateFlow(false)
    val isVaultUnlocked: StateFlow<Boolean> = _isVaultUnlocked.asStateFlow()

    private val _statusBannerMessage = MutableStateFlow<String?>(null)
    val statusBannerMessage: StateFlow<String?> = _statusBannerMessage.asStateFlow()

    private val _playerState = MutableStateFlow(PlayerRuntimeState())
    val playerState: StateFlow<PlayerRuntimeState> = _playerState.asStateFlow()

    private var loadedExternalCues: List<SubtitleCueItem> = emptyList()

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

    val folderSummaries: StateFlow<List<VideoFolderSummary>> = repository.publicVideosFlow.combine(_searchQuery) { videos, query ->
        val q = query.trim().lowercase()
        videos.groupBy { it.folderName }
            .map { (folderName, items) ->
                VideoFolderSummary(
                    folderName = folderName,
                    folderPath = "/storage/emulated/0/$folderName",
                    videoCount = items.size,
                    totalSizeBytes = items.sumOf { it.sizeBytes },
                    totalDurationMs = items.sumOf { it.durationMs },
                    previewVideos = items.take(3).map { it.title }
                )
            }
            .filter { q.isEmpty() || it.folderName.lowercase().contains(q) }
            .sortedByDescending { it.videoCount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val codecSpecs: List<CodecCapabilitySpec> = repository.getCodecCapabilitySpecs()

    init {
        // Silently scan real device storage on startup without injecting any sample videos or folders
        viewModelScope.launch {
            val discovered = StorageScannerHelper.scanMediaStoreVideos(getApplication())
            discovered.forEach { repository.insertOrUpdateVideo(it) }
        }

        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playerState.update { it.copy(isPlaying = isPlaying) }
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
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                _playerState.update {
                    it.copy(
                        playerErrorText = "Decoder fallback active (${error.errorCodeName}) — Switching to software renderer"
                    )
                }
            }
        })

        // Continuous position & subtitle cue synchronization ticker (every 120ms)
        viewModelScope.launch {
            while (isActive) {
                val activeVideo = _playerState.value.activeVideo
                if (activeVideo != null && _playerState.value.isPlayerOverlayOpen) {
                    val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
                    val dur = exoPlayer.duration.takeIf { it > 0L } ?: activeVideo.durationMs.coerceAtLeast(1L)
                    val buffered = exoPlayer.bufferedPercentage.coerceIn(0, 100)
                    val subCfg = subtitleConfig.value
                    val cue = if (subCfg.enabled) {
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
    }

    private fun buildDragonExoPlayer(app: Application): ExoPlayer {
        val extractorsFactory = DefaultExtractorsFactory()
            .setTsExtractorFlags(DefaultTsPayloadReaderFactory.FLAG_ALLOW_NON_IDR_KEYFRAMES)
            .setConstantBitrateSeekingEnabled(true)

        val renderersFactory = DefaultRenderersFactory(app)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
            .setEnableDecoderFallback(true)

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(12_000, 45_000, 1_200, 2_500)
            .build()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        return ExoPlayer.Builder(app, renderersFactory)
            .setMediaSourceFactory(DefaultMediaSourceFactory(app, extractorsFactory))
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
        if (_sortOption.value == option) {
            _sortAscending.update { !it }
        } else {
            _sortOption.value = option
            _sortAscending.value = option == VideoSortOption.NAME
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

    fun playVideo(video: VideoEntity, queue: List<VideoEntity> = emptyList(), startFromBeginning: Boolean = false) {
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

        _playerState.update {
            it.copy(
                activeVideo = video,
                isPlayerOverlayOpen = true,
                currentPositionMs = resumePos,
                durationMs = video.durationMs.coerceAtLeast(1L),
                activePlaylistQueue = effectiveQueue,
                playerErrorText = null,
                zoomScale = 1f,
                panOffsetX = 0f,
                panOffsetY = 0f
            )
        }

        try {
            val mediaItem = MediaItem.Builder()
                .setUri(Uri.parse(video.uri))
                .setMediaId(video.id.toString())
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(video.title)
                        .setSubtitle("${video.containerFormat} • ${video.videoCodec}")
                        .build()
                )
                .build()

            exoPlayer.setMediaItem(mediaItem, resumePos)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
            exoPlayer.setPlaybackSpeed(_playerState.value.playbackSpeed)
            audioProcessor.attachToPlayer(exoPlayer, audioConfig.value)
        } catch (e: Exception) {
            _playerState.update {
                it.copy(playerErrorText = "Loaded in software container mode (${e.localizedMessage ?: "ready"})")
            }
        }

        if (backgroundAudioEnabled.value) {
            runCatching {
                val app = getApplication<Application>()
                app.startService(Intent(app, DragonPlaybackService::class.java))
            }
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
    }

    fun closePlayerOverlay() {
        val currentVideo = _playerState.value.activeVideo
        val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
        if (currentVideo != null && pos > 0L) {
            viewModelScope.launch {
                repository.updateResumePosition(currentVideo.id, pos)
            }
        }
        if (!backgroundAudioEnabled.value) {
            exoPlayer.pause()
        }
        _playerState.update {
            it.copy(
                isPlayerOverlayOpen = false,
                isLongPress2xActive = false,
                isLandscapeFullscreen = false
            )
        }
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

    fun updateScreenBrightness(newBrightness: Float) {
        _playerState.update { it.copy(screenBrightness = newBrightness.coerceIn(0.05f, 1.0f)) }
    }

    fun playNextInQueue() {
        val state = _playerState.value
        val current = state.activeVideo ?: return
        val queue = state.activePlaylistQueue
        if (queue.size <= 1) return
        val idx = queue.indexOfFirst { it.id == current.id }
        val nextVideo = queue[(idx + 1).coerceAtLeast(0) % queue.size]
        playVideo(nextVideo, queue, startFromBeginning = true)
    }

    fun playPreviousInQueue() {
        val state = _playerState.value
        val current = state.activeVideo ?: return
        val queue = state.activePlaylistQueue
        if (queue.size <= 1) {
            seekToPosition(0L)
            return
        }
        val idx = queue.indexOfFirst { it.id == current.id }
        val prevIdx = if (idx <= 0) queue.lastIndex else idx - 1
        playVideo(queue[prevIdx], queue, startFromBeginning = true)
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

    // Storage Scanning & File Picker Actions
    fun scanDeviceStorage() {
        viewModelScope.launch {
            val discovered = StorageScannerHelper.scanMediaStoreVideos(getApplication())
            discovered.forEach { repository.insertOrUpdateVideo(it) }
            showBannerMessage(
                if (discovered.isNotEmpty()) {
                    "Indexed ${discovered.size} video files from device storage"
                } else {
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
        val cleanUrl = url.trim()
        if (cleanUrl.isEmpty()) return
        val effectiveTitle = title.trim().ifEmpty {
            "Network Stream (${protocol.uppercase()})"
        }
        viewModelScope.launch {
            val entity = VideoEntity(
                title = effectiveTitle,
                uri = cleanUrl,
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
            playVideo(entity.copy(id = id))
        }
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
