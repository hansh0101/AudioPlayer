package com.vimosoft.audioplayer.model.input

import android.content.res.AssetFileDescriptor
import android.media.MediaExtractor
import android.media.MediaFormat
import java.nio.ByteBuffer

/**
 * 미디어 파일을 재생하기 위해 MediaExtractor를 통해 압축된 미디어 파일을 읽어오는 작업을 전반적으로 담당하는 객체.
 */
class MediaExtractorInputUnit : AudioInputUnit() {
    // ---------------------------------------------------------------------------------------------
    // MediaExtractorManager 사용에 필요한 private variables.

    /**
     * 미디어 파일을 추출하는 MediaExtractor 객체.
     */
    private lateinit var mediaExtractor: MediaExtractor

    override val name: String = "MediaExtractor"

    // ---------------------------------------------------------------------------------------------
    // MediaExtractorManager가 외부에 제공하는 public methods.

    /**
     * MediaExtractor 객체를 구성하고 재생할 트랙의 MediaFormat을 반환한다.
     */
    override fun configure(
        assetFileDescriptor: AssetFileDescriptor,
        prefix: String
    ): MediaFormat {
        mediaExtractor = MediaExtractor().apply {
            setDataSource(assetFileDescriptor)
        }
        assetFileDescriptor.close()

        val trackIndex = getTrackIndexByType(prefix)
        if (trackIndex == -1) {
            throw IllegalStateException("There is no $prefix MIME type track.")
        }
        mediaExtractor.selectTrack(trackIndex)

        return mediaExtractor.getTrackFormat(trackIndex)
    }

    /**
     * MediaExtractorManager 사용을 마친 후 리소스를 정리한다.
     */
    override fun release() {
        mediaExtractor.release()
    }

    /**
     * MediaExtractor 객체를 사용해 미디어 파일을 읽어들여 destinationBuffer에 데이터를 쓰고 읽어온 데이터의 크기,
     * 현재 파일의 위치를 ExtractionResult 객체로 반환한다.
     */
    override fun extract(destinationBuffer: ByteBuffer): ExtractionResult {
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

    /**
     * 미디어 파일을 읽어올 위치를 조정한다.
     */
    override fun seekTo(playbackPosition: Long) {
        mediaExtractor.seekTo(playbackPosition, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
    }

    // ---------------------------------------------------------------------------------------------
    // MediaExtractorManager 내부적으로 사용하는 private methods.

    /**
     * 주어진 prefix에 해당하는 MIME 타입의 트랙 인덱스를 반환한다.
     */
    private fun getTrackIndexByType(prefix: String): Int {
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