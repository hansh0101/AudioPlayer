package com.vimosoft.audioplayer.model.audio_module.manager

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.MediaExtractor
import android.media.MediaFormat
import java.nio.ByteBuffer

class MediaExtractorManager {
    // ---------------------------------------------------------------------------------------------
    // class variables.
    private lateinit var _mediaExtractor: MediaExtractor
    val mediaExtractor: MediaExtractor get() = _mediaExtractor

    // ---------------------------------------------------------------------------------------------
    // public interfaces.
    fun configureMediaExtractor(context: Context, fileName: String, prefix: String): MediaFormat {
        val assetFileDescriptor: AssetFileDescriptor = context.assets.openFd(fileName)
        _mediaExtractor = MediaExtractor().apply {
            setDataSource(assetFileDescriptor)
        }
        assetFileDescriptor.close()

        val trackIndex = findTrackIndex(prefix)
        if (trackIndex == -1) {
            throw IllegalStateException("There is no $prefix MIME type track.")
        }
        _mediaExtractor.selectTrack(trackIndex)

        return _mediaExtractor.getTrackFormat(trackIndex)
    }

    fun release() {
        _mediaExtractor.release()
    }

    fun extract(destinationBuffer: ByteBuffer): ExtractionResult {
        val sampleSize: Int = _mediaExtractor.readSampleData(destinationBuffer, 0)
        val presentationTimeUs: Long

        if (sampleSize < 0) {
            presentationTimeUs = 0L
        } else {
            presentationTimeUs = _mediaExtractor.sampleTime
            _mediaExtractor.advance()
        }

        return ExtractionResult(sampleSize, presentationTimeUs)
    }

    // ---------------------------------------------------------------------------------------------
    // private methods.
    private fun findTrackIndex(prefix: String): Int {
        var trackIndex = -1
        for (i in 0 until _mediaExtractor.trackCount) {
            val mediaFormat: MediaFormat = _mediaExtractor.getTrackFormat(i)
            val mimeType: String? = mediaFormat.getString(MediaFormat.KEY_MIME)

            if (mimeType?.startsWith(prefix) == true) {
                trackIndex = i
                break
            }
        }

        return trackIndex
    }
}

data class ExtractionResult(
    val sampleSize: Int,
    val presentationTimeUs: Long
)