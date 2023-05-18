package com.vimosoft.audioplayer.model.audio_module

import android.media.MediaCodec
import com.vimosoft.audioplayer.model.audio_module.manager.AudioTrackManager
import com.vimosoft.audioplayer.model.audio_module.manager.MediaDecoderManager
import com.vimosoft.audioplayer.model.audio_module.manager.MediaExtractorManager
import timber.log.Timber

/**
 * 오디오 파일 디코딩 및 재생만을 담당하는 Thread 객체.
 */
class AudioPlayerThread(
    /**
     * 미디어 파일을 읽어들일 MediaExtractorManager 객체.
     */
    private val mediaExtractorManager: MediaExtractorManager,
    /**
     * 압축된 오디오 파일을 디코딩할 MediaCodec(decoder)Manager 객체.
     */
    private val mediaDecoderManager: MediaDecoderManager,
    /**
     * 디코딩된 오디오 파일을 디바이스 스피커로 출력할 AudioTrackManager 객체.
     */
    private val audioTrackManager: AudioTrackManager,
    /**
     * 오디오 재생을 마친 후 AudioPlayer 객체 내 audioPlayerThread를 null로 만들어줄 Callback.
     */
    private val onFinish: () -> Unit
) : Thread() {
    // TODO - 나중에 지우긴 해야 하는데 일단 테스트를 위해 빼둔다.
    private val mediaExtractor get() = mediaExtractorManager.mediaExtractor

    // ---------------------------------------------------------------------------------------------
    // 현재 오디오 재생에 관한 상태를 나타내는 public variables.
    // 외부에서는 getter만 사용 가능하다.

    /**
     * 현재 재생 중인 위치(단위 - microsecond) 값.
     */
    @Volatile
    var playbackPosition: Long = 0L
        private set

    /**
     * 현재 AudioPlayerThread가 일시정지인지를 나타내는 Boolean 값.
     */
    @Volatile
    var isPlaying: Boolean = false
        private set

    // ---------------------------------------------------------------------------------------------
    // 오디오 스레드 실행 제어에 필요한 private instance variables.

    /**
     * 현재 AudioPlayer가 run()을 실행하기 시작했는지를 나타내는 Boolean 값.
     */
    private var isStarted: Boolean = false

    /**
     * Thread 제어를 위한 Object 객체.
     */
    private val lock = Object()

    // ---------------------------------------------------------------------------------------------
    // AudioPlayerThread가 외부에 제공하는 public methods.

    /**
     * 'MediaExtractor를 통한 미디어 파일 추출 -> MediaCodec을 통한 디코딩 -> AudioTrack을 통한 디바이스 스피커 출력'을 수행한다.
     */
    override fun run() {
        isStarted = true

        // End-Of-Stream에 도달했는지?
        var isEOS = false

        // 미디어 파일을 디코딩해 재생한다.
        // 미디어 파일의 EOS에 도달하지 않았고, 스레드가 인터럽트되지 않은 경우 수행한다.
        while (!isEOS && !isInterrupted) {
            // 일시정지를 위한 synchronized 블록
            synchronized(lock) {
                while (!isPlaying) {
                    try {
                        lock.wait()
                    } catch (exception: InterruptedException) {
                        Timber.e(exception)
                        interrupt()
                        return
                    }
                }
            }

            // 입력 처리
            val inputBufferInfo = mediaDecoderManager.getInputBuffer()
            if (inputBufferInfo.buffer != null) {
                val extractionResult =
                    mediaExtractorManager.extract(inputBufferInfo.buffer)
                if (extractionResult.sampleSize < 0) {
                    isEOS = true
                }
                mediaDecoderManager.setInputBuffer(
                    inputBufferInfo.bufferIndex,
                    0,
                    extractionResult.sampleSize,
                    extractionResult.presentationTimeUs
                )
            }

            // 출력 처리
            val outputBufferInfo = mediaDecoderManager.getOutputBuffer()
            when (outputBufferInfo.bufferIndex) {
                MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {}
                MediaCodec.INFO_TRY_AGAIN_LATER -> {}
                else -> {
                    if (outputBufferInfo.buffer != null) {
                        audioTrackManager.outputAudio(
                            outputBufferInfo.buffer,
                            outputBufferInfo.size
                        )
                    }
                    mediaDecoderManager.setOutputBuffer(outputBufferInfo.bufferIndex, false)
                }
            }

            // playbackPosition 갱신
            playbackPosition = mediaExtractor.sampleTime
        }

        // 종료 후 onFinsh 콜백 메서드 호출
        onFinish()
    }

    override fun interrupt() {
        super.interrupt()
        onFinish()
    }

    /**
     * 오디오 재생을 시작한다.
     */
    fun play() {
        if (!isStarted) {
            start()
        }
        resumeThread()
    }

    /**
     * 오디오 재생을 중지한다.
     */
    fun pause() {
        pauseThread()
    }

    /**
     * 재생 위치를 조정한다.
     */
    fun seek(playbackPosition: Long) {
        audioTrackManager.flush()
        mediaDecoderManager.flush()
        mediaExtractorManager.seekTo(playbackPosition)
    }

    // ---------------------------------------------------------------------------------------------
    // AudioPlayerThread 내부에서만 사용되는 private methods.

    /**
     * Thread 실행을 재개한다.
     */
    private fun resumeThread() {
        synchronized(lock) {
            isPlaying = true
            lock.notify()
        }
    }

    /**
     * Thread 실행을 일시정지한다.
     */
    private fun pauseThread() {
        synchronized(lock) {
            isPlaying = false
        }
    }
}