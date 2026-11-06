package com.example.domain.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import com.example.ui.theme.CinzelFontFamily
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.OrbitronFontFamily
import com.example.ui.theme.PlusJakartaFontFamily

enum class DecoderMode(val badge: String, val label: String, val description: String) {
    AUTO_HW_PLUS(
        badge = "HW+",
        label = "Hardware+ Hybrid",
        description = "Hardware MediaCodec acceleration with seamless FFmpeg, libvpx, libdav1d, libopus & libflac software fallback"
    ),
    HARDWARE_ONLY(
        badge = "HW",
        label = "Hardware Direct",
        description = "Direct GPU/SoC MediaCodec pipeline for maximum battery efficiency"
    ),
    SOFTWARE_FFMPEG(
        badge = "SW",
        label = "Software FFmpeg Engine",
        description = "Forces bundled multi-threaded software decoding (10-bit HEVC, AV1, TrueHD, DTS-HD, ASS/SSA)"
    )
}

enum class AspectRatioMode(val label: String, val ratioValue: Float?) {
    FIT("Fit Screen", null),
    FILL_16_9("16:9 Wide", 16f / 9f),
    FILL_18_9("18:9 Univisium", 18f / 9f),
    FILL_21_9("21:9 UltraWide", 21f / 9f),
    CROP("Center Crop", null),
    STRETCH("Full Stretch", null)
}

enum class PlayerLoopMode(
    val label: String,
    val shortBadge: String,
    val description: String
) {
    OFF(
        label = "Loop Off",
        shortBadge = "OFF",
        description = "Play video once without repeating"
    ),
    LOOP_ONE(
        label = "Loop Single Video",
        shortBadge = "1×",
        description = "Repeat the current video continuously"
    ),
    LOOP_ALL(
        label = "Loop Queue / All",
        shortBadge = "ALL",
        description = "Loop through all videos in the current queue"
    ),
    SHUFFLE_LOOP(
        label = "Shuffle & Loop",
        shortBadge = "SHUF",
        description = "Continuously play queue videos in shuffled order"
    ),
    AB_REPEAT(
        label = "A–B Segment Loop",
        shortBadge = "A-B",
        description = "Loop a custom section between Point A and Point B"
    )
}

enum class VideoSortOption(val label: String) {
    NAME("Name"),
    DATE("Date Added"),
    SIZE("File Size"),
    DURATION("Duration")
}

enum class FolderSortOption(val label: String) {
    VIDEO_COUNT("Video Count"),
    NAME("Folder Name"),
    TOTAL_SIZE("Total Size"),
    TOTAL_DURATION("Total Duration")
}

enum class MediaViewStyle(val label: String, val shortLabel: String) {
    DETAILED_LIST("Detailed List", "List"),
    COMPACT_LIST("Compact List", "Compact"),
    GRID_2_COLUMN("Grid (2 Columns)", "2× Grid"),
    GRID_3_COLUMN("Grid (3 Columns)", "3× Grid"),
    SHOWCASE_BANNER("Large Showcase", "Showcase")
}

enum class MediaCardSize(val label: String, val shortLabel: String, val scaleFactor: Float) {
    SMALL("Small Size", "S", 0.80f),
    MEDIUM("Medium Size", "M", 1.00f),
    LARGE("Large Size", "L", 1.24f)
}

enum class SubtitleFontChoice(val label: String) {
    PLUS_JAKARTA("Sans Clean"),
    JETBRAINS_MONO("Tech Mono"),
    CINZEL("Cinema Serif"),
    ORBITRON("Dragon Display");

    fun toFontFamily(): FontFamily = when (this) {
        PLUS_JAKARTA -> PlusJakartaFontFamily
        JETBRAINS_MONO -> JetBrainsMonoFontFamily
        CINZEL -> CinzelFontFamily
        ORBITRON -> OrbitronFontFamily
    }
}

enum class SubtitleColorChoice(val label: String, val color: Color) {
    PURE_WHITE("White", Color(0xFFFFFFFF)),
    DRAGON_GOLD("Dragon Gold", Color(0xFFFFD54F)),
    CYBER_CYAN("Cyber Cyan", Color(0xFF00F0FF)),
    EMERALD_NEON("Jade Neon", Color(0xFF00E676)),
    CRIMSON_FLAME("Flame Rose", Color(0xFFFF5277)),
    CINEMA_YELLOW("Cinema Amber", Color(0xFFFFEA00))
}

@Immutable
data class SubtitleStyleConfig(
    val enabled: Boolean = true,
    val selectedTrackId: String = "en_builtin",
    val fontChoice: SubtitleFontChoice = SubtitleFontChoice.PLUS_JAKARTA,
    val fontSizeSp: Float = 20f,
    val textColorChoice: SubtitleColorChoice = SubtitleColorChoice.PURE_WHITE,
    val outlineWidthDp: Float = 2.0f,
    val backgroundOpacity: Float = 0.55f,
    val syncOffsetMs: Long = 0L,
    val externalSubtitleName: String? = null
)

@Immutable
data class SubtitleCueItem(
    val startTimeMs: Long,
    val endTimeMs: Long,
    val text: String
)

@Immutable
data class SubtitleTrackInfo(
    val id: String,
    val label: String,
    val language: String,
    val format: String, // .srt, .vtt, .ass, .ssa, .sub
    val isExternal: Boolean = false
)

enum class AudioChannelMode(val label: String, val badge: String) {
    STEREO("Stereo 2.0", "2.0 CH"),
    SURROUND_VIRTUALIZER("3D Spatial Dragon", "7.1 VIRT"),
    DOWNMIX_5_1("5.1 Night Downmix", "5.1 -> 2.0"),
    LEFT_MONO("Left Channel Mono", "L-MONO"),
    RIGHT_MONO("Right Channel Mono", "R-MONO")
}

enum class EqualizerPreset(
    val label: String,
    val bandGainsDb: List<Float> // 60Hz, 230Hz, 910Hz, 3.6kHz, 14kHz (-12dB..+12dB)
) {
    FLAT("Flat Studio", listOf(0f, 0f, 0f, 0f, 0f)),
    DRAGON_CINEMA("Dragon Cinema", listOf(6.5f, 4.0f, -1.0f, 3.5f, 5.5f)),
    BASS_INFERNO("Bass Inferno", listOf(9.5f, 7.0f, 1.0f, 0.5f, 2.0f)),
    VOCAL_CLARITY("Vocal Clarity", listOf(-2.5f, 1.5f, 6.5f, 5.0f, 2.0f)),
    NIGHT_MODE("Late Night Compress", listOf(-4.5f, -2.0f, 4.0f, 3.0f, -1.5f)),
    TREBLE_SPARK("Crystal Treble", listOf(0f, 1.0f, 2.5f, 6.0f, 8.5f))
}

@Immutable
data class AudioTrackInfo(
    val id: String,
    val label: String,
    val codec: String,
    val channels: String,
    val sampleRate: String,
    val language: String
)

@Immutable
data class AudioEngineConfig(
    val selectedTrackId: String = "audio_main",
    val channelMode: AudioChannelMode = AudioChannelMode.STEREO,
    val volumeBoostPercent: Int = 100, // 0% to 200%
    val audioDelayMs: Long = 0L, // -2000ms to +2000ms
    val equalizerPreset: EqualizerPreset = EqualizerPreset.DRAGON_CINEMA,
    val customBandGainsDb: List<Float> = EqualizerPreset.DRAGON_CINEMA.bandGainsDb,
    val bassBoostStrength: Int = 450 // 0 to 1000
)

@Immutable
data class VideoFolderSummary(
    val folderName: String,
    val folderPath: String,
    val videoCount: Int,
    val totalSizeBytes: Long,
    val totalDurationMs: Long,
    val previewVideos: List<String>,
    val leadVideo: com.example.data.local.VideoEntity? = null
)

@Immutable
data class CodecCapabilitySpec(
    val category: String,
    val name: String,
    val engine: String,
    val details: String,
    val isHardwareAccelerated: Boolean
)
