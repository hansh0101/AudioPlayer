package com.vimosoft.audioplayer.model.audio_module.manager

import android.media.MediaCodec
import android.media.MediaFormat
import java.nio.ByteBuffer

class MediaDecoderManager {
    // ---------------------------------------------------------------------------------------------
    // class variables.
    private lateinit var _mediaDecoder: MediaCodec
    private val timeoutUs: Long = TIMEOUT_US
    private val bufferInfo = MediaCodec.BufferInfo()

    // ---------------------------------------------------------------------------------------------
    // public interfaces.
    fun configureAudioDecoder(mediaFormat: MediaFormat) {
        val mimeType: String? = mediaFormat.getString(MediaFormat.KEY_MIME)

        if (mimeType == null) {
            throw IllegalArgumentException("MIME type is null.")
        } else {
            _mediaDecoder = MediaCodec.createDecoderByType(mimeType).apply {
                configure(mediaFormat, null, null, 0)
                start()
            }
        }
    }

    fun release() {
        _mediaDecoder.run {
            stop()
            release()
        }
    }

    fun fetchEmptyInputBuffer(): InputBufferInfo {
        val inputBufferIndex = _mediaDecoder.dequeueInputBuffer(timeoutUs)
        return if (inputBufferIndex >= 0) {
            val inputBuffer = _mediaDecoder.getInputBuffer(inputBufferIndex)
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
            _mediaDecoder.queueInputBuffer(
                bufferIndex,
                offset,
                0,
                presentationTimeUs,
                MediaCodec.BUFFER_FLAG_END_OF_STREAM
            )
        } else {
            _mediaDecoder.queueInputBuffer(bufferIndex, offset, size, presentationTimeUs, 0)
        }
    }

    fun fetchFilledOutputBuffer(): OutputBufferInfo {
        val outputBufferIndex = _mediaDecoder.dequeueOutputBuffer(bufferInfo, timeoutUs)
        return if (outputBufferIndex >= 0) {
            val outputBuffer = _mediaDecoder.getOutputBuffer(outputBufferIndex)
            OutputBufferInfo(bufferInfo, outputBufferIndex, outputBuffer)
        } else {
            OutputBufferInfo(bufferInfo, outputBufferIndex, null)
        }
    }

    fun releaseDiscardedOutputBuffer(bufferIndex: Int, render: Boolean) {
        _mediaDecoder.releaseOutputBuffer(bufferIndex, render)
    }

    fun flush() {
        _mediaDecoder.flush()
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
    val buffer: ByteBuffer?
)