package com.vimosoft.audioplayer.model.decode

import android.media.MediaCodec
import android.media.MediaCodecList
import android.media.MediaFormat
import timber.log.Timber

/**
 * 오디오 파일 재생을 위해 MediaCodec을 통해 처리(디코딩)를 담당하는 객체.
 */
class MediaCodecDecodeProcessor : AudioDecodeProcessor() {
    // ---------------------------------------------------------------------------------------------
    // MediaCodecDecodeProcessor 사용에 필요한 private variables.
    /**
     * 미디어 파일을 인코딩/디코딩하는 MediaCodec 객체.
     */
    private lateinit var mediaCodec: MediaCodec

    /**
     * MediaCodec이 사용하는 버퍼 당 메타데이터를 담는 객체.
     */
    private val bufferInfo = MediaCodec.BufferInfo()

    /**
     * UI 표시를 위해 객체명을 나타내는 String 변수.
     */
    override val name: String = "MediaCodec"

    // ---------------------------------------------------------------------------------------------
    // MediaCodecDecodeProcessor가 외부에 제공하는 public methods.
    /**
     * MediaCodec 객체를 구성 및 실행시키고, 출력 오디오 파일(데이터)의 MediaFormat을 반환한다.
     */
    override fun configure(mediaFormat: MediaFormat): MediaFormat {
        val codecName = MediaCodecList(MediaCodecList.ALL_CODECS).findDecoderForFormat(mediaFormat)

        mediaCodec = MediaCodec.createByCodecName(codecName).apply {
            configure(mediaFormat, null, null, 0)
            start()
        }

        return mediaCodec.outputFormat
    }

    /**
     * MediaCodecDecodeProcessor 객체 사용을 마친 후 리소스를 정리한다.
     */
    override fun release() {
        mediaCodec.run {
            stop()
            release()
        }
    }

    /**
     * MediaCodec 객체가 디코딩을 위한 입력 버퍼를 할당해주고 입력 버퍼의 인덱스와 입력 버퍼를 InputBufferInfo 객체에 담아 반환한다.
     */
    override fun assignInputBuffer(): InputBufferInfo {
        // MediaCodec 객체를 사용해 가용한 입력 버퍼의 인덱스를 가져온다.
        val inputBufferIndex = mediaCodec.dequeueInputBuffer(TIMEOUT_US)

        return if (inputBufferIndex >= 0) {
            // 입력 버퍼의 인덱스가 유효하다면 입력 버퍼의 인덱스와 입력 버퍼를 InputBufferInfo 객체에 담아 반환한다.
            val inputBuffer = mediaCodec.getInputBuffer(inputBufferIndex)
            InputBufferInfo(inputBufferIndex, inputBuffer)
        } else {
            // 입력 버퍼의 인덱스가 유효하지 않다면 입력 버퍼의 (유효하지 않은) 인덱스와 null을 InputBufferInfo 객체에 담아 반환한다.
            InputBufferInfo(inputBufferIndex, null)
        }
    }

    /**
     * MediaCodec 객체가 디코딩을 위한 입력 데이터가 담긴 입력 버퍼의 인덱스와 offset, 재생할 데이터 크기 및 presentationTimeUs를 전달받아
     *  해당 입력 버퍼의 디코딩을 요청한다.
     */
    override fun submitInputBuffer(
        bufferIndex: Int,
        offset: Int,
        size: Int,
        presentationTimeUs: Long,
    ) {
        if (size < 0) {
            // 재생할 데이터의 크기가 0 미만, 즉 유효하지 않다면 EOS로 간주하여 MediaCodec 객체에 디코딩을 요청한다.
            mediaCodec.queueInputBuffer(
                bufferIndex,
                offset,
                0,
                presentationTimeUs,
                MediaCodec.BUFFER_FLAG_END_OF_STREAM
            )
        } else {
            // 재생할 데이터의 크기가 0 이상이라면 아무런 flag 없이 MediaCodec 객체에 디코딩을 요청한다.
            mediaCodec.queueInputBuffer(bufferIndex, offset, size, presentationTimeUs, 0)
        }
    }

    /**
     * MediaCodec 객체가 디코딩된 출력 데이터가 담긴 출력 버퍼를 제공한다. 해당 출력 버퍼의 디코딩 정보가 담긴 BufferInfo 객체와
     *  출력 버퍼의 인덱스, 출력 버퍼와 EOS 도달 여부를 OutputBufferInfo에 담아 반환한다.
     */
    override fun getOutputBuffer(): OutputBufferInfo {
        // MediaCodec 객체를 사용해 디코딩된 데이터가 담긴 출력 버퍼의 인덱스를 가져온다.
        val outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
        // EOS 도달 여부를 확인한다.
        val isEOS = bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0

        return if (outputBufferIndex >= 0) {
            // 출력 버퍼의 인덱스가 유효하다면 MediaCodec이 제공하는 출력 버퍼의 정보가 담긴 BufferInfo 객체와 출력 버퍼 인덱스,
            // 출력 버퍼와 EOS 도달 여부를 OutputBufferInfo 객체에 담아 반환한다.
            val outputBuffer = mediaCodec.getOutputBuffer(outputBufferIndex)

            if (outputBuffer != null) {
                val buffer = copyByteBuffer(outputBuffer)
                OutputBufferInfo(bufferInfo, outputBufferIndex, buffer, isEOS)
            } else {
                OutputBufferInfo(bufferInfo, outputBufferIndex, null, isEOS)
            }
        } else {
            // 출력 버퍼의 인덱스가 유효하지 않다면 MediaCodec이 제공하는 출력 버퍼의 정보가 담긴 BufferInfo 객체와 출력 버퍼 인덱스,
            // null과 EOS 도달 여부를 OutputBufferInfo 객체에 담아 반환한다.
            OutputBufferInfo(bufferInfo, outputBufferIndex, null, isEOS)
        }
    }

    /**
     * 출력 버퍼 사용을 마치고 MediaCodec 객체에 출력 버퍼를 반납한다.
     */
    override fun giveBackOutputBuffer(bufferIndex: Int, render: Boolean) {
        runCatching {
            mediaCodec.releaseOutputBuffer(bufferIndex, render)
        }.onFailure { Timber.e(it) }
    }

    /**
     * MediaCodec 객체가 디코딩 중이던 데이터를 flush한다.
     */
    override fun flush() {
        mediaCodec.flush()
    }

    companion object {
        private const val TIMEOUT_US = 10000L
    }
}
