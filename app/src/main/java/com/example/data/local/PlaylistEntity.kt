package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val description: String,
    val createdAtEpochSec: Long = System.currentTimeMillis() / 1000L,
    val videoIdsCsv: String = "", // Comma-separated video IDs
    val accentHex: String = "#FF2A54"
) {
    fun getVideoIdList(): List<Long> =
        videoIdsCsv.split(",")
            .mapNotNull { it.trim().toLongOrNull() }
}
