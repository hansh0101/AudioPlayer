package com.vimosoft.audioplayer.model.decode

import android.media.MediaCodec
import android.media.MediaFormat
import java.nio.ByteBuffer

abstract class AudioDecodeProcessor {
    abstract fun configure(mediaFormat: MediaFormat)
    abstract fun release()
    abstract fun assignInputBuffer(): InputBufferInfo
    abstract fun submitInputBuffer(
        bufferIndex: Int,
        offset: Int,
        size: Int,
        presentationTimeUs: Long
    )

    abstract fun getOutputBuffer(): OutputBufferInfo
    abstract fun giveBackOutputBuffer(bufferIndex: Int, render: Boolean)
    abstract fun flush()
}

/**
 * 입력 버퍼에 대한 정보를 담는 객체.
 */
data class InputBufferInfo(
    val bufferIndex: Int,
    val buffer: ByteBuffer?
)

/**
 * 출력 버퍼에 대한 정보를 담는 객체.
 */
data class OutputBufferInfo(
    val info: MediaCodec.BufferInfo,
    val bufferIndex: Int,
    val buffer: ByteBuffer?,
    val isEOS: Boolean
)