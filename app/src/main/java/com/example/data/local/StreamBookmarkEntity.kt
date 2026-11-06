package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stream_bookmarks")
data class StreamBookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val url: String,
    val protocol: String = "HLS (.m3u8)",
    val createdAtEpochSec: Long = System.currentTimeMillis() / 1000L
)
