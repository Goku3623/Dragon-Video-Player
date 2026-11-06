package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.player.SubtitleParserEngine
import com.example.ui.theme.DragonThemeConfig
import com.example.ui.theme.DragonThemePreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Dragon Video Player", appName)
    }

    @Test
    fun `parse srt and ass subtitle tracks and resolve sync offset`() {
        val sampleSrt = """
            1
            00:00:01,000 --> 00:00:04,500
            Across the obsidian skyline, Dragon fire awakens.
        """.trimIndent()

        val parsedSrt = SubtitleParserEngine.parseRawSubtitleText(sampleSrt)
        assertEquals(1, parsedSrt.size)
        assertEquals(1000L, parsedSrt.first().startTimeMs)
        assertEquals(4500L, parsedSrt.first().endTimeMs)

        val resolved = SubtitleParserEngine.resolveCueForPosition(
            positionMs = 3000L,
            syncOffsetMs = 500L,
            trackId = "external_custom",
            videoTitle = "Test",
            externalCues = parsedSrt
        )
        assertNotNull(resolved)
        assertTrue(resolved!!.contains("Dragon fire"))
    }

    @Test
    fun `verify dragon theme presets and custom accent override`() {
        val flameConfig = DragonThemeConfig(preset = DragonThemePreset.DRAGON_FLAME)
        assertEquals(DragonThemePreset.DRAGON_FLAME.primaryAccent, flameConfig.activePrimary)

        val customConfig = DragonThemeConfig(
            preset = DragonThemePreset.ABYSSAL_DRAKE,
            useCustomAccent = true,
            customAccentHue = 140f
        )
        assertTrue(customConfig.activePrimary != DragonThemePreset.ABYSSAL_DRAKE.primaryAccent)
    }

    @Test
    fun `parse m3u playlist channels and variant qualities`() {
        val sampleM3u = """
            #EXTM3U
            #EXTINF:-1 tvg-id="Dragon1" group-title="Sports",Dragon Sports HD
            https://example.com/live/sports.m3u8
            #EXTINF:-1 tvg-id="Dragon2" group-title="Cinema",Dragon Movies 4K
            https://example.com/live/movies.m3u8
            #EXT-X-STREAM-INF:BANDWIDTH=4500000,RESOLUTION=1920x1080,NAME="1080p FHD"
            https://example.com/live/1080p.m3u8
        """.trimIndent()

        val parsed = com.example.player.DragonStreamPlaylistHelper.parseM3uOrMultipleUrls(sampleM3u)
        assertEquals(3, parsed.size)
        assertEquals("Dragon Sports HD", parsed[0].title)
        assertEquals("Sports", parsed[0].group)
        assertEquals("Dragon Movies 4K", parsed[1].title)
        assertEquals("1080p FHD", parsed[2].title)
    }
}
