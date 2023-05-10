package com.vimosoft.audioplayer

import android.media.*
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.appcompat.app.AppCompatActivity
import com.vimosoft.audioplayer.databinding.ActivityMainBinding
import timber.log.Timber

class MainActivity : AppCompatActivity() {
    // ---------------------------------------------------------------------------------------------
    // 음악 재생을 위한 변수들
    private var audioThread: Thread? = null
    private var isSeek = false

    // ---------------------------------------------------------------------------------------------
    // UI 조작을 위한 변수들
    @Volatile
    private var playbackPosition = 0L
    private val handler = Handler(Looper.getMainLooper())
    private val updateSeekBarRunnable = object : Runnable {
        override fun run() {
            Timber.tag("playbackPosition").i(playbackPosition.toString())
            if (audioThread?.isAlive == true) {
                binding.seekBar.progress = (playbackPosition / 1000 / 1000).toInt()
            }
            handler.postDelayed(this, 1000)
        }
    }

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

    // 이벤트 리스너 등록.
    private fun initEventListener() {
        binding.run {
            buttonPlay.setOnClickListener { playMusic() }
            buttonStop.setOnClickListener { stopMusic() }
            seekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        isSeek = true
                        playbackPosition = (progress * 1000 * 1000).toLong()
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {
                    stopMusic()
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {}
            })
        }
    }

    private fun playMusic() {
        if (audioThread?.isAlive != true) {
            audioThread = configureThread()
            audioThread?.start()
            handler.post(updateSeekBarRunnable)
        }
    }

    private fun stopMusic() {
        if (audioThread?.isAlive == true) {
            audioThread?.interrupt()
            audioThread = null
        }
    }

    private fun configureThread(): Thread {
        return Thread {
            // 1 - AudioTrack 인스턴스 생성
            val audioTrack: AudioTrack = createAudioTrack()
            Timber.tag("playMusicByAudioTrack()").i("AudioTrack 인스턴스 생성")

            // 2 - 오디오 재생 시작
            audioTrack.play()
            Timber.tag("playMusicByAudioTrack()").i("오디오 재생 시작")

            // 3 - AudioTrack에 오디오 데이터 쓰기
            writeDataIntoAudioTrack(audioTrack)
            Timber.tag("playMusicByAudioTrack()").i("AudioTrack에 오디오 데이터 쓰기")

            // 4 - 오디오 재생 중지
            audioTrack.stop()
            Timber.tag("playMusicByAudioTrack()").i("오디오 재생 중지")

            // 5 - AudioTrack에서 사용하는 리소스 해제
            audioTrack.release()
            Timber.tag("playMusicByAudioTrack()").i("AudioTrack에서 사용하는 리소스 해제")
        }
    }

    private fun createAudioTrack(): AudioTrack {
        val sampleRateInHz = 88200
        val channelConfig = AudioFormat.CHANNEL_OUT_MONO
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

        return AudioTrack.Builder()
            .setAudioAttributes(audioAttributes)
            .setAudioFormat(audioFormat)
            .setBufferSizeInBytes(bufferSizeInBytes)
            .build()
    }

    private fun writeDataIntoAudioTrack(audioTrack: AudioTrack) {
        // Extractor 초기화 및 format, mime, duration 정보 액세스
        val extractor = MediaExtractor().apply {
            setDataSource(assets.openFd(FILE_NAME))
        }
        val format = extractor.getTrackFormat(0)
        val mime = format.getString(MediaFormat.KEY_MIME) ?: error("MIME null")
        val duration = format.getLong(MediaFormat.KEY_DURATION)
        val totalSec = (duration / 1000 / 1000).toInt()
        extractor.selectTrack(0)

        // Duration을 기반으로 SeekBar의 max 값 설정
        runOnUiThread { binding.seekBar.max = totalSec }

        // MediaCodec 객체 초기화 및 버퍼 설정
        val mediaCodec = MediaCodec.createDecoderByType(mime).apply {
            configure(format, null, null, 0)
            start()
        }

        val timeOutUs = 10000L
        val bufferInfo = MediaCodec.BufferInfo()
        var sawInputEOS = false
        var noOutputCounter = 0
        val noOutputCounterLimit = 50

        // MediaCodec을 통한 디코딩 및 음원 재생
        while (!sawInputEOS && noOutputCounter < noOutputCounterLimit && !Thread.currentThread().isInterrupted) {
            noOutputCounter++

            if (isSeek) {
                extractor.seekTo(playbackPosition, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                isSeek = false
            }

            // -------------------------------------------------------------------------------------
            // 이 과정에 대한 이해 필요
            val inputBufferIndex = mediaCodec.dequeueInputBuffer(timeOutUs)
            if (inputBufferIndex >= 0) {
                val buffer = mediaCodec.getInputBuffer(inputBufferIndex) ?: break
                var sampleSize = extractor.readSampleData(buffer, 0)
                var presentationTimeUs = 0L

                if (sampleSize < 0) {
                    sawInputEOS = true
                    sampleSize = 0
                } else {
                    presentationTimeUs = extractor.sampleTime
                }

                mediaCodec.queueInputBuffer(
                    inputBufferIndex,
                    0,
                    sampleSize,
                    presentationTimeUs,
                    if (sawInputEOS) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0
                )

                if (!sawInputEOS) {
                    extractor.advance()
                }
            } else {
                Timber.tag("inputBufIndex = ").e(inputBufferIndex.toString())
            }

            // -------------------------------------------------------------------------------------
            // 이 과정에 대한 이해 필요
            val outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, timeOutUs)
            if (outputBufferIndex >= 0) {
                if (bufferInfo.size > 0) {
                    noOutputCounter = 0
                }
                val buffer = mediaCodec.getOutputBuffer(outputBufferIndex)
                val chunk = ByteArray(bufferInfo.size)
                buffer?.get(chunk)
                buffer?.clear()

                if (chunk.isNotEmpty()) {
                    audioTrack.write(chunk, 0, chunk.size)
                }

                mediaCodec.releaseOutputBuffer(outputBufferIndex, false)
            }

            // playbackPosition 갱신
            playbackPosition = extractor.sampleTime
        }
    }

    companion object {
        const val FILE_NAME = "music.mp3"
    }
}