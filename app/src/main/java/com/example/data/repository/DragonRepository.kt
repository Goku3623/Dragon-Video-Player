package com.example.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.local.DragonDao
import com.example.data.local.PlaylistEntity
import com.example.data.local.VideoEntity
import com.example.domain.model.AudioChannelMode
import com.example.domain.model.AudioEngineConfig
import com.example.domain.model.CodecCapabilitySpec
import com.example.domain.model.DecoderMode
import com.example.domain.model.EqualizerPreset
import com.example.domain.model.SubtitleColorChoice
import com.example.domain.model.SubtitleFontChoice
import com.example.domain.model.SubtitleStyleConfig
import com.example.ui.theme.DragonThemeConfig
import com.example.ui.theme.DragonThemePreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dragonDataStore by preferencesDataStore(name = "dragon_player_prefs")

class DragonRepository(
    private val context: Context,
    private val dao: DragonDao
) {
    private object Keys {
        val THEME_PRESET = stringPreferencesKey("theme_preset")
        val USE_CUSTOM_ACCENT = booleanPreferencesKey("use_custom_accent")
        val CUSTOM_ACCENT_HUE = floatPreferencesKey("custom_accent_hue")
        val GLOW_INTENSITY = floatPreferencesKey("glow_intensity")
        val HUD_CORNER_RADIUS = floatPreferencesKey("hud_corner_radius")
        val BACKDROP_BLUR = floatPreferencesKey("backdrop_blur")
        val DRAGON_SCALE_MESH = booleanPreferencesKey("dragon_scale_mesh")

        val DECODER_MODE = stringPreferencesKey("decoder_mode")
        val BACKGROUND_AUDIO = booleanPreferencesKey("background_audio")
        val VAULT_PIN = stringPreferencesKey("vault_pin")

        val SUB_ENABLED = booleanPreferencesKey("sub_enabled")
        val SUB_TRACK = stringPreferencesKey("sub_track")
        val SUB_FONT = stringPreferencesKey("sub_font")
        val SUB_SIZE = floatPreferencesKey("sub_size")
        val SUB_COLOR = stringPreferencesKey("sub_color")
        val SUB_OUTLINE = floatPreferencesKey("sub_outline")
        val SUB_BG_OPACITY = floatPreferencesKey("sub_bg_opacity")
        val SUB_SYNC_MS = longPreferencesKey("sub_sync_ms")

        val AUDIO_TRACK = stringPreferencesKey("audio_track")
        val AUDIO_CHANNEL = stringPreferencesKey("audio_channel")
        val AUDIO_BOOST = intPreferencesKey("audio_boost")
        val AUDIO_DELAY_MS = longPreferencesKey("audio_delay_ms")
        val AUDIO_EQ_PRESET = stringPreferencesKey("audio_eq_preset")
        val AUDIO_BASS_BOOST = intPreferencesKey("audio_bass_boost")
    }

    val publicVideosFlow: Flow<List<VideoEntity>> = dao.observePublicVideos()
    val vaultVideosFlow: Flow<List<VideoEntity>> = dao.observeVaultVideos()
    val trashedVideosFlow: Flow<List<VideoEntity>> = dao.observeTrashedVideos()
    val playlistsFlow: Flow<List<PlaylistEntity>> = dao.observePlaylists()

    val themeConfigFlow: Flow<DragonThemeConfig> = context.dragonDataStore.data.map { prefs ->
        val presetName = prefs[Keys.THEME_PRESET] ?: DragonThemePreset.DRAGON_FLAME.name
        val preset = DragonThemePreset.entries.find { it.name == presetName } ?: DragonThemePreset.DRAGON_FLAME
        DragonThemeConfig(
            preset = preset,
            useCustomAccent = prefs[Keys.USE_CUSTOM_ACCENT] ?: false,
            customAccentHue = prefs[Keys.CUSTOM_ACCENT_HUE] ?: 348f,
            glowIntensity = prefs[Keys.GLOW_INTENSITY] ?: 0.75f,
            hudCornerRadiusDp = prefs[Keys.HUD_CORNER_RADIUS] ?: 18f,
            backdropBlurStrength = prefs[Keys.BACKDROP_BLUR] ?: 0.72f,
            showDragonScalePattern = prefs[Keys.DRAGON_SCALE_MESH] ?: true
        )
    }

    val decoderModeFlow: Flow<DecoderMode> = context.dragonDataStore.data.map { prefs ->
        val name = prefs[Keys.DECODER_MODE] ?: DecoderMode.AUTO_HW_PLUS.name
        DecoderMode.entries.find { it.name == name } ?: DecoderMode.AUTO_HW_PLUS
    }

    val backgroundAudioEnabledFlow: Flow<Boolean> = context.dragonDataStore.data.map { prefs ->
        prefs[Keys.BACKGROUND_AUDIO] ?: true
    }

    val vaultPinFlow: Flow<String> = context.dragonDataStore.data.map { prefs ->
        prefs[Keys.VAULT_PIN] ?: "1234"
    }

    val subtitleConfigFlow: Flow<SubtitleStyleConfig> = context.dragonDataStore.data.map { prefs ->
        val fontName = prefs[Keys.SUB_FONT] ?: SubtitleFontChoice.PLUS_JAKARTA.name
        val colorName = prefs[Keys.SUB_COLOR] ?: SubtitleColorChoice.PURE_WHITE.name
        SubtitleStyleConfig(
            enabled = prefs[Keys.SUB_ENABLED] ?: true,
            selectedTrackId = prefs[Keys.SUB_TRACK] ?: "en_builtin",
            fontChoice = SubtitleFontChoice.entries.find { it.name == fontName } ?: SubtitleFontChoice.PLUS_JAKARTA,
            fontSizeSp = prefs[Keys.SUB_SIZE] ?: 20f,
            textColorChoice = SubtitleColorChoice.entries.find { it.name == colorName } ?: SubtitleColorChoice.PURE_WHITE,
            outlineWidthDp = prefs[Keys.SUB_OUTLINE] ?: 2.0f,
            backgroundOpacity = prefs[Keys.SUB_BG_OPACITY] ?: 0.55f,
            syncOffsetMs = prefs[Keys.SUB_SYNC_MS] ?: 0L
        )
    }

    val audioConfigFlow: Flow<AudioEngineConfig> = context.dragonDataStore.data.map { prefs ->
        val channelName = prefs[Keys.AUDIO_CHANNEL] ?: AudioChannelMode.STEREO.name
        val eqName = prefs[Keys.AUDIO_EQ_PRESET] ?: EqualizerPreset.DRAGON_CINEMA.name
        val eqPreset = EqualizerPreset.entries.find { it.name == eqName } ?: EqualizerPreset.DRAGON_CINEMA
        AudioEngineConfig(
            selectedTrackId = prefs[Keys.AUDIO_TRACK] ?: "audio_main",
            channelMode = AudioChannelMode.entries.find { it.name == channelName } ?: AudioChannelMode.STEREO,
            volumeBoostPercent = prefs[Keys.AUDIO_BOOST] ?: 100,
            audioDelayMs = prefs[Keys.AUDIO_DELAY_MS] ?: 0L,
            equalizerPreset = eqPreset,
            customBandGainsDb = eqPreset.bandGainsDb,
            bassBoostStrength = prefs[Keys.AUDIO_BASS_BOOST] ?: 450
        )
    }

    suspend fun updateThemeConfig(transform: (DragonThemeConfig) -> DragonThemeConfig, current: DragonThemeConfig) {
        val updated = transform(current)
        context.dragonDataStore.edit { prefs ->
            prefs[Keys.THEME_PRESET] = updated.preset.name
            prefs[Keys.USE_CUSTOM_ACCENT] = updated.useCustomAccent
            prefs[Keys.CUSTOM_ACCENT_HUE] = updated.customAccentHue
            prefs[Keys.GLOW_INTENSITY] = updated.glowIntensity
            prefs[Keys.HUD_CORNER_RADIUS] = updated.hudCornerRadiusDp
            prefs[Keys.BACKDROP_BLUR] = updated.backdropBlurStrength
            prefs[Keys.DRAGON_SCALE_MESH] = updated.showDragonScalePattern
        }
    }

    suspend fun setDecoderMode(mode: DecoderMode) {
        context.dragonDataStore.edit { it[Keys.DECODER_MODE] = mode.name }
    }

    suspend fun setBackgroundAudioEnabled(enabled: Boolean) {
        context.dragonDataStore.edit { it[Keys.BACKGROUND_AUDIO] = enabled }
    }

    suspend fun setVaultPin(newPin: String) {
        context.dragonDataStore.edit { it[Keys.VAULT_PIN] = newPin }
    }

    suspend fun updateSubtitleConfig(config: SubtitleStyleConfig) {
        context.dragonDataStore.edit { prefs ->
            prefs[Keys.SUB_ENABLED] = config.enabled
            prefs[Keys.SUB_TRACK] = config.selectedTrackId
            prefs[Keys.SUB_FONT] = config.fontChoice.name
            prefs[Keys.SUB_SIZE] = config.fontSizeSp
            prefs[Keys.SUB_COLOR] = config.textColorChoice.name
            prefs[Keys.SUB_OUTLINE] = config.outlineWidthDp
            prefs[Keys.SUB_BG_OPACITY] = config.backgroundOpacity
            prefs[Keys.SUB_SYNC_MS] = config.syncOffsetMs
        }
    }

    suspend fun updateAudioConfig(config: AudioEngineConfig) {
        context.dragonDataStore.edit { prefs ->
            prefs[Keys.AUDIO_TRACK] = config.selectedTrackId
            prefs[Keys.AUDIO_CHANNEL] = config.channelMode.name
            prefs[Keys.AUDIO_BOOST] = config.volumeBoostPercent
            prefs[Keys.AUDIO_DELAY_MS] = config.audioDelayMs
            prefs[Keys.AUDIO_EQ_PRESET] = config.equalizerPreset.name
            prefs[Keys.AUDIO_BASS_BOOST] = config.bassBoostStrength
        }
    }

    suspend fun insertOrUpdateVideo(video: VideoEntity): Long {
        val existing = dao.findVideoByUri(video.uri)
        return if (existing != null) {
            dao.updateVideo(
                video.copy(
                    id = existing.id,
                    lastPositionMs = existing.lastPositionMs,
                    isFavorite = existing.isFavorite,
                    isVaultHidden = existing.isVaultHidden,
                    isTrashed = existing.isTrashed,
                    trashedAtEpochSec = existing.trashedAtEpochSec
                )
            )
            existing.id
        } else {
            dao.insertVideo(video)
        }
    }

    suspend fun updateResumePosition(videoId: Long, positionMs: Long) {
        dao.updateResumePosition(videoId, positionMs)
    }

    suspend fun toggleFavorite(video: VideoEntity) {
        dao.updateFavorite(video.id, !video.isFavorite)
    }

    suspend fun toggleVaultStatus(video: VideoEntity) {
        dao.updateVaultStatus(video.id, !video.isVaultHidden)
    }

    suspend fun moveVideoToTrash(videoId: Long) {
        dao.moveToTrash(videoId, System.currentTimeMillis() / 1000L)
    }

    suspend fun restoreVideoFromTrash(videoId: Long) {
        dao.restoreFromTrash(videoId)
    }

    suspend fun restoreAllVideosFromTrash() {
        dao.restoreAllTrashedVideos()
    }

    suspend fun attachExternalSubtitle(videoId: Long, uri: String, format: String) {
        dao.updateExternalSubtitle(videoId, uri, format)
    }

    suspend fun deleteVideoForever(videoId: Long) {
        dao.deleteVideoById(videoId)
    }

    suspend fun emptyTrashBinForever() {
        dao.deleteAllTrashedVideos()
    }

    suspend fun createPlaylist(name: String, description: String, videoIds: List<Long> = emptyList()) {
        dao.insertPlaylist(
            PlaylistEntity(
                name = name.trim(),
                description = description.trim().ifEmpty { "Custom Dragon Media Queue" },
                videoIdsCsv = videoIds.joinToString(",")
            )
        )
    }

    suspend fun toggleVideoInPlaylist(playlist: PlaylistEntity, videoId: Long) {
        val currentIds = playlist.getVideoIdList().toMutableList()
        if (currentIds.contains(videoId)) {
            currentIds.remove(videoId)
        } else {
            currentIds.add(videoId)
        }
        dao.updatePlaylist(playlist.copy(videoIdsCsv = currentIds.joinToString(",")))
    }

    suspend fun deletePlaylist(playlistId: Long) {
        dao.deletePlaylistById(playlistId)
    }

    fun getCodecCapabilitySpecs(): List<CodecCapabilitySpec> = listOf(
        CodecCapabilitySpec("Video Codecs", "H.265 / HEVC (Main & Main10)", "MediaCodec HW+ & FFmpeg", "Up to 8K 60fps, HDR10/Dolby Vision profile fallback", true),
        CodecCapabilitySpec("Video Codecs", "AV1 (AOMedia Video 1)", "MediaCodec HW+ & libdav1d / libav1", "Multi-threaded tile decoding for 4K/8K MKV & WebM", true),
        CodecCapabilitySpec("Video Codecs", "VP9 Profile 0/2 (10-bit)", "MediaCodec HW+ & bundled libvpx", "Seamless YouTube/WebM 60fps hardware & software decode", true),
        CodecCapabilitySpec("Video Codecs", "H.264 / AVC (High@L5.2)", "MediaCodec Hardware Direct", "Zero-copy SurfaceTexture rendering with low power draw", true),
        CodecCapabilitySpec("Audio Codecs", "Dolby AC3 / E-AC3 / TrueHD", "Bundled FFmpeg Audio Renderer", "5.1 & 7.1 surround channel mapping with Night Downmix", false),
        CodecCapabilitySpec("Audio Codecs", "DTS / DTS-HD Master Audio", "Bundled FFmpeg Audio Renderer", "Lossless core extraction & 200% LoudnessEnhancer DSP", false),
        CodecCapabilitySpec("Audio Codecs", "Opus & FLAC 24-bit/192kHz", "Bundled libopus & libflac", "Sample-accurate millisecond audio delay sync (-2s..+2s)", false),
        CodecCapabilitySpec("Containers", "MKV, MP4, AVI, WebM, FLV, TS, MOV", "Media3 DefaultExtractorsFactory", "Embedded multi-track audio & ASS/SSA/SRT/VTT chapters", true),
        CodecCapabilitySpec("Streaming", "HLS (.m3u8), DASH (.mpd), SmoothStreaming, RTSP", "Media3 Adaptive MediaSource", "Dynamic ABR bitrate switching & low-latency live buffer", true)
    )
}
