package com.vimosoft.audioplayer

import android.media.*
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.vimosoft.audioplayer.databinding.ActivityMainBinding
import timber.log.Timber

class MainActivity : AppCompatActivity() {
    // ---------------------------------------------------------------------------------------------
    // 음악 재생을 위한 변수
    private val streamType = AudioManager.STREAM_MUSIC
    private val sampleRateInHz = 88200
    private val channelConfig = AudioFormat.CHANNEL_OUT_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSizeInBytes =
        AudioTrack.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat)

//    MediaPlayer 객체
//    private val mediaPlayer = MediaPlayer().apply {
//        setOnCompletionListener { reset() }
//    }

    // ---------------------------------------------------------------------------------------------
    // UI 바인딩 객체 변수
    private lateinit var binding: ActivityMainBinding

    // ---------------------------------------------------------------------------------------------
    // 생명주기 콜백 메서드
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initEventListener()
    }

    override fun onResume() {
        super.onResume()
        playMusicByAudioTrack()
    }

    // 이벤트 리스너 등록.
    private fun initEventListener() {
        binding.run {
            buttonPlay.setOnClickListener { playMusic() }
            buttonStop.setOnClickListener { stopMusic() }
        }
    }

    private fun playMusic() {
//        MediaPlayer를 사용한 음악 재생
//        if (mediaPlayer.isPlaying) return
//
//        runCatching {
//            val assetFd = assets.openFd(FILE_NAME)
//            mediaPlayer.setDataSource(assetFd.fileDescriptor, assetFd.startOffset, assetFd.length)
//            assetFd.close()
//            mediaPlayer.run {
//                prepare()
//                start()
//            }
//        }.onFailure { Timber.e(it) }
    }

    private fun stopMusic() {
//        MediaPlayer를 사용한 음악 재생 중지
//        if (!mediaPlayer.isPlaying) return
//
//        mediaPlayer.run {
//            stop()
//            reset()
//        }
    }

    private fun playMusicByAudioTrack() {
        Thread {
            Timber.tag("playMusicByAudioTrack()").i("called")

            // 1 - AudioTrack 인스턴스 생성
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            val audioFormat = AudioFormat.Builder()
                .setChannelMask(channelConfig)
                .setEncoding(audioFormat)
                .setSampleRate(sampleRateInHz)
                .build()

            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(audioAttributes)
                .setAudioFormat(audioFormat)
                .setBufferSizeInBytes(bufferSizeInBytes)
                .build()

            // 1 - Logging
            Timber.tag("playMusicByAudioTrack()").i("AudioTrack 인스턴스 생성")

            // 2 - 오디오 재생 시작
            audioTrack.play()

            // 2 - Logging
            Timber.tag("playMusicByAudioTrack()").i("오디오 재생 시작")

            // 3 - AudioTrack에 오디오 데이터 쓰기
            // 3 - 여기 부분을 다시 작성해야 함
            // AudioTrack에 바로 넣어서 재생할 수 있는 파일은 압축되지 않은 파일만 가능함
            // 따라서 MediaCodec을 활용해 압축 해제하는 과정 필요

            val extractor = MediaExtractor().apply {
                setDataSource(assets.openFd(FILE_NAME))
            }

            val format = extractor.getTrackFormat(0)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: error("MIME null")
            val duration = format.getLong(MediaFormat.KEY_DURATION)
            val totalSec = (duration / 1000 / 1000).toInt()
            val min = totalSec / 60
            val sec = totalSec % 60

            Timber.tag("Time = ").i("${min}:${sec}")
            Timber.tag("Duration = ").i("$duration")

            val mediaCodec = MediaCodec.createDecoderByType(mime).apply {
                configure(format, null, null, 0)
                start()
            }

            val codecInputBuffers = mediaCodec.inputBuffers
            val codecOutputBuffers = mediaCodec.outputBuffers

            val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)

            Timber.tag("mime = ").i(mime)
            Timber.tag("sampleRate = ").i(sampleRate.toString())

            extractor.selectTrack(0)

            val timeOutUs = 10000L
            val info = MediaCodec.BufferInfo()
            var sawInputEOS = false
            var noOutputCounter = 0
            val noOutputCounterLimit = 50

            while (!sawInputEOS && noOutputCounter < noOutputCounterLimit) {
                noOutputCounter++

//                    if (isSeek) {
//                        extractor.seekTo(seekTime * 1000 * 1000, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
//                        isSeek = false
//                    }

                val inputBufIndex = mediaCodec.dequeueInputBuffer(timeOutUs)
                if (inputBufIndex >= 0) {
                    val dstBuf = codecInputBuffers[inputBufIndex]
                    var sampleSize = extractor.readSampleData(dstBuf, 0)
                    var presentationTimeUs = 0L

                    if (sampleSize < 0) {
                        Timber.i("saw input EOS.")
                        sawInputEOS = true
                        sampleSize = 0
                    } else {
                        presentationTimeUs = extractor.sampleTime
                        Timber.tag("presentationTime = ")
                            .i((presentationTimeUs / 1000 / 1000).toString())
                    }

                    mediaCodec.queueInputBuffer(
                        inputBufIndex,
                        0,
                        sampleSize,
                        presentationTimeUs,
                        if (sawInputEOS) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0
                    )

                    if (!sawInputEOS) {
                        extractor.advance()
                    }
                } else {
                    Timber.tag("inputBufIndex = ").e(inputBufIndex.toString())
                }

                val res = mediaCodec.dequeueOutputBuffer(info, timeOutUs)
                if (res >= 0) {
                    if (info.size > 0) {
                        noOutputCounter = 0
                    }

                    val outputBufIndex = res
                    val buf = codecOutputBuffers[outputBufIndex]
                    val chunk = ByteArray(info.size)
                    buf.get(chunk)
                    buf.clear()

                    if (chunk.isNotEmpty()) {
                        audioTrack.write(chunk, 0, chunk.size)
                    }

                    mediaCodec.releaseOutputBuffer(outputBufIndex, false)
                }
            }

            // 3 - Logging
            Timber.tag("playMusicByAudioTrack()").i("AudioTrack에 오디오 데이터 쓰기")

            // 4 - 오디오 재생 중지
            audioTrack.stop()

            // 4 - Logging
            Timber.tag("playMusicByAudioTrack()").i("오디오 재생 중지")

            // 5 - AudioTrack에서 사용하는 리소스 해제
            audioTrack.release()

            // 5 - Logging
            Timber.tag("playMusicByAudioTrack()").i("AudioTrack에서 사용하는 리소스 해제")
        }.start()
    }

    companion object {
        const val FILE_NAME = "music.mp3"
    }
}