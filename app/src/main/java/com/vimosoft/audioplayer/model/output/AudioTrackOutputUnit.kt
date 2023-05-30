package com.vimosoft.audioplayer.model.output

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaFormat
import java.nio.ByteBuffer

/**
 * 소리를 출력하기 위해 AudioTrack을 통해 PCM 데이터를 재생하는 작업을 전반적으로 담당하는 객체.
 */
class AudioTrackOutputUnit : AudioOutputUnit() {
    // ---------------------------------------------------------------------------------------------
    // AudioTrackManager 사용에 필요한 private variables.

    /**
     * 소리를 출력하는 AudioTrack 객체.
     */
    private lateinit var audioTrack: AudioTrack

    // ---------------------------------------------------------------------------------------------
    // AudioTrackManager가 외부에 제공하는 public methods.

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
     * AudioTrackManager 사용을 마친 후 리소스를 정리한다.
     */
    override fun release() {
        audioTrack.run {
            stop()
            release()
        }
    }

    /**
     * AudioTrack 객체를 사용해 버퍼의 데이터를 소리로 출력한다.
     */
    override fun outputAudio(outputBuffer: ByteBuffer, size: Int) {
        audioTrack.write(outputBuffer, size, AudioTrack.WRITE_BLOCKING)
        outputBuffer.clear()
    }

    // ---------------------------------------------------------------------------------------------
    // AudioTrackManager 내부적으로 사용하는 private methods.

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