package com.vimosoft.audioplayer.model

import android.media.MediaFormat
import java.nio.ByteBuffer

class OboeAudioOutputUnit {
    private var audioOutputUnit = 0L

    fun configure(mediaFormat: MediaFormat) {
        if (audioOutputUnit == 0L) {
            audioOutputUnit = initialize()
        }
    }

    fun release() {
        if (audioOutputUnit != 0L) {
            releaseOutputUnit()
            audioOutputUnit = 0L
        }
    }

    fun outputAudio(outputBuffer: ByteBuffer, size: Int) {
        if (audioOutputUnit != 0L) {
            val thread = Thread {
                val numFrames = getNumberOfFrames(outputBuffer, 2, size)
                requestPlayback(buffer = outputBuffer, numberOfFrames = numFrames)
            }
            thread.start()
            thread.join()
        }
    }

    fun flush() {
        if (audioOutputUnit != 0L) {
            flushBuffer()
        }
    }

    private fun getNumberOfFrames(
        buffer: ByteBuffer,
        channelCount: Int,
        sampleSize: Int
    ): Int {
        val capacity = buffer.capacity()
        val bytesPerFrame = channelCount * sampleSize
        return capacity / bytesPerFrame
    }

    // JNI func
    /**
     * 초기화
     */
    private external fun initialize(): Long

    /**
     * 소멸
     */
    private external fun releaseOutputUnit(outputUnit: Long = audioOutputUnit)

    /**
     * 재생
     */
    private external fun requestPlayback(
        outputUnit: Long = audioOutputUnit,
        buffer: ByteBuffer,
        numberOfFrames: Int
    )

    /**
     * flush
     */
    private external fun flushBuffer(outputUnit: Long = audioOutputUnit)

    companion object {
        init {
            System.loadLibrary("SoundGenerator");
        }
    }
}