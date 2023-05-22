package com.vimosoft.audioplayer.model

import android.content.Context
import android.media.MediaFormat
import com.vimosoft.audioplayer.model.manager.AudioDecodeProcessor
import com.vimosoft.audioplayer.model.manager.AudioInputUnit
import com.vimosoft.audioplayer.model.manager.AudioOutputUnit
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
    @Volatile
    var isPlaying: Boolean = false
        private set

    /**
     * 오디오 파일의 길이(단위 - microsecond) 값.
     */
    var duration: Long = 0L
        private set

    /**
     * 현재 재생 중인 위치(단위 - microsecond) 값.
     */
    @Volatile
    var playbackPosition: Long = 0L
        private set

    // ---------------------------------------------------------------------------------------------
    // 오디오 파일 추출, 디코딩, 재생을 담당하는 private instance variables.

    /**
     * MediaExtractor 초기화, 해제, 미디어 파일 추출, 재생 위치 조정 등의 작업을 담당하는 MediaExtractorManager 객체.
     */
    private val audioInputUnit: AudioInputUnit = AudioInputUnit()

    /**
     * MediaCodec 초기화, 해제, 입출력 버퍼 제공 및 회수, 인코딩/디코딩 등의 작업을 담당하는 MediaCodecManager 객체.
     */
    private val audioDecodeProcessor: AudioDecodeProcessor = AudioDecodeProcessor()

    /**
     * AudioTrack 초기화, 해제, 소리 출력 등의 작업을 담당하는 AudioTrackManager 객체.
     */
    private val audioOutputUnit: AudioOutputUnit = AudioOutputUnit()

    /**
     * 오디오를 재생하는 스레드를
     */
    private val lock = Object()
    private var audioThread: Thread? = null

    // ---------------------------------------------------------------------------------------------
    // 오디오 재생에 필요한 private variables.

    /**
     * 재생할 오디오 파일의 이름을 나타내는 String 값.
     */
    private var fileName: String = ""

    /**
     * 미디어 파일의 포맷.
     */
    private lateinit var mediaFormat: MediaFormat

    // ---------------------------------------------------------------------------------------------
    // AudioPlayer가 외부에 제공하는 public methods.

    /**
     * 오디오 재생을 위한 리소스를 준비한다.
     */
    fun prepare(fileName: String = "") {
        // 재생할 오디오 파일의 이름을 설정한다.
        if (fileName != "") {
            this.fileName = fileName
        }

        // MediaExtractorManager를 통해 MediaExtractor 객체를 구성한다.
        mediaFormat =
            audioInputUnit.configure(context.assets.openFd(this.fileName), "audio/")
        duration = mediaFormat.getLong(MediaFormat.KEY_DURATION)

        // MediaCodecManager를 통해 MediaCodec 객체를 구성한다.
        audioDecodeProcessor.configure(mediaFormat)

        // AudioTrackManager를 통해 AudioTrack 객체를 구성한다.
        audioOutputUnit.configure(mediaFormat)
    }

    /**
     * 오디오 재생을 마친 후 리소스를 정리한다.
     */
    fun release() {
        audioInputUnit.release()
        audioDecodeProcessor.release()
        audioOutputUnit.release()
        audioThread?.interrupt()
        audioThread = null
    }

    /**
     * 오디오 재생을 시작한다.
     */
    fun play() {
        if (audioThread == null || audioThread?.isInterrupted == true) {
            configureAudioThread()
        }

        if (!isPlaying) {
            synchronized(lock) {
                isPlaying = true
                lock.notify()
            }
        }
    }

    /**
     * 오디오 재생을 중지한다.
     */
    fun pause() {
        if (isPlaying) {
            isPlaying = false
        }
    }

    /**
     * 재생 위치를 조정한다.
     */
    fun seek(playbackPosition: Long) {
        audioOutputUnit.flush()
        audioDecodeProcessor.flush()
        audioInputUnit.seekTo(playbackPosition)
    }

    // ---------------------------------------------------------------------------------------------
    // AudioPlayer 내부적으로 사용하는 private methods.

    /**
     * 오디오 파일 추출, 디코딩, 재생을 처리하는 Thread 객체를 구성한다.
     */
    private fun configureAudioThread() {
        audioThread = Thread {
            // Input data가 EOS에 도달했는지를 나타내는 Boolean 값.
            var isInputEOSReached = false
            // Output data가 EOS에 도달했는지를 나타내는 Boolean 값.
            var isOutputEOSReached = false

            while (!Thread.currentThread().isInterrupted) {
                // 일시정지 상태라면 재생 상태가 될 때까지 Thread의 State를 WAITING으로 만든다.
                try {
                    waitForPlayback()
                } catch (exception: InterruptedException) {
                    Timber.e(exception)
                    return@Thread
                }

                // Input data가 EOS에 도달하지 않았다면 디코딩을 요청한다. (입력 -> 처리 단계)
                if (!isInputEOSReached) {
                    isInputEOSReached = requestDecodeUntilEOS()
                }
                // OutputData가 EOS에 도달하지 않았다면 디코딩 결과를 처리한다. (처리 -> 출력 단계)
                if (!isOutputEOSReached) {
                    isOutputEOSReached = handleDecodeResultUntilEOS()
                }

                // Input, Output data 모두 EOS에 도달했다면 다음 트랙 재생을 준비한다.
                if (isInputEOSReached && isOutputEOSReached) {
                    prepareNextTrack()
                    isInputEOSReached = false
                    isOutputEOSReached = false
                }
            }
        }.apply { start() }
    }

    /**
     * 일시정지 시 오디오를 재생하는 스레드를 WAITING 상태로 만든다.
     */
    private fun waitForPlayback() {
        synchronized(lock) {
            if (!isPlaying) {
                lock.wait()
            }
        }
    }

    /**
     * MediaCodecManager에 오디오 파일 디코딩을 요청한다.
     */
    private fun requestDecodeUntilEOS(): Boolean {
        val inputBufferInfo = audioDecodeProcessor.assignInputBuffer()
        if (inputBufferInfo.buffer != null) {
            val extractionResult = audioInputUnit.extract(inputBufferInfo.buffer)
            audioDecodeProcessor.submitInputBuffer(
                inputBufferInfo.bufferIndex,
                0,
                extractionResult.sampleSize,
                extractionResult.presentationTimeUs
            )

            if (extractionResult.sampleSize < 0) {
                return true
            }
        }
        return false
    }

    /**
     * MediaCodecManager가 수행한 디코딩 결과를 처리한다.
     */
    private fun handleDecodeResultUntilEOS(): Boolean {
        val outputBufferInfo = audioDecodeProcessor.getOutputBuffer()
        if (outputBufferInfo.buffer != null) {
            audioOutputUnit.outputAudio(outputBufferInfo.buffer, outputBufferInfo.info.size)
            audioDecodeProcessor.giveBackOutputBuffer(outputBufferInfo.bufferIndex, false)

            playbackPosition = outputBufferInfo.info.presentationTimeUs
            return outputBufferInfo.isEOS
        }
        return false
    }

    /**
     * 다음 트랙 재생을 준비한다.
     */
    private fun prepareNextTrack() {
        audioInputUnit.release()
        audioDecodeProcessor.release()
        audioOutputUnit.release()
        prepare()
        isPlaying = false
        playbackPosition = 0
    }
}