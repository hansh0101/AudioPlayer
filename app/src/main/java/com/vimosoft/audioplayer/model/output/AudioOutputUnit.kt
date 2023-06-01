package com.vimosoft.audioplayer.model.output

import android.media.MediaFormat
import java.nio.ByteBuffer

/**
 * 오디오 파일 재생을 위해 출력을 처리하는 객체.
 */
abstract class AudioOutputUnit() {
    // ---------------------------------------------------------------------------------------------
    // AudioOutputUnit의 클래스 변수.
    /**
     * UI 표시를 위해 객체명을 나타내는 String 변수.
     */
    abstract val name: String

    // ---------------------------------------------------------------------------------------------
    // AudioOutputUnit이 외부에 제공하는 public methods.
    /**
     * AudioOutputUnit 객체를 구성한다.
     */
    abstract fun configure(mediaFormat: MediaFormat)

    /**
     * AudioOutputUnit 객체 사용을 마친 후 리소스를 정리한다.
     */
    abstract fun release()

    /**
     * outputBuffer에 들어있는 size 크기의 오디오 데이터를 소리로 출력한다.
     */
    abstract fun outputAudio(outputBuffer: ByteBuffer, size: Int)
}