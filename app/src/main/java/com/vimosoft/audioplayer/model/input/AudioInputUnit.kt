package com.vimosoft.audioplayer.model.input

import android.content.res.AssetFileDescriptor
import android.media.MediaFormat
import java.nio.ByteBuffer

/**
 * 오디오 파일 재생을 위해 입력을 처리하는 객체.
 */
abstract class AudioInputUnit {
    // ---------------------------------------------------------------------------------------------
    // AudioInputUnit의 클래스 변수.
    /**
     * UI 표시를 위해 객체명을 나타내는 String 변수.
     */
    abstract val name: String

    // ---------------------------------------------------------------------------------------------
    // AudioInputUnit이 외부에 제공하는 public methods.
    /**
     * AudioInputUnit 객체를 구성하고 입력 오디오 파일(데이터)의 MediaFormat을 반환한다.
     */
    abstract fun configure(assetFileDescriptor: AssetFileDescriptor): MediaFormat

    /**
     * AudioInputUnit 객체 사용을 마친 후 리소스를 정리한다.
     */
    abstract fun release()

    /**
     * 입력 오디오 파일을 읽어 destinationBuffer에 데이터를 write하고,
     *  읽어온 데이터의 크기와 현재 파일 내 읽은 위치를 ExtractionResult 객체에 담아 반환한다.
     */
    abstract fun extract(destinationBuffer: ByteBuffer): ExtractionResult

    /**
     * 오디오 파일을 읽어올 위치를 조정한다.
     */
    abstract fun seekTo(playbackPosition: Long)
}

/**
 * 미디어 파일 추출 결과를 담는 객체.
 */
data class ExtractionResult(
    val sampleSize: Int,
    val presentationTimeUs: Long
)