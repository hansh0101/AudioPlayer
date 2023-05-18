package com.vimosoft.audioplayer.model.audio_module.manager

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat

class MediaExtractorManager {
    companion object {
        /**
         * MediaExtractor 객체를 생성하고 주어진 `fileName`을 Data Source로 지정한다. 해당 Media file에서 주어진
         *  `prefix`로 시작하는 MIME type을 가진 트랙을 찾는다. 이후 MediaExtractor 객체와 해당 트랙의 trackIndex를
         *   MediaExtractorInfo로 Wrapping해 반환한다.
         */
        fun createAudioExtractor(context: Context, fileName: String, prefix: String): MediaExtractorInfo {
            val assetFileDescriptor = context.assets.openFd(fileName)
            val mediaExtractor = MediaExtractor().apply {
                setDataSource(assetFileDescriptor)
            }
            assetFileDescriptor.close()

            var trackIndex = -1

            for (i in 0 until mediaExtractor.trackCount) {
                val mediaFormat = mediaExtractor.getTrackFormat(i)
                val mimeType = mediaFormat.getString(MediaFormat.KEY_MIME)
                    ?: throw IllegalStateException("There is NO MIME type.")

                if (mimeType.startsWith("audio/")) {
                    trackIndex = i
                    break
                }
            }

            if (trackIndex == -1) {
                throw IllegalStateException("There is NO $prefix type tracks.")
            }

            mediaExtractor.selectTrack(trackIndex)

            return MediaExtractorInfo(mediaExtractor, trackIndex)
        }
    }
}

data class MediaExtractorInfo(
    val mediaExtractor: MediaExtractor,
    val trackIndex: Int
)