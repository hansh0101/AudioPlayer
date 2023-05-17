package com.vimosoft.audioplayer.model.audio_module

import android.media.AudioTrack
import android.media.MediaCodec
import android.media.MediaExtractor
import timber.log.Timber

/**
 * 오디오 파일 디코딩 및 재생만을 담당하는 Thread 객체.
 */
class AudioPlayerThread(
    /**
     * 미디어 파일을 읽어들일 MediaExtractor 객체.
     */
    private val mediaExtractor: MediaExtractor,
    /**
     * 압축된 오디오 파일을 디코딩할 MediaCodec(decoder) 객체.
     */
    private val mediaCodec: MediaCodec,
    /**
     * 디코딩된 오디오 파일을 디바이스 스피커로 출력할 AudioTrack 객체.
     */
    private val audioTrack: AudioTrack,
    /**
     * 오디오 재생을 마친 후 AudioPlayer 객체 내 audioPlayerThread를 null로 만들어줄 Callback.
     */
    private val onFinish: () -> Unit
) : Thread() {
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

        // MediaCodec에서 Input/Output buffer를 꺼내올 때 timeout의 기준이 되는 마이크로초.
        val timeOutUs = 10000L
        // MediaCodec의 버퍼에 대한 메타데이터.
        val bufferInfo = MediaCodec.BufferInfo()
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

            // MediaCodec에서 입력 데이터를 쓸 버퍼의 인덱스를 가져온다.
            val inputBufferIndex = mediaCodec.dequeueInputBuffer(timeOutUs)
            if (inputBufferIndex >= 0) {
                // MediaCodec에서 입력 데이터를 쓸 버퍼를 가져온다.
                val destinationBuffer = mediaCodec.getInputBuffer(inputBufferIndex) ?: break
                // MediaExtractor를 사용해 버퍼에 압축된 미디어 데이터 샘플을 저장하고, 크기를 sampleSize에 기록한다.
                var sampleSize = mediaExtractor.readSampleData(destinationBuffer, 0)
                // 현재 위치 마이크로초
                var presentationTimeUs = 0L
                // MediaCodec에 전달할 플래그
                var flag = 0

                // MediaExtractor로 읽어온 샘플의 크기가 0보다 작다면, 즉 읽지 못했다면?
                if (sampleSize < 0) {
                    isEOS = true
                    sampleSize = 0
                    flag = MediaCodec.BUFFER_FLAG_END_OF_STREAM
                } else {
                    presentationTimeUs = mediaExtractor.sampleTime
                }

                // MediaCodec에 입력 데이터를 쓴 버퍼를 넣는다.
                mediaCodec.queueInputBuffer(
                    inputBufferIndex,
                    0,
                    sampleSize,
                    presentationTimeUs,
                    flag
                )

                // EOS에 도달하지 않았다면 MediaExtractor가 미디어 데이터를 읽는 위치를 다음 샘플로 이동시킨다.
                if (!isEOS) {
                    mediaExtractor.advance()
                }
            }

            // MediaCodec에서 출력 데이터를 담은 버퍼의 인덱스를 가져온다.
            when (val outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, timeOutUs)) {
                MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    // The output format has changed, update any renderer
                    // configuration here if necessary
                }
                MediaCodec.INFO_TRY_AGAIN_LATER -> {
                    // No output available yet, wait for it
                }
                else -> {
                    // MediaCodec에서 출력 데이터를 담은 버퍼를 가져온다.
                    val buffer = mediaCodec.getOutputBuffer(outputBufferIndex)
                    // MediaCodec의 버퍼 크기만큼의 ByteArray를 만들고, 버퍼의 내용을 ByteArray로 옮긴다.
                    val chunk = ByteArray(bufferInfo.size)
                    buffer?.get(chunk)
                    buffer?.clear()

                    // ByteArray가 비어있지 않다면, AudioTrack을 사용해 디코딩된 출력 데이터를 재생한다.
                    if (chunk.isNotEmpty()) {
                        audioTrack.write(chunk, 0, chunk.size)
                    }

                    // MediaCodec으로 출력 데이터를 담은 버퍼를 반환한다.
                    mediaCodec.releaseOutputBuffer(outputBufferIndex, false)
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
        audioTrack.flush()
        mediaExtractor.seekTo(playbackPosition, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
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