package com.vimosoft.audioplayer

import android.content.Context
import android.media.*
import java.util.*

class AudioPlayer(private val context: Context) {
    // ---------------------------------------------------------------------------------------------
    // Media 재생과 관련된 Android 라이브러리 객체
    private var mediaExtractor: MediaExtractor? = null
    private var mediaCodec: MediaCodec? = null
    private var audioTrack: AudioTrack? = null

    // ---------------------------------------------------------------------------------------------
    // Audio 재생을 담당하는 스레드
    private var audioPlayerThread: AudioPlayerThread? = null

    // ---------------------------------------------------------------------------------------------
    // 현재 재생 중인가?
    var isPlaying: Boolean = false

    // 재생할 파일명
    private var fileName: String = ""

    // 트랙 인덱스
    private var trackIndex: Int = -1

    // 트랙 길이
    var duration: Long = 0L

    // 현재 재생 위치
    var playbackPosition: Long = 0L

    // playbackPosition 갱신을 위한 Timer 및 TimerTask
    private var timer: Timer? = null
    private val playbackPositionUpdater: TimerTask
        get() = object : TimerTask() {
            override fun run() {
                playbackPosition = audioPlayerThread?.playbackPosition ?: 0L
                if (audioPlayerThread?.isAlive == false) {
                    isPlaying = false
                    timer?.cancel()
                    timer = null
                    prepare()
                }
            }
        }

    // ---------------------------------------------------------------------------------------------
    // MediaExtractor, MediaCodec, AudioTrack 객체를 준비한다.
    fun prepare(fileName: String = "") {
        if (fileName != "") {
            this.fileName = fileName
        }

        configureMediaExtractor()
        configureMediaCodec()
        configureAudioTrack()
        configureAudioPlayerThread()
    }

    // 오디오 출력을 시작한다.
    fun play() {
        isPlaying = true
        if (audioPlayerThread?.isAlive != true) {
            configureAudioPlayerThread()
            audioPlayerThread?.start()
        } else {
            audioPlayerThread?.play()
        }

        timer = Timer()
        timer?.scheduleAtFixedRate(playbackPositionUpdater, 0, 100)
    }

    // 오디오 출력을 일시중지한다.
    fun pause() {
        isPlaying = false
        audioPlayerThread?.pause()

        timer?.cancel()
        timer = null
    }

    // 오디오 출력을 원하는 시점으로 설정한다.
    fun seek(playbackPosition: Long) {
        audioPlayerThread?.seek(playbackPosition)
    }

    // 오디오 재생을 종료하고 리소스를 해제한다.
    fun release() {
        mediaCodec?.stop()
        audioTrack?.stop()

        mediaCodec?.release()
        mediaExtractor?.release()
        audioTrack?.release()

        audioPlayerThread?.interrupt()
        audioPlayerThread = null
    }

    // ---------------------------------------------------------------------------------------------
    // MediaExtractor 객체를 구성한다.
    private fun configureMediaExtractor() {
        val assetFileDescriptor = context.assets.openFd(fileName)
        mediaExtractor = MediaExtractor().apply {
            setDataSource(assetFileDescriptor)
        }
        assetFileDescriptor.close()

        val trackCount = mediaExtractor!!.trackCount
        for (i in (trackIndex + 1) until trackCount) {
            val format = mediaExtractor!!.getTrackFormat(i)
            duration = format.getLong(MediaFormat.KEY_DURATION)
            val mimeType = format.getString(MediaFormat.KEY_MIME)
                ?: error("Error occurred when get MIME type from track format.")

            if (mimeType.startsWith("audio/")) {
                trackIndex = i
                break
            }
        }

        if (trackIndex in 0 until trackCount) {
            mediaExtractor!!.selectTrack(trackIndex)
        }
    }

    // MediaCodec 객체를 구성한다.
    private fun configureMediaCodec() {
        if (mediaExtractor == null) {
            return
        }

        val format = mediaExtractor!!.getTrackFormat(trackIndex)
        val mimeType = format.getString(MediaFormat.KEY_MIME)
            ?: error("Error occurred in ")

        mediaCodec = MediaCodec.createDecoderByType(mimeType).apply {
            configure(format, null, null, 0)
            start()
        }
    }

    // AudioTrack 객체를 구성한다.
    private fun configureAudioTrack() {
        if (mediaExtractor == null) {
            return
        }

        val format = mediaExtractor!!.getTrackFormat(trackIndex)
        val sampleRateInHz = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val channelConfig =
            when (val channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)) {
                1 -> AudioFormat.CHANNEL_OUT_MONO
                2 -> AudioFormat.CHANNEL_OUT_STEREO
                3 -> AudioFormat.CHANNEL_OUT_STEREO or AudioFormat.CHANNEL_OUT_FRONT_CENTER
                4 -> AudioFormat.CHANNEL_OUT_QUAD
                5 -> AudioFormat.CHANNEL_OUT_QUAD or AudioFormat.CHANNEL_OUT_FRONT_CENTER
                6 -> AudioFormat.CHANNEL_OUT_5POINT1
                7 -> AudioFormat.CHANNEL_OUT_5POINT1 or AudioFormat.CHANNEL_OUT_BACK_CENTER
                8 -> AudioFormat.CHANNEL_OUT_7POINT1_SURROUND
                else -> throw IllegalArgumentException("Illegal channelCount argument : $channelCount")
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

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(audioAttributes)
            .setAudioFormat(audioFormat)
            .setBufferSizeInBytes(bufferSizeInBytes)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build().apply {
                play()
            }
    }

    // AudioPlayerThread 객체를 구성한다.
    private fun configureAudioPlayerThread() {
        if (mediaExtractor == null || mediaCodec == null || audioTrack == null) {
            return
        }

        audioPlayerThread = AudioPlayerThread(mediaExtractor!!, mediaCodec!!, audioTrack!!)
    }
}