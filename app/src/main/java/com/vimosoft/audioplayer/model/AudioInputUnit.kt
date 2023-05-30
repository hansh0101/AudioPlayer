package com.vimosoft.audioplayer.model

import android.content.res.AssetFileDescriptor
import android.media.MediaFormat
import java.nio.ByteBuffer

abstract class AudioInputUnit {
    abstract fun configure(assetFileDescriptor: AssetFileDescriptor, prefix: String): MediaFormat
    abstract fun release()
    abstract fun extract(destinationBuffer: ByteBuffer): ExtractionResult
    abstract fun seekTo(playbackPosition: Long)
}

/**
 * 미디어 파일 추출 결과를 담는 객체.
 */
data class ExtractionResult(
    val sampleSize: Int,
    val presentationTimeUs: Long
)