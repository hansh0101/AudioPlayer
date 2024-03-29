package com.vimosoft.audioplayer.model.decode

import android.media.MediaCodec
import android.media.MediaFormat
import java.nio.ByteBuffer

/**
 * 오디오 파일 재생을 위해 처리(디코딩)를 담당하는 객체.
 */
abstract class AudioDecodeProcessor {
    // ---------------------------------------------------------------------------------------------
    // AudioDecodeProcessor의 클래스 변수.
    /**
     * UI 표시를 위해 객체명을 나타내는 String 변수.
     */
    abstract val name: String

    // ---------------------------------------------------------------------------------------------
    // AudioDecodeProcessor가 외부에 제공하는 public methods.
    /**
     * AudioDecodeProcessor 객체를 구성하고 출력 오디오 파일(데이터)의 MediaFormat을 반환한다.
     */
    abstract fun configure(mediaFormat: MediaFormat): MediaFormat

    /**
     * AudioDecodeProcessor 객체 사용을 마친 후 리소스를 정리한다.
     */
    abstract fun release()

    /**
     * 디코딩을 위한 입력 버퍼를 할당해주고 입력 버퍼의 인덱스와 입력 버퍼를 InputBufferInfo 객체에 담아 반환한다.
     */
    abstract fun assignInputBuffer(): InputBufferInfo

    /**
     * 디코딩을 위한 입력 데이터가 담긴 입력 버퍼의 인덱스와 offset, 재생할 데이터 크기 및 presentationTimeUs를 전달받아
     *  해당 입력 버퍼의 디코딩을 요청한다.
     */
    abstract fun submitInputBuffer(
        bufferIndex: Int,
        offset: Int,
        size: Int,
        presentationTimeUs: Long
    )

    /**
     * 디코딩된 출력 데이터가 담긴 출력 버퍼를 제공한다. 해당 출력 버퍼의 디코딩 정보가 담긴 BufferInfo 객체와
     *  출력 버퍼의 인덱스, 출력 버퍼와 EOS 도달 여부를 OutputBufferInfo에 담아 반환한다.
     */
    abstract fun getOutputBuffer(): OutputBufferInfo

    /**
     * 출력 버퍼 사용을 마치고 AudioDecodeProcessor 객체에 출력 버퍼를 반납한다.
     */
    abstract fun giveBackOutputBuffer(bufferIndex: Int, render: Boolean)

    /**
     * AudioDecodeProcessor 객체가 디코딩 중이던 데이터를 flush한다.
     */
    abstract fun flush()

    /**
     * sourceBuffer를 destinationBuffer로 깊은 복사를 한 후 destinationBuffer를 반환한다.
     */
    protected fun copyByteBuffer(sourceBuffer: ByteBuffer) : ByteBuffer {
        // Direct Buffer인지 확인 후 해당하는 Buffer를 생성한다.
        val destinationBuffer = if (sourceBuffer.isDirect) {
            ByteBuffer.allocateDirect(sourceBuffer.capacity())
        } else {
            ByteBuffer.allocate(sourceBuffer.capacity())
        }

        // sourceBuffer의 position, limit, byteOrder를 저장해둔다.
        val position = sourceBuffer.position()
        val limit = sourceBuffer.limit()
        val byteOrder = sourceBuffer.order()

        // sourceBuffer의 position을 0으로 돌려 처음부터 읽을 준비를 한다.
        sourceBuffer.rewind()
        // destinationBuffer의 order를 sourceBuffer의 order와 맞춘다.
        destinationBuffer.order(byteOrder)
        // destinationBuffer에 sourceBuffer 데이터를 복사한다.
        destinationBuffer.put(sourceBuffer)

        // destinationBuffer의 position, limit을 sourceBuffer의 원래 position, limit과 맞춘다.
        destinationBuffer.position(position)
        destinationBuffer.limit(limit)

        return destinationBuffer
    }
}

/**
 * 입력 버퍼에 대한 정보를 담는 객체.
 */
data class InputBufferInfo(
    val bufferIndex: Int,
    val buffer: ByteBuffer?
)

/**
 * 출력 버퍼에 대한 정보를 담는 객체.
 */
data class OutputBufferInfo(
    val info: MediaCodec.BufferInfo,
    val bufferIndex: Int,
    val buffer: ByteBuffer?,
    val isEOS: Boolean
)