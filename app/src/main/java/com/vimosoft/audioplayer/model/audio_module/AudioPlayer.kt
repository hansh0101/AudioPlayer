package com.vimosoft.audioplayer.model.audio_module

import android.content.Context
import android.media.AudioTrack
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import com.vimosoft.audioplayer.model.audio_module.manager.AudioTrackManager
import com.vimosoft.audioplayer.model.audio_module.manager.MediaCodecManager
import com.vimosoft.audioplayer.model.audio_module.manager.MediaExtractorManager
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
    val isPlaying: Boolean get() = audioPlayerThread?.isPlaying == true

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
    private lateinit var mediaExtractor: MediaExtractor

    /**
     * 압축된 오디오 파일을 디코딩할 MediaCodec(decoder) 객체.
     */
    private lateinit var mediaCodec: MediaCodec

    /**
     * 디코딩된 오디오 파일을 디바이스 스피커로 출력할 AudioTrack 객체.
     */
    private lateinit var audioTrack: AudioTrack

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

    /**
     * 미디어 파일의 포맷
     */
    private lateinit var mediaFormat: MediaFormat

    /**
     * 미디어 파일 내에서 재생할 트랙의 인덱스 값.
     */
    private var trackIndex: Int = 0

    // ---------------------------------------------------------------------------------------------
    // AudioPlayer가 외부에 제공하는 public methods.

    /**
     * 오디오 재생을 위한 리소스를 준비한다.
     */
    fun prepare(fileName: String = "") {
        // fileName을 설정한다.
        if (fileName != "") {
            this.fileName = fileName
        }

        // MediaExtractor 객체를 구성한다.
        runCatching {
            MediaExtractorManager.createAudioExtractor(context, this.fileName, "audio/")
        }.onSuccess { mediaExtractorInfo ->
            mediaExtractor = mediaExtractorInfo.mediaExtractor
            trackIndex = mediaExtractorInfo.trackIndex

            mediaFormat = mediaExtractor.getTrackFormat(trackIndex)
            duration = mediaFormat.getLong(MediaFormat.KEY_DURATION)
        }.onFailure {
            Timber.e(it)
        }

        // MediaCodec 객체를 구성한다.
        runCatching {
            MediaCodecManager.createAudioDecoder(mediaFormat)
        }.onSuccess { mediaCodecDecoder ->
            this.mediaCodec = mediaCodecDecoder
        }.onFailure {
            Timber.e(it)
        }

        // AudioTrack 객체를 구성한다.
        runCatching {
            AudioTrackManager.createAudioTrack(mediaFormat)
        }.onSuccess { audioTrack ->
            this.audioTrack = audioTrack
        }.onFailure {
            Timber.e(it)
        }
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
            mediaCodec.stop()
            audioTrack.stop()

            mediaCodec.release()
            mediaExtractor.release()
            audioTrack.release()

            audioPlayerThread?.interrupt()
            audioPlayerThread = null
        }.onFailure { Timber.e(it) }
    }

    /**
     * AudioPlayerThread 객체를 구성한다.
     */
    private fun configureAudioPlayerThread() {
        audioPlayerThread = AudioPlayerThread(mediaExtractor, mediaCodec, audioTrack) {
            audioPlayerThread = null
            release()
            prepare()
        }
    }
}