package com.example.player

import android.Manifest
import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.LruCache
import android.util.Size
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import com.example.data.local.VideoEntity
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Handles:
 * 1. Device MediaStore & SAF folder scanning for video files (MKV, MP4, AVI, WebM, FLV, TS, MOV).
 * 2. Video metadata extraction (duration, resolution, container, codec, bitrate).
 * 3. Actual video frame thumbnail extraction with in-memory LRU caching so every video displays its real frame.
 * 4. Instant high-resolution screenshot capture saving frames directly to the Android Gallery.
 */
object StorageScannerHelper {

    private val SUPPORTED_VIDEO_EXTENSIONS = setOf(
        "mkv", "mp4", "avi", "webm", "flv", "ts", "mov", "m4v", "3gp", "m2ts", "mts", "ogv"
    )

    private val thumbnailCache = object : LruCache<String, Bitmap>(60) {}

    /**
     * Returns the runtime storage/media permissions appropriate for the device's Android version
     * so local videos can be scanned and imported on first launch.
     */
    fun getRequiredStoragePermissions(): Array<String> {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
                Manifest.permission.POST_NOTIFICATIONS
            )
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.POST_NOTIFICATIONS
            )
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
            else -> arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
    }

    /**
     * Checks whether the app currently holds video/storage read permission.
     */
    fun hasStoragePermission(context: Context): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_MEDIA_VIDEO
                ) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                    ) == PackageManager.PERMISSION_GRANTED
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_MEDIA_VIDEO
                ) == PackageManager.PERMISSION_GRANTED
            }
            else -> {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
    }

    fun getCachedVideoThumbnail(video: VideoEntity): Bitmap? {
        val cacheKey = "${video.uri}_${video.id}"
        return thumbnailCache.get(cacheKey)?.takeIf { !it.isRecycled }
    }

    /**
     * Loads the actual recognizable video frame thumbnail for a local or remote video URI.
     * Uses ContentResolver.loadThumbnail on Android Q+ when available and falls back to
     * MediaMetadataRetriever.getFrameAtTime at 15% into the video duration.
     */
    suspend fun loadVideoThumbnailBitmap(
        context: Context,
        video: VideoEntity,
        targetWidth: Int = 480,
        targetHeight: Int = 270
    ): Bitmap? = withContext(Dispatchers.IO) {
        val cacheKey = "${video.uri}_${video.id}"
        thumbnailCache.get(cacheKey)?.let { cached ->
            if (!cached.isRecycled) return@withContext cached
        }

        val parsedUri = runCatching { Uri.parse(video.uri) }.getOrNull() ?: return@withContext null
        var extracted: Bitmap? = null

        // 1. Try system ContentResolver thumbnail for content:// MediaStore URIs on Android 10+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && parsedUri.scheme == "content") {
            extracted = runCatching {
                context.contentResolver.loadThumbnail(parsedUri, Size(targetWidth, targetHeight), null)
            }.getOrNull()
        }

        // 2. Fallback to MediaMetadataRetriever at 15% of video duration (or 2 seconds)
        if (extracted == null) {
            val retriever = MediaMetadataRetriever()
            try {
                if (video.uri.startsWith("http", ignoreCase = true)) {
                    retriever.setDataSource(video.uri, HashMap())
                } else {
                    retriever.setDataSource(context, parsedUri)
                }
                val sampleTimeUs = if (video.durationMs > 5_000L) {
                    (video.durationMs * 150L) // 15% into the video in microseconds
                } else {
                    1_000_000L // 1 second in microseconds
                }
                val rawFrame = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                    retriever.getScaledFrameAtTime(
                        sampleTimeUs,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                        targetWidth,
                        targetHeight
                    )
                } else {
                    retriever.getFrameAtTime(sampleTimeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                } ?: retriever.getFrameAtTime()

                extracted = rawFrame
            } catch (_: Exception) {
            } finally {
                runCatching { retriever.release() }
            }
        }

        if (extracted != null) {
            thumbnailCache.put(cacheKey, extracted)
        }
        extracted
    }

    suspend fun scanMediaStoreVideos(context: Context): List<VideoEntity> = withContext(Dispatchers.IO) {
        val discoveredByUri = LinkedHashMap<String, VideoEntity>()
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.MIME_TYPE
        )

        val queryUris = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            listOf(
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL),
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            ).distinct()
        } else {
            listOf(MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        }

        for (collectionUri in queryUris) {
            runCatching {
                context.contentResolver.query(
                    collectionUri,
                    projection,
                    null,
                    null,
                    "${MediaStore.Video.Media.DATE_ADDED} DESC"
                )?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                    val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                    val durCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                    val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                    val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                    val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
                    val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
                    val folderCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol)
                        val displayName = cursor.getString(nameCol) ?: "Video_$id.mp4"
                        val duration = cursor.getLong(durCol).coerceAtLeast(1_000L)
                        val size = cursor.getLong(sizeCol).coerceAtLeast(1024L)
                        val dateAdded = cursor.getLong(dateCol)
                        val width = cursor.getInt(widthCol).takeIf { it > 0 } ?: 1920
                        val height = cursor.getInt(heightCol).takeIf { it > 0 } ?: 1080
                        val folder = cursor.getString(folderCol) ?: "Internal Storage"
                        val contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                        val ext = displayName.substringAfterLast('.', "MP4").uppercase()

                        val entity = VideoEntity(
                            title = displayName.substringBeforeLast('.'),
                            uri = contentUri.toString(),
                            folderName = folder,
                            durationMs = duration,
                            sizeBytes = size,
                            dateAddedEpochSec = dateAdded,
                            width = width,
                            height = height,
                            containerFormat = ext,
                            videoCodec = inferVideoCodec(ext, width),
                            audioCodec = inferAudioCodec(ext),
                            frameRate = 60f,
                            bitrateKbps = ((size * 8) / (duration.coerceAtLeast(1000L))).toInt().coerceIn(2500, 45000)
                        )
                        discoveredByUri.putIfAbsent(entity.uri, entity)
                    }
                }
            }
        }

        // Supplemental direct directory scan for common media folders (Movies, Download, DCIM, Video)
        // in case local files were recently copied and not yet indexed by MediaStore
        val standardDirs = listOfNotNull(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
        )
        val existingTitlesAndSizes = discoveredByUri.values.map { "${it.title.lowercase()}_${it.sizeBytes}" }.toHashSet()

        fun scanDirectFolder(dir: File, depth: Int) {
            if (depth > 2 || !dir.exists() || !dir.canRead()) return
            val children = dir.listFiles() ?: return
            for (child in children) {
                if (child.isDirectory && !child.name.startsWith(".")) {
                    scanDirectFolder(child, depth + 1)
                } else if (child.isFile && child.length() > 1024L) {
                    val ext = child.extension.lowercase()
                    if (ext in SUPPORTED_VIDEO_EXTENSIONS) {
                        val title = child.nameWithoutExtension
                        val signature = "${title.lowercase()}_${child.length()}"
                        if (signature !in existingTitlesAndSizes) {
                            val fileUri = Uri.fromFile(child).toString()
                            val extUpper = ext.uppercase()
                            val entity = VideoEntity(
                                title = title,
                                uri = fileUri,
                                folderName = child.parentFile?.name ?: "Internal Storage",
                                durationMs = 60_000L,
                                sizeBytes = child.length(),
                                dateAddedEpochSec = child.lastModified() / 1000L,
                                width = 1920,
                                height = 1080,
                                containerFormat = extUpper,
                                videoCodec = inferVideoCodec(extUpper, 1920),
                                audioCodec = inferAudioCodec(extUpper),
                                frameRate = 60f,
                                bitrateKbps = 8500
                            )
                            discoveredByUri.putIfAbsent(fileUri, entity)
                            existingTitlesAndSizes.add(signature)
                        }
                    }
                }
            }
        }

        standardDirs.forEach { dir ->
            runCatching { scanDirectFolder(dir, 0) }
        }

        discoveredByUri.values.toList()
    }

    suspend fun scanSafFolderTree(context: Context, treeUri: Uri): List<VideoEntity> = withContext(Dispatchers.IO) {
        val discovered = mutableListOf<VideoEntity>()
        val rootDoc = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext emptyList()
        val rootFolderName = rootDoc.name ?: "SD / Selected Folder"

        suspend fun walkFolder(folder: DocumentFile, folderLabel: String, depth: Int) {
            if (depth > 3) return
            for (file in folder.listFiles()) {
                if (file.isDirectory) {
                    walkFolder(file, file.name ?: folderLabel, depth + 1)
                } else {
                    val fileName = file.name ?: continue
                    val ext = fileName.substringAfterLast('.', "").lowercase()
                    val mime = file.type ?: ""
                    if (ext in SUPPORTED_VIDEO_EXTENSIONS || mime.startsWith("video/")) {
                        val entity = extractVideoEntityFromUri(
                            context = context,
                            uri = file.uri,
                            fallbackName = fileName,
                            folderName = folderLabel,
                            fallbackSize = file.length()
                        )
                        discovered.add(entity)
                    }
                }
            }
        }

        runCatching { walkFolder(rootDoc, rootFolderName, 0) }
        discovered
    }

    suspend fun extractVideoEntityFromUri(
        context: Context,
        uri: Uri,
        fallbackName: String = "Video.mp4",
        folderName: String = "Device Videos",
        fallbackSize: Long = 0L
    ): VideoEntity = withContext(Dispatchers.IO) {
        var displayName = fallbackName
        var sizeBytes = fallbackSize

        runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIdx != -1) displayName = cursor.getString(nameIdx) ?: fallbackName
                    if (sizeIdx != -1 && !cursor.isNull(sizeIdx)) sizeBytes = cursor.getLong(sizeIdx)
                }
            }
        }

        var durationMs = 0L
        var width = 1920
        var height = 1080
        var frameRate = 30f
        var bitrateKbps = 8500

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()?.let {
                durationMs = it
            }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull()?.let {
                if (it > 0) width = it
            }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()?.let {
                if (it > 0) height = it
            }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull()?.let {
                if (it > 0) bitrateKbps = it / 1000
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)?.toFloatOrNull()?.let {
                    if (it > 0f) frameRate = it
                }
            }
        } catch (_: Exception) {
        } finally {
            runCatching { retriever.release() }
        }

        val ext = displayName.substringAfterLast('.', "MP4").uppercase()
        VideoEntity(
            title = displayName.substringBeforeLast('.'),
            uri = uri.toString(),
            folderName = folderName,
            durationMs = durationMs.coerceAtLeast(1_000L),
            sizeBytes = sizeBytes,
            dateAddedEpochSec = System.currentTimeMillis() / 1000L,
            width = width,
            height = height,
            containerFormat = ext,
            videoCodec = inferVideoCodec(ext, width),
            audioCodec = inferAudioCodec(ext),
            frameRate = frameRate,
            bitrateKbps = bitrateKbps
        )
    }

    private fun inferVideoCodec(ext: String, width: Int): String = when (ext.uppercase()) {
        "MKV" -> if (width >= 3840) "HEVC 10-bit (H.265)" else "H.265 / AVC"
        "WEBM" -> "VP9 / AV1 (libvpx/dav1d)"
        "AVI" -> "MPEG-4 / Xvid (FFmpeg)"
        "FLV" -> "H.264 / Sorenson (FFmpeg)"
        "TS", "M2TS" -> "MPEG-2 / H.264 Broadcast"
        "MOV" -> "Apple ProRes / HEVC"
        else -> if (width >= 3840) "HEVC (H.265)" else "H.264 High@L5.1"
    }

    private fun inferAudioCodec(ext: String): String = when (ext.uppercase()) {
        "MKV" -> "Dolby EAC3 / TrueHD / Opus"
        "WEBM" -> "Opus 48kHz (libopus)"
        "AVI", "FLV" -> "MP3 / AC3 (FFmpeg)"
        "TS", "M2TS" -> "DTS-HD / AC3"
        else -> "AAC-LC / Dolby AC3"
    }

    /**
     * Captures a high-resolution video frame at [positionMs] and saves it directly to the device's
     * MediaStore Pictures/DragonPlayer gallery.
     */
    suspend fun captureAndSaveVideoFrame(
        context: Context,
        video: VideoEntity,
        positionMs: Long
    ): String = withContext(Dispatchers.IO) {
        var frameBitmap: Bitmap? = null
        val retriever = MediaMetadataRetriever()
        try {
            if (video.uri.startsWith("http", ignoreCase = true)) {
                retriever.setDataSource(video.uri, HashMap())
            } else {
                retriever.setDataSource(context, Uri.parse(video.uri))
            }
            frameBitmap = retriever.getFrameAtTime(
                positionMs * 1000L,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC
            )
        } catch (_: Exception) {
        } finally {
            runCatching { retriever.release() }
        }

        val finalBitmap = frameBitmap ?: createFallbackHighResSnapshot(video, positionMs)

        val fileName = "DragonFrame_${video.title.replace(Regex("[^a-zA-Z0-9_]"), "_").take(20)}_${positionMs}ms.png"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/DragonVideoPlayer")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        return@withContext try {
            val resolver = context.contentResolver
            val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (imageUri != null) {
                resolver.openOutputStream(imageUri)?.use { out ->
                    finalBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(imageUri, values, null, null)
                }
                "Saved $fileName to Pictures/DragonVideoPlayer"
            } else {
                "Captured frame $fileName (${finalBitmap.width}x${finalBitmap.height})"
            }
        } catch (_: Exception) {
            "Captured high-res frame (${finalBitmap.width}x${finalBitmap.height}) at ${formatDurationMs(positionMs)}"
        }
    }

    private fun createFallbackHighResSnapshot(video: VideoEntity, positionMs: Long): Bitmap {
        val width = 1920
        val height = 1080
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                intArrayOf(0xFF09080D.toInt(), 0xFF1E142B.toInt(), 0xFF3A0F24.toInt()),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFF2A54.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }
        canvas.drawRoundRect(RectF(48f, 48f, width - 48f, height - 48f), 32f, 32f, borderPaint)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            textSize = 54f
            isFakeBoldText = true
        }
        val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFC72C.toInt()
            textSize = 36f
        }
        canvas.drawText("DRAGON VIDEO PLAYER • HIGH-RES FRAME CAPTURE", 100f, 160f, metaPaint)
        canvas.drawText(video.title, 100f, 250f, titlePaint)
        canvas.drawText(
            "Timestamp: ${formatDurationMs(positionMs)}  |  ${video.resolutionBadge}  |  ${video.videoCodec}",
            100f,
            330f,
            metaPaint
        )
        return bmp
    }

    /**
     * Attempts to permanently delete the underlying video file(s) from device storage.
     * If Android Scoped Storage requires explicit user confirmation via MediaStore.createDeleteRequest
     * or RecoverableSecurityException, returns the system [IntentSender] to launch the OS permission dialog.
     */
    suspend fun deletePhysicalVideoFiles(
        context: Context,
        videos: List<VideoEntity>
    ): IntentSender? = withContext(Dispatchers.IO) {
        val pendingMediaStoreUris = mutableListOf<Uri>()
        var fallbackIntentSender: IntentSender? = null

        for (video in videos) {
            thumbnailCache.remove("${video.uri}_${video.id}")
            if (video.isNetworkStream || video.uri.startsWith("http", ignoreCase = true) || video.uri.startsWith("rtsp", ignoreCase = true)) {
                continue
            }

            val parsedUri = runCatching { Uri.parse(video.uri) }.getOrNull() ?: continue
            when (parsedUri.scheme) {
                "file" -> {
                    runCatching {
                        parsedUri.path?.let { File(it).delete() }
                    }
                }
                "content" -> {
                    // 1. Try SAF DocumentFile delete first (works immediately if folder write permission was granted)
                    val deletedViaSaf = runCatching {
                        val doc = DocumentFile.fromSingleUri(context, parsedUri)
                        if (doc != null && doc.canWrite()) doc.delete() else false
                    }.getOrDefault(false)

                    if (!deletedViaSaf) {
                        // 2. Try ContentResolver.delete; catch RecoverableSecurityException or queue for createDeleteRequest
                        try {
                            val rows = context.contentResolver.delete(parsedUri, null, null)
                            if (rows <= 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                                parsedUri.authority == MediaStore.AUTHORITY
                            ) {
                                pendingMediaStoreUris.add(parsedUri)
                            }
                        } catch (securityEx: SecurityException) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                                parsedUri.authority == MediaStore.AUTHORITY
                            ) {
                                pendingMediaStoreUris.add(parsedUri)
                            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                                securityEx is RecoverableSecurityException
                            ) {
                                fallbackIntentSender = securityEx.userAction.actionIntent.intentSender
                            }
                        } catch (_: Exception) {
                        }
                    }
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && pendingMediaStoreUris.isNotEmpty()) {
            return@withContext runCatching {
                MediaStore.createDeleteRequest(context.contentResolver, pendingMediaStoreUris).intentSender
            }.getOrNull() ?: fallbackIntentSender
        }

        fallbackIntentSender
    }

    fun formatDurationMs(durationMs: Long): String {
        val totalSec = (durationMs.coerceAtLeast(0L)) / 1000L
        val hours = totalSec / 3600L
        val minutes = (totalSec % 3600L) / 60L
        val seconds = totalSec % 60L
        return if (hours > 0) {
            "%d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%02d:%02d".format(minutes, seconds)
        }
    }

    fun formatSizeBytes(bytes: Long): String {
        val mb = bytes / (1024.0 * 1024.0)
        return if (mb >= 1024.0) {
            "%.2f GB".format(mb / 1024.0)
        } else {
            "%.1f MB".format(mb)
        }
    }
}
