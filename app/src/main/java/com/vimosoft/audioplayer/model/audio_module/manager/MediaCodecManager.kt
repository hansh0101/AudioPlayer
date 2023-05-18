package com.vimosoft.audioplayer.model.audio_module.manager

import android.media.MediaCodec
import android.media.MediaFormat

class MediaCodecManager {
    // ---------------------------------------------------------------------------------------------
    // class variables.
    private lateinit var _mediaCodec: MediaCodec
    val mediaCodec: MediaCodec get() = _mediaCodec

    // ---------------------------------------------------------------------------------------------
    // public interfaces.
    fun configureAudioDecoder(mediaFormat: MediaFormat) {
        val mimeType: String? = mediaFormat.getString(MediaFormat.KEY_MIME)

        if (mimeType == null) {
            throw IllegalArgumentException("MIME type is null.")
        } else {
            _mediaCodec = MediaCodec.createDecoderByType(mimeType).apply {
                configure(mediaFormat, null, null, 0)
                start()
            }
        }
    }

    fun release() {
        _mediaCodec.run {
            stop()
            release()
        }
    }
}