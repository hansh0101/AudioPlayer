package com.vimosoft.audioplayer.model.audio_module.manager

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaFormat

class AudioTrackManager {
    // ---------------------------------------------------------------------------------------------
    // class variables.
    private lateinit var _audioTrack: AudioTrack
    val audioTrack: AudioTrack get() = _audioTrack

    // ---------------------------------------------------------------------------------------------
    // public interfaces.
    fun configureAudioTrack(mediaFormat: MediaFormat) {
        val sampleRateInHz = mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val channelConfig = getChannelConfig(mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT))
        val audioEncodingFormat = AudioFormat.ENCODING_PCM_16BIT
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

        _audioTrack = AudioTrack.Builder()
            .setAudioAttributes(audioAttributes)
            .setAudioFormat(audioFormat)
            .setBufferSizeInBytes(bufferSizeInBytes)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
            .apply {
                play()
            }
    }

    fun release() {
        _audioTrack.run {
            stop()
            release()
        }
    }

    // ---------------------------------------------------------------------------------------------
    // private methods.
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