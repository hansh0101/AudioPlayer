package com.vimosoft.audioplayer.model.audio_module

import android.content.Context
import android.media.*
import timber.log.Timber

/**
 * 오디오 파일을 재생하는 객체.
 */
class AudioPlayer(private val context: Context) {
    // ---------------------------------------------------------------------------------------------
    // 오디오 재생에 관한 상태를 나타내는 public variables.
    // 외부에서는 getter만 사용 가능하다.

    /**
     * 현재 AudioPlayer가 재생중인지를 나타내는 Boolean 값.
     */
    val isPlaying: Boolean get() = audioPlayerThread?.isPaused == false

    /**
     * 오디오 파일의 길이(단위 - microsecond) 값.
     */
    var duration: Long = 0L
        private set

    /**
     * 현재 재생 중인 위치(단위 - microsecond) 값.
     */
    val playbackPosition: Long get() = audioPlayerThread?.playbackPosition ?: 0L

    // ---------------------------------------------------------------------------------------------
    // 오디오 파일 추출, 디코딩, 재생을 담당하는 private instance variables.

    /**
     * 미디어 파일을 읽어들일 MediaExtractor 객체.
     */
    private var mediaExtractor: MediaExtractor? = null

    /**
     * 압축된 오디오 파일을 디코딩할 MediaCodec(decoder) 객체.
     */
    private var mediaCodec: MediaCodec? = null

    /**
     * 디코딩된 오디오 파일을 디바이스 스피커로 출력할 AudioTrack 객체.
     */
    private var audioTrack: AudioTrack? = null

    /**
     * 오디오 디코딩 및 재생만을 담당하는 AudioPlayerThread 객체.
     */
    private var audioPlayerThread: AudioPlayerThread? = null

    // ---------------------------------------------------------------------------------------------
    // 오디오 재생에 필요한 private variables.

    /**
     * 재생할 오디오 파일의 이름을 나타내는 String 값.
     */
    private var fileName: String = ""

    // ---------------------------------------------------------------------------------------------
    // AudioPlayer가 외부에 제공하는 public methods.

    /**
     * 오디오 재생을 위한 리소스를 준비한다.
     */
    fun prepare(fileName: String = "") {
        runCatching {
            if (fileName != "") {
                this.fileName = fileName
            }

            // MediaExtractor 객체를 구성한다.
            configureMediaExtractor()

            // MediaExtractor 객체를 통해 오디오 트랙의 인덱스를 계산한다.
            val trackIndex = getTrackIndex()
            // MediaExtractor가 추출할 트랙의 MediaFormat
            val format = mediaExtractor!!.getTrackFormat(trackIndex)
            // trackIndex가 유효하다면 해당 인덱스로 MediaExtractor가 추출할 트랙을 설정한다.
            mediaExtractor?.selectTrack(trackIndex)

            // MediaCodec 객체를 구성한다.
            configureMediaCodec(format)
            // AudioTrack 객체를 구성한다.
            configureAudioTrack(format)
        }.onFailure { Timber.e(it) }
    }

    /**
     * 오디오 재생을 시작한다.
     */
    fun play() {
        runCatching {
            if (audioPlayerThread == null || audioPlayerThread?.isInterrupted == true) {
                configureAudioPlayerThread()
            }
            audioPlayerThread?.play()
        }.onFailure { Timber.e(it) }
    }

    /**
     * 오디오 재생을 중지한다.
     */
    fun pause() {
        runCatching {
            audioPlayerThread?.pause()
        }.onFailure { Timber.e(it) }
    }

    /**
     * 재생 위치를 조정한다.
     */
    fun seek(playbackPosition: Long) {
        runCatching {
            audioPlayerThread?.seek(playbackPosition)
        }.onFailure { Timber.e(it) }
    }

    /**
     * 오디오 재생을 마친 후 리소스를 정리한다.
     */
    fun release() {
        runCatching {
            mediaCodec?.stop()
            audioTrack?.stop()

            mediaCodec?.release()
            mediaExtractor?.release()
            audioTrack?.release()

            audioPlayerThread?.interrupt()
            audioPlayerThread = null
        }.onFailure { Timber.e(it) }
    }

    // ---------------------------------------------------------------------------------------------
    // AudioPlayer 내부에서만 사용되는 private methods.

    /**
     * MediaExtractor 객체를 구성한다.
     */
    private fun configureMediaExtractor() {
        val assetFileDescriptor = context.assets.openFd(fileName)
        mediaExtractor = MediaExtractor().apply {
            setDataSource(assetFileDescriptor)
        }
        assetFileDescriptor.close()
    }

    /**
     * MediaCodec 객체를 구성한다.
     */
    private fun configureMediaCodec(format: MediaFormat) {
        if (mediaExtractor == null) {
            return
        }

        val mimeType = format.getString(MediaFormat.KEY_MIME)
        mediaCodec = MediaCodec.createDecoderByType(mimeType!!).apply {
            configure(format, null, null, 0)
            start()
        }
    }

    /**
     * AudioTrack 객체를 구성한다.
     */
    private fun configureAudioTrack(format: MediaFormat) {
        if (mediaExtractor == null) {
            return
        }

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

    /**
     * AudioPlayerThread 객체를 구성한다.
     */
    private fun configureAudioPlayerThread() {
        if (mediaExtractor == null || mediaCodec == null || audioTrack == null) {
            return
        }

        audioPlayerThread = AudioPlayerThread(mediaExtractor!!, mediaCodec!!, audioTrack!!) {
            audioPlayerThread = null
            release()
            prepare()
        }
    }

    /**
     * AudioTrack의 인덱스를 반환한다.
     */
    private fun getTrackIndex(): Int {
        if (mediaExtractor == null) {
            return -1
        }

        var trackIndex = -1
        val trackCount = mediaExtractor!!.trackCount
        for (i in 0 until trackCount) {
            val format = mediaExtractor!!.getTrackFormat(i)
            duration = format.getLong(MediaFormat.KEY_DURATION)
            val mimeType = format.getString(MediaFormat.KEY_MIME)
                ?: error("Error occurred when get MIME type from track format.")

            if (mimeType.startsWith("audio/")) {
                trackIndex = i
                break
            }
        }

        return trackIndex
    }
}