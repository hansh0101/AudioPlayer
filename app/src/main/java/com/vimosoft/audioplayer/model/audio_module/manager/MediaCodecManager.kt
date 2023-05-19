package com.vimosoft.audioplayer.model.audio_module.manager

import android.media.MediaCodec
import android.media.MediaFormat
import java.nio.ByteBuffer

class MediaCodecManager {
    // ---------------------------------------------------------------------------------------------
    // class variables.
    private lateinit var mediaCodec: MediaCodec
    private val timeoutUs: Long = TIMEOUT_US
    private val bufferInfo = MediaCodec.BufferInfo()

    // ---------------------------------------------------------------------------------------------
    // public interfaces.
    fun configureAudioDecoder(mediaFormat: MediaFormat) {
        val mimeType: String? = mediaFormat.getString(MediaFormat.KEY_MIME)

        if (mimeType == null) {
            throw IllegalArgumentException("MIME type is null.")
        } else {
            mediaCodec = MediaCodec.createDecoderByType(mimeType).apply {
                configure(mediaFormat, null, null, 0)
                start()
            }
        }
    }

    fun release() {
        mediaCodec.run {
            stop()
            release()
        }
    }

    fun fetchEmptyInputBuffer(): InputBufferInfo {
        val inputBufferIndex = mediaCodec.dequeueInputBuffer(timeoutUs)
        return if (inputBufferIndex >= 0) {
            val inputBuffer = mediaCodec.getInputBuffer(inputBufferIndex)
            InputBufferInfo(inputBufferIndex, inputBuffer)
        } else {
            InputBufferInfo(inputBufferIndex, null)
        }
    }

    fun deliverFilledInputBuffer(
        bufferIndex: Int,
        offset: Int,
        size: Int,
        presentationTimeUs: Long,
    ) {
        if (size < 0) {
            mediaCodec.queueInputBuffer(
                bufferIndex,
                offset,
                0,
                presentationTimeUs,
                MediaCodec.BUFFER_FLAG_END_OF_STREAM
            )
        } else {
            mediaCodec.queueInputBuffer(bufferIndex, offset, size, presentationTimeUs, 0)
        }
    }

    fun fetchFilledOutputBuffer(): OutputBufferInfo {
        val outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, timeoutUs)
        val isEOS = bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
        return if (outputBufferIndex >= 0) {
            val outputBuffer = mediaCodec.getOutputBuffer(outputBufferIndex)
            OutputBufferInfo(bufferInfo, outputBufferIndex, outputBuffer, isEOS)
        } else {
            OutputBufferInfo(bufferInfo, outputBufferIndex, null, isEOS)
        }
    }

    fun releaseDiscardedOutputBuffer(bufferIndex: Int, render: Boolean) {
        mediaCodec.releaseOutputBuffer(bufferIndex, render)
    }

    fun flush() {
        mediaCodec.flush()
    }

    companion object {
        private const val TIMEOUT_US = 10000L
    }
}

data class InputBufferInfo(
    val bufferIndex: Int,
    val buffer: ByteBuffer?
)

data class OutputBufferInfo(
    val info: MediaCodec.BufferInfo,
    val bufferIndex: Int,
    val buffer: ByteBuffer?,
    val isEOS: Boolean
)