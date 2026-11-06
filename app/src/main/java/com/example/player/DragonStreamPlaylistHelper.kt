package com.example.player

import android.net.Uri
import com.example.data.local.VideoEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

/**
 * Robust parser & remote fetcher for M3U / M3U8 multi-stream playlists,
 * IPTV channel lists, HLS master variant playlists, and multi-link inputs.
 */
object DragonStreamPlaylistHelper {

    const val STREAM_USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36 DragonPlayer/2.5"

    data class ParsedStreamItem(
        val title: String,
        val url: String,
        val group: String? = null,
        val logoUrl: String? = null,
        val resolution: String? = null,
        val bandwidth: Long? = null,
        val protocol: String = "HLS"
    )

    fun ParsedStreamItem.toVideoEntity(id: Long = System.nanoTime()): VideoEntity {
        val widthHeight = resolution?.split("x")?.let { parts ->
            if (parts.size == 2) {
                (parts[0].toIntOrNull() ?: 1920) to (parts[1].toIntOrNull() ?: 1080)
            } else 1920 to 1080
        } ?: (1920 to 1080)

        val cleanProto = when {
            url.contains(".m3u8", ignoreCase = true) || url.contains("m3u8", ignoreCase = true) -> "HLS (.M3U8)"
            url.contains(".mpd", ignoreCase = true) -> "DASH (.MPD)"
            url.startsWith("rtsp://", ignoreCase = true) -> "RTSP"
            else -> protocol.uppercase()
        }

        return VideoEntity(
            id = id,
            title = title,
            uri = url,
            folderName = group?.ifBlank { "Stream Playlist" } ?: "Stream Playlist",
            durationMs = 1800_000L,
            sizeBytes = 0L,
            dateAddedEpochSec = System.currentTimeMillis() / 1000L,
            width = widthHeight.first,
            height = widthHeight.second,
            containerFormat = cleanProto,
            videoCodec = if (resolution != null) "HLS $resolution" else "Adaptive Stream",
            audioCodec = "AAC / EAC3 / Opus",
            frameRate = 60f,
            bitrateKbps = ((bandwidth ?: 5000000L) / 1000L).toInt(),
            isNetworkStream = true,
            streamProtocol = cleanProto
        )
    }

    /**
     * Parse text content which may contain:
     * 1) #EXTM3U IPTV playlist with #EXTINF lines
     * 2) #EXT-X-STREAM-INF HLS master playlist variants
     * 3) Multi-line URLs (pasted links)
     */
    fun parseM3uOrMultipleUrls(
        content: String,
        baseUrl: String? = null,
        defaultTitle: String = "Stream Channel"
    ): List<ParsedStreamItem> {
        val lines = content.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val items = mutableListOf<ParsedStreamItem>()

        var pendingTitle: String? = null
        var pendingGroup: String? = null
        var pendingLogo: String? = null
        var pendingResolution: String? = null
        var pendingBandwidth: Long? = null

        for (line in lines) {
            when {
                line.startsWith("#EXTM3U", ignoreCase = true) -> {
                    // Header, continue
                }
                line.startsWith("#EXTINF:", ignoreCase = true) -> {
                    // Example: #EXTINF:-1 tvg-id="CNN" tvg-logo="https://..." group-title="News",CNN International HD
                    val commaIndex = line.lastIndexOf(',')
                    val metaPart = if (commaIndex != -1) line.substring(8, commaIndex) else line.substring(8)
                    val titlePart = if (commaIndex != -1) line.substring(commaIndex + 1).trim() else ""

                    if (titlePart.isNotBlank()) {
                        pendingTitle = titlePart
                    }

                    // Extract tvg-logo
                    val logoMatch = Regex("""tvg-logo=["']([^"']+)["']""").find(metaPart)
                    pendingLogo = logoMatch?.groupValues?.getOrNull(1)

                    // Extract group-title
                    val groupMatch = Regex("""group-title=["']([^"']+)["']""").find(metaPart)
                    pendingGroup = groupMatch?.groupValues?.getOrNull(1)
                }
                line.startsWith("#EXT-X-STREAM-INF:", ignoreCase = true) -> {
                    // Example: #EXT-X-STREAM-INF:BANDWIDTH=4500000,RESOLUTION=1920x1080,NAME="1080p FHD"
                    val bwMatch = Regex("""BANDWIDTH=(\d+)""").find(line)
                    pendingBandwidth = bwMatch?.groupValues?.getOrNull(1)?.toLongOrNull()

                    val resMatch = Regex("""RESOLUTION=(\d+x\d+)""").find(line)
                    pendingResolution = resMatch?.groupValues?.getOrNull(1)

                    val nameMatch = Regex("""NAME=["']([^"']+)["']""").find(line)
                    val rawName = nameMatch?.groupValues?.getOrNull(1)
                    pendingTitle = rawName ?: pendingResolution?.let { "Quality: $it" }
                }
                !line.startsWith("#") -> {
                    // This is a candidate URL line
                    val resolvedUrl = resolveUrl(baseUrl, line)
                    if (isValidStreamUrl(resolvedUrl)) {
                        // Skip if it looks like a single sub-second media chunk (e.g. segment_100.ts) unless it's a full stream
                        val isSingleTsChunk = resolvedUrl.endsWith(".ts", ignoreCase = true) &&
                            (line.contains("segment", ignoreCase = true) || line.contains("chunk", ignoreCase = true))

                        if (!isSingleTsChunk) {
                            val finalTitle = pendingTitle
                                ?: pendingResolution?.let { "Quality ($it)" }
                                ?: (if (items.isEmpty()) defaultTitle else "$defaultTitle #${items.size + 1}")

                            val protocol = when {
                                resolvedUrl.contains(".m3u8", ignoreCase = true) -> "HLS"
                                resolvedUrl.contains(".mpd", ignoreCase = true) -> "DASH"
                                resolvedUrl.startsWith("rtsp://", ignoreCase = true) -> "RTSP"
                                else -> "HTTP"
                            }

                            items.add(
                                ParsedStreamItem(
                                    title = finalTitle,
                                    url = resolvedUrl,
                                    group = pendingGroup,
                                    logoUrl = pendingLogo,
                                    resolution = pendingResolution,
                                    bandwidth = pendingBandwidth,
                                    protocol = protocol
                                )
                            )
                        }
                    }
                    // Reset pending metadata for the next item
                    pendingTitle = null
                    pendingGroup = null
                    pendingLogo = null
                    pendingResolution = null
                    pendingBandwidth = null
                }
            }
        }

        // If no structured playlist tags were found, check if content contains multiple plain URLs
        if (items.isEmpty()) {
            val urlRegex = Regex("""(https?://[^\s"'<>]+|rtsp://[^\s"'<>]+)""")
            val matches = urlRegex.findAll(content).map { it.value.trim() }.distinct().toList()
            if (matches.size > 1) {
                matches.forEachIndexed { index, u ->
                    items.add(
                        ParsedStreamItem(
                            title = if (index == 0 && defaultTitle.isNotBlank()) defaultTitle else "$defaultTitle #${index + 1}",
                            url = u,
                            protocol = if (u.contains(".m3u8", ignoreCase = true)) "HLS" else "STREAM"
                        )
                    )
                }
            }
        }

        return items
    }

    /**
     * Asynchronously downloads a remote M3U / M3U8 playlist and parses any multiple channels
     * or variant streams into switchable playlist items.
     * Follows HTTP -> HTTPS cross-protocol redirects up to 6 hops.
     */
    suspend fun fetchAndParseRemoteM3u(
        url: String,
        defaultTitle: String
    ): List<ParsedStreamItem> = withContext(Dispatchers.IO) {
        runCatching {
            var currentUrl = url
            var responseCode = -1
            var connection: HttpURLConnection? = null
            var redirects = 0
            val maxRedirects = 6

            while (redirects < maxRedirects) {
                val conn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 12_000
                    readTimeout = 18_000
                    instanceFollowRedirects = false // Manually handle cross-protocol (http -> https) redirects
                    setRequestProperty("User-Agent", STREAM_USER_AGENT)
                    setRequestProperty("Accept", "*/*")
                    setRequestProperty("Accept-Encoding", "gzip, deflate")
                }
                responseCode = conn.responseCode
                if (responseCode in 300..399) {
                    val location = conn.getHeaderField("Location")
                    conn.disconnect()
                    if (!location.isNullOrBlank()) {
                        currentUrl = resolveUrl(currentUrl, location)
                        redirects++
                        continue
                    }
                }
                connection = conn
                break
            }

            if (connection != null && responseCode in 200..299) {
                val stream = if ("gzip".equals(connection.contentEncoding, ignoreCase = true)) {
                    java.util.zip.GZIPInputStream(connection.inputStream)
                } else {
                    connection.inputStream
                }

                val text = BufferedReader(InputStreamReader(stream)).use { it.readText() }
                connection.disconnect()

                val parsed = parseM3uOrMultipleUrls(text, baseUrl = currentUrl, defaultTitle = defaultTitle)
                parsed
            } else {
                connection?.disconnect()
                emptyList()
            }
        }.getOrDefault(emptyList())
    }

    private fun resolveUrl(baseUrl: String?, relativeOrAbsoluteUrl: String): String {
        val trimmed = relativeOrAbsoluteUrl.trim()
        if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true) ||
            trimmed.startsWith("rtsp://", ignoreCase = true)
        ) {
            return trimmed
        }
        if (baseUrl.isNullOrBlank()) return trimmed

        return runCatching {
            URL(URL(baseUrl), trimmed).toString()
        }.getOrElse {
            runCatching {
                URI(baseUrl).resolve(trimmed).toString()
            }.getOrDefault(trimmed)
        }
    }

    private fun isValidStreamUrl(url: String): Boolean {
        if (url.length < 7) return false
        val lower = url.lowercase()
        return lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("rtsp://")
    }
}
