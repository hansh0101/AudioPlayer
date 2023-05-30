package com.vimosoft.audioplayer.model

import android.content.Context
import android.media.MediaFormat
import com.vimosoft.audioplayer.model.decode.AudioDecodeProcessor
import com.vimosoft.audioplayer.model.decode.MediaCodecDecodeProcessor
import com.vimosoft.audioplayer.model.input.AudioInputUnit
import com.vimosoft.audioplayer.model.input.MediaExtractorInputUnit
import com.vimosoft.audioplayer.model.output.AudioOutputUnit
import com.vimosoft.audioplayer.model.output.AudioTrackOutputUnit
import com.vimosoft.audioplayer.model.output.OboeOutputUnit

/**
 * 오디오 파일을 재생하는 객체.
 */
class AudioPlayer(
    private val context: Context,
    inputType: Int,
    decodeType: Int,
    outputType: Int,
    private val onConfigure: (String, String, String, Int) -> Unit,
    private val onInputFileReady: (Int) -> Unit,
    private val onPlay: (Int) -> Unit,
    private val onPause: () -> Unit
) {
    // ---------------------------------------------------------------------------------------------
    // 오디오 재생에 관한 상태를 나타내는 public variables.
    // 외부에서는 getter만 사용 가능하다.
    /**
     * 현재 AudioPlayer가 재생중인지를 나타내는 Boolean 값.
     */
    val isPlaying: Boolean get() = audioThread?.isAlive == true

    // ---------------------------------------------------------------------------------------------
    // 오디오 파일 추출, 디코딩, 재생을 담당하는 private instance variables.
    /**
     * 오디오 파일 입력을 담당하는 AudioInputUnit 객체.
     */
    private val audioInputUnit: AudioInputUnit = when (inputType) {
        MEDIA_EXTRACTOR -> MediaExtractorInputUnit()
        else -> throw IllegalArgumentException("Illegal inputType argument.")
    }

    /**
     * 오디오 파일 처리(디코딩)을 담당하는 AudioDecodeProcessor 객체.
     */
    private val audioDecodeProcessor: AudioDecodeProcessor = when (decodeType) {
        MEDIA_CODEC -> MediaCodecDecodeProcessor()
        else -> throw IllegalArgumentException("Illegal decodeType argument.")
    }

    /**
     * 오디오 파일 출력을 담당하는 AudioOutputUnit 객체.
     */
    private val audioOutputUnit: AudioOutputUnit = when (outputType) {
        AUDIO_TRACK -> AudioTrackOutputUnit()
        OBOE -> OboeOutputUnit()
        else -> throw IllegalArgumentException("Illegal outputType argument.")
    }

    /**
     * 오디오를 재생하는 Thread 객체.
     */
    private var audioThread: Thread? = null

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
        // 재생할 오디오 파일의 이름을 설정한다.
        if (fileName != "") {
            this.fileName = fileName
        }

        // AudioInputUnit을 구성하고, 재생할 압축된 오디오 데이터의 MediaFormat을 가져온다.
        val inputMediaFormat =
            audioInputUnit.configure(context.assets.openFd(this.fileName), "audio/")

        // 입력 오디오 데이터의 MediaFormat을 활용해 AudioDecodeProcessor를 구성하고, 출력할 디코딩된 오디오 데이터의 MediaFormat을 가져온다.
        val outputMediaFormat = audioDecodeProcessor.configure(inputMediaFormat)

        // 출력 오디오 데이터의 MediaFormat을 활용해 AudioOutputUnit을 구성한다.
        audioOutputUnit.configure(outputMediaFormat)

        // 입력, 처리, 출력 객체를 구성한 후 Callback을 통해 AudioPlayer의 정보를 Client 측에 전달한다.
        val duration = (inputMediaFormat.getLong(MediaFormat.KEY_DURATION) / 1000 / 1000).toInt()
        onConfigure(audioInputUnit.name, audioDecodeProcessor.name, audioOutputUnit.name, duration)
        onInputFileReady(duration)
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
            startAudioThread()
        }
    }

    /**
     * 오디오 재생을 중지한다.
     */
    fun pause() {
        audioThread?.interrupt()
        audioThread = null
        onPause()
    }

    /**
     * 재생 위치를 조정한다.
     */
    fun seek(playbackPosition: Long) {
        audioDecodeProcessor.flush()
        audioInputUnit.seekTo(playbackPosition)
    }

    // ---------------------------------------------------------------------------------------------
    // AudioPlayer 내부적으로 사용하는 private methods.
    /**
     * 오디오 파일 추출, 디코딩, 재생을 처리하는 Thread 객체를 구성한다.
     */
    private fun startAudioThread() {
        audioThread = Thread {
            // Input data가 EOS에 도달했는지를 나타내는 Boolean 값.
            var isInputEOSReached = false
            // Output data가 EOS에 도달했는지를 나타내는 Boolean 값.
            var isOutputEOSReached = false

            while (!Thread.currentThread().isInterrupted) {
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
     * AudioDecodeProcessor에 압축된 오디오 파일의 디코딩을 요청한다.
     */
    private fun requestDecodeUntilEOS(): Boolean {
        // AudioDecodeProcessor를 통해 입력 버퍼를 가져온다.
        val inputBufferInfo = audioDecodeProcessor.assignInputBuffer()

        if (inputBufferInfo.buffer != null) {
            // AudioInputUnit을 통해 압축된 오디오 파일의 일부분을 읽어온다(입력).
            val extractionResult = audioInputUnit.extract(inputBufferInfo.buffer)
            // 읽어온 압축된 오디오 데이터 처리(디코딩)를 AudioDecodeProcessor에 요청한다.
            audioDecodeProcessor.submitInputBuffer(
                inputBufferInfo.bufferIndex,
                0,
                extractionResult.sampleSize,
                extractionResult.presentationTimeUs
            )

            // 읽어온 압축된 오디오 데이터 크기가 음수라면 입력 EOS에 도달했다는 의미이므로 true를 반환한다.
            if (extractionResult.sampleSize < 0) {
                return true
            }
        }
        // 입력 EOS에 도달하지 않았다면 false를 반환한다.
        return false
    }

    /**
     * MediaCodecManager가 수행한 디코딩 결과를 처리한다.
     */
    private fun handleDecodeResultUntilEOS(): Boolean {
        // AudioDecodeProcessor를 통해 출력 버퍼를 가져온다.
        val outputBufferInfo = audioDecodeProcessor.getOutputBuffer()

        if (outputBufferInfo.buffer != null) {
            // AudioOutputUnit을 통해 디코딩된 오디오 데이터 재생(출력)을 요청한다.
            audioOutputUnit.outputAudio(outputBufferInfo.buffer, outputBufferInfo.info.size)
            // 디코딩된 오디오 데이터가 든 출력 버퍼를 AudioDecodeProcessor에 반환한다.
            audioDecodeProcessor.giveBackOutputBuffer(outputBufferInfo.bufferIndex, false)

            // Thread가 interrupt되지 않았다면 onPlay 콜백을 통해 현재 재생 위치를 전달한다.
            if (!Thread.currentThread().isInterrupted) {
                onPlay((outputBufferInfo.info.presentationTimeUs / 1000 / 1000).toInt())
            }

            // 디코딩된 오디오 데이터가 EOS에 도달했는지에 해당하는 OutputBufferInfo.isEOS 값을 반환한다.
            return outputBufferInfo.isEOS
        }
        // 출력 EOS에 도달하지 않았다면 false를 반환한다.
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
    }

    companion object {
        const val MEDIA_EXTRACTOR = 1
        const val MEDIA_CODEC = 11
        const val AUDIO_TRACK = 21
        const val OBOE = 22
    }
}