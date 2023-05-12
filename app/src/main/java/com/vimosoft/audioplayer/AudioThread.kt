package com.vimosoft.audioplayer

import android.content.Context
import android.media.*
import timber.log.Timber

class AudioThread(
    private val context: Context,
    private var isSeek: Boolean = false,
    var playbackPosition: Long = 0L
) : Thread() {
    // ---------------------------------------------------------------------------------------------
    // AudioThread 동작에 필요한 MediaCodec, MediaExtractor, AudioTrack 변수들
    private var audioTrack: AudioTrack? = null
    private var extractor: MediaExtractor? = null
    private var codec: MediaCodec? = null

    // ---------------------------------------------------------------------------------------------
    // Audio 재생 컨트롤에 관한 변수들
    var duration = 0L

    // ---------------------------------------------------------------------------------------------
    // Thread의 동작을 정의한다.
    override fun run() {
        // 1 - MediaExtractor 객체 생성
        configureMediaExtractor()
        Timber.tag(TAG).i("1 - MediaExtractor 객체 구성")

        // 2 - MediaCodec 객체 생성
        configureMediaCodec()
        Timber.tag(TAG).i("2 - MediaCodec 객체 구성")

        // 3 - AudioTrack 객체 생성
        configureAudioTrack()
        Timber.tag(TAG).i("3 - AudioTrack 객체 구성")

        // 4 - 오디오 재생 시작
        audioTrack?.play()
        Timber.tag(TAG).i("4 - 오디오 재생 시작")

        // 5 - MediaCodec을 통해 음원 디코딩 및 AudioTrack을 통해 음원 재생
        decodeAndPlay()
        Timber.tag(TAG).i("5 - MediaCodec 통해 음원 디코딩 및 AudioTrack 통해 음원 재생")

        // 6 - 오디오 재생 중지
        codec?.stop()
        audioTrack?.stop()
        Timber.tag(TAG).i("6 - 오디오 재생 중지")

        // 7 - 리소스 해제
        codec?.release()
        extractor?.release()
        audioTrack?.release()
        Timber.tag(TAG).i("7 - 리소스 해제")
    }

    // ---------------------------------------------------------------------------------------------
    // 1 - MediaExtractor 객체를 생성한다.
    private fun configureMediaExtractor() {
        val assetFileDescriptor = context.assets.openFd(FILE_NAME)
        extractor = MediaExtractor().apply {
            setDataSource(assetFileDescriptor)
        }
        assetFileDescriptor.close()
    }

    // ---------------------------------------------------------------------------------------------
    // 2 - MediaCodec 객체를 생성한다.
    private fun configureMediaCodec() {
        if (extractor == null) {
            return
        }

        if ((extractor!!.trackCount) > 0) {
            val format = extractor!!.getTrackFormat(0)
            val mime = format.getString(MediaFormat.KEY_MIME)
                ?: error("Error occurred when get MIME from track format.")

            codec = MediaCodec.createDecoderByType(mime).apply {
                configure(format, null, null, 0)
                start()
            }
            extractor?.selectTrack(0)
        }
    }

    // ---------------------------------------------------------------------------------------------
    // 3 - AudioTrack 객체를 생성한다.
    private fun configureAudioTrack() {
        if (extractor == null) {
            return
        }

        if ((extractor!!.trackCount) > 0) {
            val format = extractor!!.getTrackFormat(0)
            duration = format.getLong(MediaFormat.KEY_DURATION)

            val sampleRateInHz = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val channelConfig = when (format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)) {
                1 -> AudioFormat.CHANNEL_OUT_MONO
                2 -> AudioFormat.CHANNEL_OUT_STEREO
                else -> AudioFormat.CHANNEL_OUT_MONO
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
                .build()
        }
    }

    // ---------------------------------------------------------------------------------------------
    // 5 - MediaCodec을 통해 음원 디코딩을 처리하고 AudioTrack을 통해 음원을 재생한다.
    private fun decodeAndPlay() {
        // AudioTrack, MediaExtractor, MediaCodec 객체가 null이면 실행을 멈춘다.
        if (audioTrack == null || extractor == null || codec == null) {
            return
        }

        // MediaCodec에서 Input/Output buffer를 꺼내올 때 timeout의 기준이 되는 마이크로초.
        val timeOutUs = 10000L
        // MediaCodec의 버퍼에 대한 메타데이터.
        val bufferInfo = MediaCodec.BufferInfo()
        // End-Of-Stream에 도달했는지?
        var isEOS = false
        // Output이 나오지 않은 횟수.
        var noOutputCount = 0
        // noOutputCount가 50을 넘기면 더 이상 디코딩 및 재생 작업을 수행하지 않는다.
        val noOutputCountLimit = 50

        // 미디어 파일을 디코딩해 재생한다.
        // 미디어 파일의 EOS에 도달하지 않았고, Output이 잘 나오고 있으며 스레드가 인터럽트되지 않은 경우 수행
        while (!isEOS && noOutputCount < noOutputCountLimit && !isInterrupted) {
            noOutputCount++

            // 특정 위치 탐색을 원하는가?
            if (isSeek) {
                // MediaExtractor를 특정 위치(playbackPosition)으로 이동시킨다.
                extractor!!.seekTo(playbackPosition, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                isSeek = false
            }

            // MediaCodec에서 입력 데이터를 쓸 버퍼의 인덱스를 가져온다.
            val inputBufferIndex = codec!!.dequeueInputBuffer(timeOutUs)
            if (inputBufferIndex >= 0) {
                // MediaCodec에서 입력 데이터를 쓸 버퍼를 가져온다.
                val destinationBuffer = codec!!.getInputBuffer(inputBufferIndex) ?: break
                // MediaExtractor를 사용해 버퍼에 압축된 미디어 데이터 샘플을 저장하고, 크기를 sampleSize에 기록한다.
                var sampleSize = extractor!!.readSampleData(destinationBuffer, 0)
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
                    presentationTimeUs = extractor!!.sampleTime
                }

                // MediaCodec에 입력 데이터를 쓴 버퍼를 넣는다.
                codec!!.queueInputBuffer(inputBufferIndex, 0, sampleSize, presentationTimeUs, flag)

                // EOS에 도달하지 않았다면 MediaExtractor가 미디어 데이터를 읽는 위치를 다음 샘플로 이동시킨다.
                if (!isEOS) {
                    extractor!!.advance()
                }
            }

            // MediaCodec에서 출력 데이터를 담은 버퍼의 인덱스를 가져온다.
            when (val outputBufferIndex = codec!!.dequeueOutputBuffer(bufferInfo, timeOutUs)) {
                MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    // The output format has changed, update any renderer
                    // configuration here if necessary
                }
                MediaCodec.INFO_TRY_AGAIN_LATER -> {
                    // No output available yet, wait for it
                }
                else -> {
                    if (bufferInfo.size > 0) {
                        noOutputCount = 0
                    }

                    // MediaCodec에서 출력 데이터를 담은 버퍼를 가져온다.
                    val buffer = codec!!.getOutputBuffer(outputBufferIndex)
                    // MediaCodec의 버퍼 크기만큼의 ByteArray를 만들고, 버퍼의 내용을 ByteArray로 옮긴다.
                    val chunk = ByteArray(bufferInfo.size)
                    buffer?.get(chunk)
                    buffer?.clear()

                    // ByteArray가 비어있지 않다면, AudioTrack을 사용해 디코딩된 출력 데이터를 재생한다.
                    if (chunk.isNotEmpty()) {
                        audioTrack!!.write(chunk, 0, chunk.size)
                    }

                    // MediaCodec으로 출력 데이터를 담은 버퍼를 반환한다.
                    codec!!.releaseOutputBuffer(outputBufferIndex, false)
                }
            }

            // playbackPosition 갱신
            playbackPosition = extractor!!.sampleTime
        }
    }


    companion object {
        private const val TAG = "AudioThread"
        private const val FILE_NAME = "music.mp3"
    }
}