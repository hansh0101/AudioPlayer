package com.vimosoft.audioplayer.model.output

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaFormat
import java.nio.ByteBuffer

/**
 * 오디오 파일 재생을 위해 AudioTrack을 통해 출력을 처리하는 객체.
 */
class AudioTrackOutputUnit : AudioOutputUnit() {
    // ---------------------------------------------------------------------------------------------
    // AudioTrackOutputUnit 사용에 필요한 private variables.
    /**
     * 소리를 출력하는 AudioTrack 객체.
     */
    private lateinit var audioTrack: AudioTrack

    /**
     * UI 표시를 위해 객체명을 나타내는 String 변수.
     */
    override val name: String = "AudioTrack"

    // ---------------------------------------------------------------------------------------------
    // AudioTrackOutputUnit이 외부에 제공하는 public methods.
    /**
     * AudioTrack 객체를 구성한다.
     */
    override fun configure(mediaFormat: MediaFormat) {
        val sampleRateInHz = mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val channelConfig = getChannelConfig(mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT))
        val audioEncodingFormat = try {
            mediaFormat.getInteger(MediaFormat.KEY_PCM_ENCODING)
        } catch (e: Exception) {
            AudioFormat.ENCODING_PCM_16BIT
        }
        val bufferSizeInBytes =
            AudioTrack.getMinBufferSize(sampleRateInHz, channelConfig, audioEncodingFormat)

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        val audioFormat = AudioFormat.Builder()
            .setChannelMask(channelConfig)
            .setEncoding(audioEncodingFormat)
            .setSampleRate(sampleRateInHz)
            .build()

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(audioAttributes)
            .setAudioFormat(audioFormat)
            .setBufferSizeInBytes(bufferSizeInBytes)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
            .apply { play() }
    }

    /**
     * AudioTrackOutputUnit 객체 사용을 마친 후 리소스를 정리한다.
     */
    override fun release() {
        audioTrack.run {
            stop()
            release()
        }
    }

    /**
     * AudioTrack 객체를 사용해 outputBuffer에 들어있는 size 크기의 오디오 데이터를 소리로 출력한다.
     */
    override fun outputAudio(outputBuffer: ByteBuffer, size: Int) {
        audioTrack.write(outputBuffer, size, AudioTrack.WRITE_BLOCKING)
        outputBuffer.clear()
    }

    // ---------------------------------------------------------------------------------------------
    // AudioTrackOutputUnit 내부적으로 사용하는 private methods.
    /**
     * 채널 수에 대한 채널 마스크 Int 값을 반환한다.
     */
    private fun getChannelConfig(channelCount: Int): Int {
        return when (channelCount) {
            1 -> AudioFormat.CHANNEL_OUT_MONO
            2 -> AudioFormat.CHANNEL_OUT_STEREO
            3 -> AudioFormat.CHANNEL_OUT_STEREO or AudioFormat.CHANNEL_OUT_FRONT_CENTER
            4 -> AudioFormat.CHANNEL_OUT_QUAD
            5 -> AudioFormat.CHANNEL_OUT_QUAD or AudioFormat.CHANNEL_OUT_FRONT_CENTER
            6 -> AudioFormat.CHANNEL_OUT_5POINT1
            7 -> AudioFormat.CHANNEL_OUT_5POINT1 or AudioFormat.CHANNEL_OUT_BACK_CENTER
            8 -> AudioFormat.CHANNEL_OUT_7POINT1_SURROUND
            else -> throw IllegalArgumentException("Illegal channelCount argument : $channelCount.")
        }
    }
}