package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "videos")
data class VideoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val uri: String,
    val folderName: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val dateAddedEpochSec: Long,
    val width: Int = 1920,
    val height: Int = 1080,
    val containerFormat: String = "MKV",
    val videoCodec: String = "HEVC (H.265)",
    val audioCodec: String = "Dolby AC3 / Opus",
    val frameRate: Float = 60f,
    val bitrateKbps: Int = 18500,
    val lastPositionMs: Long = 0L,
    val isFavorite: Boolean = false,
    val isVaultHidden: Boolean = false,
    val isTrashed: Boolean = false,
    val trashedAtEpochSec: Long = 0L,
    val isNetworkStream: Boolean = false,
    val streamProtocol: String = "LOCAL", // LOCAL, HLS, DASH, RTSP, HTTP, SS
    val externalSubtitleUri: String? = null,
    val externalSubtitleFormat: String? = null
) {
    val resolutionBadge: String
        get() = when {
            width >= 3840 || height >= 2160 -> "4K UHD"
            width >= 2560 || height >= 1440 -> "2K QHD"
            width >= 1920 || height >= 1080 -> "1080p FHD"
            width >= 1280 || height >= 720 -> "720p HD"
            else -> "SD"
        }

    val resumeProgressFraction: Float
        get() = if (durationMs > 0L) (lastPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
}
