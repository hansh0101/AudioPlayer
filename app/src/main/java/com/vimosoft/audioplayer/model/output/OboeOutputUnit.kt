package com.vimosoft.audioplayer.model.output

import android.media.AudioFormat
import android.media.MediaFormat
import java.nio.ByteBuffer

/**
 * 오디오 파일 재생을 위해 Oboe를 통해 출력을 처리하는 객체.
 */
class OboeOutputUnit : AudioOutputUnit() {
    // ---------------------------------------------------------------------------------------------
    // OboeOutputUnit 사용에 필요한 private variables.
    /**
     * Oboe AudioStream 객체 포인터 주소값을 나타내는 Long 변수.
     */
    private var oboeStreamAddress: Long = 0L

    /**
     * UI 표시를 위해 객체명을 나타내는 String 변수.
     */
    override val name: String = "Oboe"

    // ---------------------------------------------------------------------------------------------
    // OboeOutputUnit이 외부에 제공하는 public methods.
    /**
     * Oboe AudioStream 객체를 구성한다.
     */
    override fun configure(mediaFormat: MediaFormat) {
        if (oboeStreamAddress == 0L) {
            val channelCount = mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            val sampleRate = mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            var isFloat = false
            val bitDepth = try {
                when (mediaFormat.getInteger(MediaFormat.KEY_PCM_ENCODING)) {
                    AudioFormat.ENCODING_PCM_FLOAT -> {
                        isFloat = true
                        32
                    }

                    AudioFormat.ENCODING_PCM_32BIT -> 32
                    AudioFormat.ENCODING_PCM_24BIT_PACKED -> 24
                    AudioFormat.ENCODING_PCM_16BIT -> 16
                    AudioFormat.ENCODING_PCM_8BIT -> 8
                    else -> 16
                }
            } catch (e: Exception) {
                16
            }

            oboeStreamAddress = initialize(channelCount, sampleRate, bitDepth, isFloat)
            start()
        }
    }

    /**
     * OboeOutputUnit 객체 사용을 마친 후 리소스를 정리한다.
     */
    override fun release() {
        if (oboeStreamAddress != 0L) {
            release(oboeStreamAddress)
            oboeStreamAddress = 0L
        }
    }

    /**
     * 오디오 재생을 시작한다.
     */
    override fun start() {
        if (oboeStreamAddress != 0L) {
            requestStart(oboeStreamAddress)
        }
    }

    /**
     * 오디오 재생을 중지한다.
     */
    override fun pause() {
        if (oboeStreamAddress != 0L) {
            requestPause(oboeStreamAddress)
        }
    }

    /**
     * Oboe AudioStream 객체를 사용해 outputBuffer에 들어있는 size 크기의 오디오 데이터를 소리로 출력한다.
     */
    override fun outputAudio(outputBuffer: ByteBuffer, size: Int) {
        if (oboeStreamAddress != 0L) {
            val outputByteArray = ByteArray(size)
            outputBuffer.get(outputByteArray)
            requestPlayback(oboeStreamAddress, outputByteArray, size)
        }
    }

    // ---------------------------------------------------------------------------------------------
    // JNI - OboeOutputUnit 내부적으로 사용하는 private methods.
    /**
     * JNI - C++ AudioSinkUnit 객체를 구성하고 Long 주소값을 반환한다.
     */
    private external fun initialize(
        channelCount: Int,
        sampleRate: Int,
        bitDepth: Int,
        isFloat: Boolean
    ): Long

    /**
     * JNI - AudioSinkUnit 객체 사용을 마친 후 리소스를 정리한다.
     */
    private external fun release(oboeStreamAddress: Long)

    /**
     * JNI - AudioSinkUnit 객체를 사용해 outputBuffer에 들어있는 size 크기의 오디오 데이터를 소리로 출력한다.
     */
    private external fun requestPlayback(
        oboeStreamAddress: Long,
        outputByteArray: ByteArray,
        size: Int
    )

    /**
     * JNI - AudioSinkUnit 객체의 오디오 재생 시작을 요청한다.
     */
    private external fun requestStart(oboeStreamAddress: Long)

    /**
     * JNI - AudioSinkUnit 객체의 오디오 재생 중지를 요청한다.
     */
    private external fun requestPause(oboeStreamAddress: Long)

    companion object {
        init {
            System.loadLibrary("SoundGenerator")
        }
    }
}