package com.vimosoft.audioplayer.model.decode

import android.media.MediaCodec
import android.media.MediaCodecList
import android.media.MediaFormat

/**
 * MediaCodec을 통해 미디어 파일을 인코딩/디코딩하는 작업을 전반적으로 담당하는 객체.
 */
class MediaCodecDecodeProcessor : AudioDecodeProcessor() {
    // ---------------------------------------------------------------------------------------------
    // MediaCodecManager 사용에 필요한 private variables.

    /**
     * 미디어 파일을 인코딩/디코딩하는 MediaCodec 객체.
     */
    private lateinit var mediaCodec: MediaCodec

    /**
     * MediaCodec이 사용하는 버퍼 당 메타데이터를 담는 객체.
     */
    private val bufferInfo = MediaCodec.BufferInfo()

    // ---------------------------------------------------------------------------------------------
    // MediaCodecManager가 외부에 제공하는 public methods.

    /**
     * MediaCodec 객체를 구성하고 시작 가능한 상태로 만든다.
     */
    override fun configure(mediaFormat: MediaFormat) {
        val codecName = MediaCodecList(MediaCodecList.ALL_CODECS).findDecoderForFormat(mediaFormat)

        mediaCodec = MediaCodec.createByCodecName(codecName).apply {
            configure(mediaFormat, null, null, 0)
            start()
        }
    }

    /**
     * MediaCodecManager 사용을 마친 후 리소스를 정리한다.
     */
    override fun release() {
        mediaCodec.run {
            stop()
            release()
        }
    }

    /**
     * MediaCodec의 빈 입력 버퍼와 버퍼 인덱스를 InputBufferInfo로 반환한다.
     */
    override fun assignInputBuffer(): InputBufferInfo {
        val inputBufferIndex = mediaCodec.dequeueInputBuffer(TIMEOUT_US)
        return if (inputBufferIndex >= 0) {
            val inputBuffer = mediaCodec.getInputBuffer(inputBufferIndex)
            InputBufferInfo(inputBufferIndex, inputBuffer)
        } else {
            InputBufferInfo(inputBufferIndex, null)
        }
    }

    /**
     * 인코딩/디코딩을 위해 MediaCodec에 데이터로 채워진 입력 버퍼 처리를 요청한다.
     */
    override fun submitInputBuffer(
        bufferIndex: Int,
        offset: Int,
        size: Int,
        presentationTimeUs: Long,
    ) {
        if (size < 0) {
            mediaCodec.queueInputBuffer(
                bufferIndex,
                offset,
                0,
                presentationTimeUs,
                MediaCodec.BUFFER_FLAG_END_OF_STREAM
            )
        } else {
            mediaCodec.queueInputBuffer(bufferIndex, offset, size, presentationTimeUs, 0)
        }
    }

    /**
     * MediaCodec이 인코딩/디코딩한 출력 데이터를 담은 출력 버퍼와 버퍼 인덱스, 버퍼 메타데이터 및 EOS 도달 여부를
     * OutputBufferInfo로 반환한다.
     */
    override fun getOutputBuffer(): OutputBufferInfo {
        val outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
        val isEOS = bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
        return if (outputBufferIndex >= 0) {
            val outputBuffer = mediaCodec.getOutputBuffer(outputBufferIndex)
            OutputBufferInfo(bufferInfo, outputBufferIndex, outputBuffer, isEOS)
        } else {
            OutputBufferInfo(bufferInfo, outputBufferIndex, null, isEOS)
        }
    }

    /**
     * 인코딩/디코딩된 출력 데이터를 사용한 후 MediaCodec에 출력 버퍼를 반납한다.
     */
    override fun giveBackOutputBuffer(bufferIndex: Int, render: Boolean) {
        mediaCodec.releaseOutputBuffer(bufferIndex, render)
    }

    /**
     * MediaCodec의 입출력 데이터를 모두 flush한다.
     */
    override fun flush() {
        mediaCodec.flush()
    }

    companion object {
        private const val TIMEOUT_US = 10000L
    }
}
