package com.overdrive.app.surveillance.deterrent

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import com.overdrive.app.logging.DaemonLogger
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.PI
import kotlin.math.sin

/**
 * Generates and plays acoustic security deterrent warning tones over the vehicle audio hardware.
 * Uses fully self-contained PCM synthesis via AudioTrack so no external audio files are required.
 */
class DeterrentAudioPlayer {

    private val logger = DaemonLogger.getInstance("DeterrentAudioPlayer")
    private val isPlaying = AtomicBoolean(false)
    private val executor: ExecutorService = Executors.newSingleThreadExecutor { r ->
        Thread(r, "DeterrentAudioWorker").apply { isDaemon = true }
    }

    /**
     * Plays a multi-frequency security warning pulse tone for [durationMs] milliseconds.
     */
    fun playWarningAlarm(durationMs: Int = 2500) {
        if (!isPlaying.compareAndSet(false, true)) {
            logger.debug("Deterrent audio already playing, skipping.")
            return
        }

        executor.execute {
            var track: AudioTrack? = null
            try {
                logger.info("Starting acoustic deterrent alarm playback ($durationMs ms)...")
                val sampleRate = 16000
                val totalSamples = (sampleRate * (durationMs / 1000.0)).toInt()
                val pcmBuffer = ShortArray(totalSamples)

                // Synthesize dual-tone warble (1000 Hz and 1600 Hz alternating every 150ms)
                val pulseSamples = (sampleRate * 0.15).toInt()
                for (i in 0 until totalSamples) {
                    val phase = i / pulseSamples
                    val freq = if (phase % 2 == 0) 1100.0 else 1650.0
                    val angle = 2.0 * PI * i * freq / sampleRate
                    // Apply smooth envelope to avoid clicking
                    val sample = (sin(angle) * Short.MAX_VALUE * 0.85).toInt()
                    pcmBuffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }

                val bufferSizeBytes = pcmBuffer.size * 2
                track = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    AudioTrack.Builder()
                        .setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ALARM)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build()
                        )
                        .setAudioFormat(
                            AudioFormat.Builder()
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setSampleRate(sampleRate)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                .build()
                        )
                        .setBufferSizeInBytes(bufferSizeBytes)
                        .setTransferMode(AudioTrack.MODE_STATIC)
                        .build()
                } else {
                    @Suppress("DEPRECATION")
                    AudioTrack(
                        AudioManager.STREAM_ALARM,
                        sampleRate,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSizeBytes,
                        AudioTrack.MODE_STATIC
                    )
                }

                track.write(pcmBuffer, 0, pcmBuffer.size)
                track.play()

                Thread.sleep(durationMs.toLong())
                track.stop()
                logger.info("Acoustic deterrent playback completed successfully.")
            } catch (e: Throwable) {
                logger.warn("Acoustic deterrent playback error: ${e.message}")
            } finally {
                try {
                    track?.release()
                } catch (_: Throwable) {}
                isPlaying.set(false)
            }
        }
    }

    fun isCurrentlyPlaying(): Boolean = isPlaying.get()
}
