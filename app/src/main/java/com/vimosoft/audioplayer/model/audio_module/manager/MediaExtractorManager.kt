package com.vimosoft.audioplayer.model.audio_module.manager

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.MediaExtractor
import android.media.MediaFormat
import java.nio.ByteBuffer

class MediaExtractorManager {
    // ---------------------------------------------------------------------------------------------
    // class variables.
    private lateinit var mediaExtractor: MediaExtractor

    // ---------------------------------------------------------------------------------------------
    // public interfaces.
    fun configureMediaExtractor(context: Context, fileName: String, prefix: String): MediaFormat {
        val assetFileDescriptor: AssetFileDescriptor = context.assets.openFd(fileName)
        mediaExtractor = MediaExtractor().apply {
            setDataSource(assetFileDescriptor)
        }
        assetFileDescriptor.close()

        val trackIndex = findTrackIndex(prefix)
        if (trackIndex == -1) {
            throw IllegalStateException("There is no $prefix MIME type track.")
        }
        mediaExtractor.selectTrack(trackIndex)

        return mediaExtractor.getTrackFormat(trackIndex)
    }

    fun release() {
        mediaExtractor.release()
    }

    fun extract(destinationBuffer: ByteBuffer): ExtractionResult {
        val sampleSize: Int = mediaExtractor.readSampleData(destinationBuffer, 0)
        val presentationTimeUs: Long

        if (sampleSize < 0) {
            presentationTimeUs = 0L
        } else {
            presentationTimeUs = mediaExtractor.sampleTime
            mediaExtractor.advance()
        }

        return ExtractionResult(sampleSize, presentationTimeUs)
    }

    fun seekTo(playbackPosition: Long) {
        mediaExtractor.seekTo(playbackPosition, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
    }

    // ---------------------------------------------------------------------------------------------
    // private methods.
    private fun findTrackIndex(prefix: String): Int {
        var trackIndex = -1
        for (i in 0 until mediaExtractor.trackCount) {
            val mediaFormat: MediaFormat = mediaExtractor.getTrackFormat(i)
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