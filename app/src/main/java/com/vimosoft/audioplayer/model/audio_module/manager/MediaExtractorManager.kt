package com.vimosoft.audioplayer.model.audio_module.manager

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.MediaExtractor
import android.media.MediaFormat

class MediaExtractorManager {
    private lateinit var _mediaExtractor: MediaExtractor
    val mediaExtractor: MediaExtractor get() = _mediaExtractor

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