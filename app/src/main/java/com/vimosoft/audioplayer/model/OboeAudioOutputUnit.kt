package com.vimosoft.audioplayer.model

import android.media.MediaFormat
import java.nio.ByteBuffer

class OboeAudioOutputUnit {
    private var audioSink: Long = 0L

    fun configure(mediaFormat: MediaFormat) {
        val channelCount = mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        val sampleRate = mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)

        if (audioSink == 0L) {
            audioSink = initialize(channelCount, sampleRate)
        }
    }

    fun release() {
        if (audioSink != 0L) {
            release(audioSink)
            audioSink = 0L
        }
    }

    fun outputAudio(outputBuffer: ByteBuffer, size: Int) {
        if (audioSink != 0L) {
            requestPlayback(audioSink, outputBuffer)
        }
    }

    fun flush() {
        if (audioSink != 0L) {
            requestStop(audioSink)
            audioSink = 0L
        }
    }

    private external fun initialize(channelCount: Int, sampleRate: Int): Long
    private external fun release(audioSink: Long)
    private external fun requestPlayback(audioSink: Long, outputBuffer: ByteBuffer)
    private external fun requestStop(audioSink: Long)

    companion object {
        init {
            System.loadLibrary("SoundGenerator")
        }
    }
}