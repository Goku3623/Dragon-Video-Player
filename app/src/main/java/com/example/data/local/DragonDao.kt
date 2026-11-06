package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DragonDao {
    @Query("SELECT * FROM videos WHERE isVaultHidden = 0 AND isTrashed = 0 ORDER BY dateAddedEpochSec DESC")
    fun observePublicVideos(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE isVaultHidden = 1 AND isTrashed = 0 ORDER BY dateAddedEpochSec DESC")
    fun observeVaultVideos(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE isTrashed = 1 ORDER BY trashedAtEpochSec DESC")
    fun observeTrashedVideos(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos ORDER BY dateAddedEpochSec DESC")
    suspend fun getAllVideosOnce(): List<VideoEntity>

    @Query("SELECT * FROM videos WHERE uri = :uri LIMIT 1")
    suspend fun findVideoByUri(uri: String): VideoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: VideoEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideos(videos: List<VideoEntity>)

    @Update
    suspend fun updateVideo(video: VideoEntity)

    @Query("UPDATE videos SET lastPositionMs = :positionMs WHERE id = :videoId")
    suspend fun updateResumePosition(videoId: Long, positionMs: Long)

    @Query("UPDATE videos SET isFavorite = :isFavorite WHERE id = :videoId")
    suspend fun updateFavorite(videoId: Long, isFavorite: Boolean)

    @Query("UPDATE videos SET isVaultHidden = :isHidden WHERE id = :videoId")
    suspend fun updateVaultStatus(videoId: Long, isHidden: Boolean)

    @Query("UPDATE videos SET isTrashed = 1, trashedAtEpochSec = :trashedAt WHERE id = :videoId")
    suspend fun moveToTrash(videoId: Long, trashedAt: Long)

    @Query("UPDATE videos SET isTrashed = 0, trashedAtEpochSec = 0 WHERE id = :videoId")
    suspend fun restoreFromTrash(videoId: Long)

    @Query("UPDATE videos SET isTrashed = 0, trashedAtEpochSec = 0 WHERE isTrashed = 1")
    suspend fun restoreAllTrashedVideos()

    @Query("UPDATE videos SET externalSubtitleUri = :subUri, externalSubtitleFormat = :format WHERE id = :videoId")
    suspend fun updateExternalSubtitle(videoId: Long, subUri: String?, format: String?)

    @Query("DELETE FROM videos WHERE id = :videoId")
    suspend fun deleteVideoById(videoId: Long)

    @Query("DELETE FROM videos WHERE isTrashed = 1")
    suspend fun deleteAllTrashedVideos()

    @Query("SELECT * FROM playlists ORDER BY createdAtEpochSec DESC")
    fun observePlaylists(): Flow<List<PlaylistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Update
    suspend fun updatePlaylist(playlist: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylistById(playlistId: Long)
}
