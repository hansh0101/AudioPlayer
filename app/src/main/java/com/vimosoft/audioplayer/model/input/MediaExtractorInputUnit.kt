package com.vimosoft.audioplayer.model.input

import android.content.res.AssetFileDescriptor
import android.media.MediaExtractor
import android.media.MediaFormat
import java.nio.ByteBuffer

/**
 * 오디오 파일 재생을 위해 MediaExtractor를 통해 입력을 처리하는 객체.
 */
class MediaExtractorInputUnit : AudioInputUnit() {
    // ---------------------------------------------------------------------------------------------
    // MediaExtractorInputUnit 사용에 필요한 private variables.
    /**
     * 미디어 파일을 추출하는 MediaExtractor 객체.
     */
    private lateinit var mediaExtractor: MediaExtractor

    /**
     * UI 표시를 위해 객체명을 나타내는 String 변수.
     */
    override val name: String = "MediaExtractor"

    // ---------------------------------------------------------------------------------------------
    // MediaExtractorInputUnit이 외부에 제공하는 public methods.
    /**
     * MediaExtractor 객체를 구성하고 입력 오디오 파일(데이터)의 MediaFormat을 반환한다.
     */
    override fun configure(assetFileDescriptor: AssetFileDescriptor): MediaFormat {
        // MediaExtractor 객체를 생성하고 data source를 설정한다.
        mediaExtractor = MediaExtractor().apply {
            setDataSource(assetFileDescriptor)
        }
        // File Descriptor를 닫는다.
        assetFileDescriptor.close()

        // audio MIME 타입의 트랙 인덱스를 찾는다.
        val trackIndex = getAudioTrackIndex()
        // audio MIME 타입의 트랙이 존재하지 않으면 IllegalStateException을 발생시킨다.
        if (trackIndex == -1) {
            throw IllegalStateException("There is no audio MIME type track.")
        }
        // MediaExtractor 객체의 트랙을 해당 트랙 인덱스로 설정한다.
        mediaExtractor.selectTrack(trackIndex)

        // 해당 트랙 인덱스의 MediaFormat을 반환한다.
        return mediaExtractor.getTrackFormat(trackIndex)
    }

    /**
     * AudioInputUnit 객체 사용을 마친 후 리소스를 정리한다.
     */
    override fun release() {
        mediaExtractor.release()
    }

    /**
     * MediaExtractor 객체를 사용해 입력 오디오 파일을 읽어 destinationBuffer에 데이터를 write하고,
     *  읽어온 데이터의 크기와 현재 파일 내 읽은 위치를 ExtractionResult 객체에 담아 반환한다.
     */
    override fun extract(destinationBuffer: ByteBuffer): ExtractionResult {
        // MediaExtractor 객체를 사용해 입력 오디오 파일을 읽고, sampleSize를 얻는다.
        val sampleSize: Int = mediaExtractor.readSampleData(destinationBuffer, 0)
        // 현재 파일 내 읽은 위치 (단위 - microsecond)
        val presentationTimeUs: Long

        if (sampleSize < 0) {
            // sampleSize가 0 이하라면 presentationTimeUs를 0으로 할당한다.
            presentationTimeUs = 0L
        } else {
            // sampleSize가 0 이상이라면 MediaExtractor에서 sampleTime을 읽어와 presentationTimeUs에 할당한다.
            presentationTimeUs = mediaExtractor.sampleTime
            // 이후 다음 데이터를 읽을 수 있도록 MediaExtractor 객체가 읽을 위치를 다음 위치로 이동시킨다.
            mediaExtractor.advance()
        }

        // ExtractionResult 객체에 sampleSize와 presentationTimeUs를 담아 반환한다.
        return ExtractionResult(sampleSize, presentationTimeUs)
    }

    /**
     * MediaExtractor 객체가 오디오 파일을 읽어올 위치를 조정한다.
     */
    override fun seekTo(playbackPosition: Long) {
        // MediaExtractor 객체가 읽을 위치를 playbackPosition으로 이동시킨다.
        mediaExtractor.seekTo(playbackPosition, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
    }

    // ---------------------------------------------------------------------------------------------
    // MediaExtractorInputUnit 내부적으로 사용하는 private methods.

    /**
     * audio MIME 타입의 트랙 인덱스를 반환한다.
     */
    private fun getAudioTrackIndex(): Int {
        var trackIndex = -1
        for (i in 0 until mediaExtractor.trackCount) {
            val mediaFormat: MediaFormat = mediaExtractor.getTrackFormat(i)
            val mimeType: String? = mediaFormat.getString(MediaFormat.KEY_MIME)

            if (mimeType?.startsWith("audio/") == true) {
                trackIndex = i
                break
            }
        }

        return trackIndex
    }
}