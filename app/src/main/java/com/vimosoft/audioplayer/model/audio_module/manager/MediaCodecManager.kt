package com.vimosoft.audioplayer.model.audio_module.manager

import android.media.MediaCodec
import android.media.MediaFormat

class MediaCodecManager {
    companion object {
        fun createAudioDecoder(mediaFormat: MediaFormat): MediaCodec {
            val mimeType = mediaFormat.getString(MediaFormat.KEY_MIME)

            if (mimeType == null) {
                throw IllegalArgumentException("MIME type is null.")
            } else {
                return MediaCodec.createDecoderByType(mimeType).apply {
                    configure(mediaFormat, null, null, 0)
                    start()
                }
            }
        }
    }
}