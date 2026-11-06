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
@Config(sdk = [36])
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
}
