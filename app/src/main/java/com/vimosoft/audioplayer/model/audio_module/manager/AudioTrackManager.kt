package com.vimosoft.audioplayer.model.audio_module.manager

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaFormat

class AudioTrackManager {
    companion object {
        fun createAudioTrack(mediaFormat: MediaFormat): AudioTrack {
            val sampleRateInHz = mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val channelConfig =
                when (val channelCount = mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)) {
                    1 -> AudioFormat.CHANNEL_OUT_MONO
                    2 -> AudioFormat.CHANNEL_OUT_STEREO
                    3 -> AudioFormat.CHANNEL_OUT_STEREO or AudioFormat.CHANNEL_OUT_FRONT_CENTER
                    4 -> AudioFormat.CHANNEL_OUT_QUAD
                    5 -> AudioFormat.CHANNEL_OUT_QUAD or AudioFormat.CHANNEL_OUT_FRONT_CENTER
                    6 -> AudioFormat.CHANNEL_OUT_5POINT1
                    7 -> AudioFormat.CHANNEL_OUT_5POINT1 or AudioFormat.CHANNEL_OUT_BACK_CENTER
                    8 -> AudioFormat.CHANNEL_OUT_7POINT1_SURROUND
                    else -> throw IllegalArgumentException("Illegal channelCount : $channelCount")
                }
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

            return AudioTrack.Builder()
                .setAudioAttributes(audioAttributes)
                .setAudioFormat(audioFormat)
                .setBufferSizeInBytes(bufferSizeInBytes)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build().apply {
                    play()
                }
        }
    }
}