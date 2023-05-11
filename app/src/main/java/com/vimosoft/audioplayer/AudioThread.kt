package com.vimosoft.audioplayer

import android.content.Context
import android.content.res.AssetFileDescriptor
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
    private lateinit var assetFileDescriptor: AssetFileDescriptor
    var duration = 0L

    // ---------------------------------------------------------------------------------------------
    // Thread의 동작을 정의한다.
    override fun run() {
        // 1 - MediaExtractor 객체 생성
        assetFileDescriptor = context.assets.openFd(FILE_NAME)
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
        assetFileDescriptor.close()
        codec?.release()
        extractor?.release()
        audioTrack?.release()
        Timber.tag(TAG).i("7 - 리소스 해제")
    }

    // ---------------------------------------------------------------------------------------------
    // 1 - MediaExtractor 객체를 생성한다.
    private fun configureMediaExtractor() {
        extractor = MediaExtractor().apply {
            setDataSource(assetFileDescriptor)
        }
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
                .build()
        }
    }

    // ---------------------------------------------------------------------------------------------
    // 5 - MediaCodec을 통해 음원 디코딩을 처리하고 AudioTrack을 통해 음원을 재생한다.
    private fun decodeAndPlay() {
        if (audioTrack == null || extractor == null || codec == null) {
            return
        }

        val timeOutUs = 10000L
        val bufferInfo = MediaCodec.BufferInfo()
        var isEOS = false
        var noOutputCount = 0
        val noOutputCountLimit = 50

        // 반복문을 통해 inputBuffer를 읽고 outputBuffer를 통해 쓴다.
        while (!isEOS && noOutputCount < noOutputCountLimit && !isInterrupted) {
            noOutputCount++

            if (isSeek) {
                extractor!!.seekTo(playbackPosition, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                isSeek = false
            }

            // InputBuffer 처리
            val inputBufferIndex = codec!!.dequeueInputBuffer(timeOutUs)
            if (inputBufferIndex >= 0) {
                val destinationBuffer = codec!!.getInputBuffer(inputBufferIndex) ?: break
                var sampleSize = extractor!!.readSampleData(destinationBuffer, 0)
                var presentationTimeUs = 0L
                var flag = 0

                if (sampleSize < 0) {
                    isEOS = true
                    sampleSize = 0
                    flag = MediaCodec.BUFFER_FLAG_END_OF_STREAM
                } else {
                    presentationTimeUs = extractor!!.sampleTime
                }

                codec!!.queueInputBuffer(inputBufferIndex, 0, sampleSize, presentationTimeUs, flag)

                if (!isEOS) {
                    extractor!!.advance()
                }
            }

            // OutputBuffer 처리
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

                    val buffer = codec!!.getOutputBuffer(outputBufferIndex)
                    val chunk = ByteArray(bufferInfo.size)
                    buffer?.get(chunk)
                    buffer?.clear()

                    if (chunk.isNotEmpty()) {
                        audioTrack!!.write(chunk, 0, chunk.size)
                    }

                    codec!!.releaseOutputBuffer(outputBufferIndex, false)
                }
            }

            if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                isEOS = true
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