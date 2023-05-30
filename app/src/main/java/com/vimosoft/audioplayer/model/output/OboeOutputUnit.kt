package com.vimosoft.audioplayer.model.output

import android.media.AudioFormat
import android.media.MediaFormat
import java.nio.ByteBuffer

class OboeOutputUnit : AudioOutputUnit() {
    private var audioSink: Long = 0L

    override fun configure(mediaFormat: MediaFormat) {
        val channelCount = mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        val sampleRate = mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        var isFloat = false
        val bitDepth = try {
            when (mediaFormat.getInteger(MediaFormat.KEY_PCM_ENCODING)) {
                AudioFormat.ENCODING_PCM_FLOAT -> {
                    isFloat = true
                    32
                }

                AudioFormat.ENCODING_PCM_32BIT -> 32
                AudioFormat.ENCODING_PCM_24BIT_PACKED -> 24
                AudioFormat.ENCODING_PCM_16BIT -> 16
                AudioFormat.ENCODING_PCM_8BIT -> 8
                else -> 16
            }
        } catch (e: Exception) {
            16
        }

        if (audioSink == 0L) {
            audioSink = initialize(channelCount, sampleRate, bitDepth, isFloat)
        }
    }

    override fun release() {
        if (audioSink != 0L) {
            release(audioSink)
            audioSink = 0L
        }
    }

    override fun outputAudio(outputBuffer: ByteBuffer, size: Int) {
        if (audioSink != 0L) {
            requestPlayback(audioSink, outputBuffer)
        }
    }

    private external fun initialize(
        channelCount: Int,
        sampleRate: Int,
        bitDepth: Int,
        isFloat: Boolean
    ): Long

    private external fun release(audioSink: Long)
    private external fun requestPlayback(audioSink: Long, outputBuffer: ByteBuffer)

    companion object {
        init {
            System.loadLibrary("SoundGenerator")
        }
    }
}