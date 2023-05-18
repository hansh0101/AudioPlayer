package com.vimosoft.audioplayer.model.audio_module.manager

import android.media.MediaCodec
import android.media.MediaFormat

class MediaDecoderManager {
    // ---------------------------------------------------------------------------------------------
    // class variables.
    private lateinit var _mediaDecoder: MediaCodec
    val mediaDecoder: MediaCodec get() = _mediaDecoder

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
}