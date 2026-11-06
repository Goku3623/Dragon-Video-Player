package com.example.player

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.util.Log
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import com.example.domain.model.AudioChannelMode
import com.example.domain.model.AudioEngineConfig

/**
 * Manages real hardware/DSP audio effects bound to ExoPlayer's audioSessionId:
 * - LoudnessEnhancer for 100%..200% volume boost amplification
 * - 5-band Equalizer presets
 * - BassBoost engine
 * - Virtualizer for 3D Spatial Dragon surround mode
 */
class DragonAudioProcessorManager {

    private var currentSessionId: Int = C.AUDIO_SESSION_ID_UNSET
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null

    fun attachToPlayer(player: ExoPlayer, config: AudioEngineConfig) {
        val sessionId = player.audioSessionId
        if (sessionId != C.AUDIO_SESSION_ID_UNSET && sessionId != 0 && sessionId != currentSessionId) {
            releaseEffects()
            currentSessionId = sessionId
            try {
                loudnessEnhancer = LoudnessEnhancer(sessionId).apply { enabled = true }
            } catch (e: Exception) {
                Log.w("DragonAudio", "LoudnessEnhancer unavailable: ${e.message}")
            }
            try {
                equalizer = Equalizer(0, sessionId).apply { enabled = true }
            } catch (e: Exception) {
                Log.w("DragonAudio", "Equalizer unavailable: ${e.message}")
            }
            try {
                bassBoost = BassBoost(0, sessionId).apply { enabled = true }
            } catch (e: Exception) {
                Log.w("DragonAudio", "BassBoost unavailable: ${e.message}")
            }
            try {
                virtualizer = Virtualizer(0, sessionId).apply { enabled = true }
            } catch (e: Exception) {
                Log.w("DragonAudio", "Virtualizer unavailable: ${e.message}")
            }
        }
        applyConfig(player, config)
    }

    fun applyConfig(player: ExoPlayer, config: AudioEngineConfig) {
        // 0%..100% controls player base volume; 101%..200% uses LoudnessEnhancer up to +1500mB (+15dB)
        val baseVolume = (config.volumeBoostPercent.coerceIn(0, 100) / 100f)
        player.volume = if (config.volumeBoostPercent >= 100) 1.0f else baseVolume

        try {
            loudnessEnhancer?.let { enhancer ->
                if (config.volumeBoostPercent > 100) {
                    val boostRatio = (config.volumeBoostPercent - 100) / 100f
                    val gainMb = (boostRatio * 1500).toInt() // up to 1500 millibels
                    enhancer.setTargetGain(gainMb)
                    enhancer.enabled = true
                } else {
                    enhancer.setTargetGain(0)
                }
            }
        } catch (_: Exception) {
        }

        // Apply Equalizer band gains
        try {
            equalizer?.let { eq ->
                val numBands = eq.numberOfBands.toInt()
                val range = eq.bandLevelRange
                val minLevel = range.getOrElse(0) { -1500 }
                val maxLevel = range.getOrElse(1) { 1500 }
                val gains = config.customBandGainsDb
                for (band in 0 until minOf(numBands, gains.size)) {
                    val targetMb = (gains[band] * 100f).toInt().coerceIn(minLevel.toInt(), maxLevel.toInt()).toShort()
                    eq.setBandLevel(band.toShort(), targetMb)
                }
            }
        } catch (_: Exception) {
        }

        // Apply BassBoost (0..1000)
        try {
            bassBoost?.let { bb ->
                if (bb.strengthSupported) {
                    bb.setStrength(config.bassBoostStrength.coerceIn(0, 1000).toShort())
                }
            }
        } catch (_: Exception) {
        }

        // Apply Virtualizer for Surround mode
        try {
            virtualizer?.let { virt ->
                if (virt.strengthSupported) {
                    val strength = when (config.channelMode) {
                        AudioChannelMode.SURROUND_VIRTUALIZER -> 900
                        AudioChannelMode.DOWNMIX_5_1 -> 450
                        else -> 0
                    }
                    virt.setStrength(strength.toShort())
                }
            }
        } catch (_: Exception) {
        }
    }

    fun releaseEffects() {
        runCatching { loudnessEnhancer?.release() }
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        runCatching { virtualizer?.release() }
        loudnessEnhancer = null
        equalizer = null
        bassBoost = null
        virtualizer = null
        currentSessionId = C.AUDIO_SESSION_ID_UNSET
    }
}
