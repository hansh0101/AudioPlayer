package com.vimosoft.audioplayer.controller

import android.os.Bundle
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.appcompat.app.AppCompatActivity
import com.vimosoft.audioplayer.R
import com.vimosoft.audioplayer.databinding.ActivityAudioPlayerBinding
import com.vimosoft.audioplayer.model.AudioPlayer

class AudioPlayerActivity : AppCompatActivity() {
    // ---------------------------------------------------------------------------------------------
    // 음악 재생을 위한 AudioPlayer 객체
    private var audioPlayer: AudioPlayer? = null

    // ---------------------------------------------------------------------------------------------
    // AudioPlayer Callback
    private val onConfigure: (String, String, String, Int) -> Unit =
        { inputUnit, decodeUnit, outputUnit, duration ->
            runOnUiThread {
                binding.textInputUnit.text = getString(R.string.label_input_unit, inputUnit)
                binding.textDecodeUnit.text = getString(R.string.label_decode_unit, decodeUnit)
                binding.textOutputUnit.text = getString(R.string.label_output_unit, outputUnit)
                binding.textDuration.text = resources.getString(R.string.label_duration, duration)
            }
        }
    private val onInputFileReady: (Int) -> Unit = { duration ->
        runOnUiThread { binding.seekBar.max = duration }
    }
    private val onPlay: (Int) -> Unit = { playbackPosition ->
        runOnUiThread {
            binding.seekBar.progress = playbackPosition
            binding.textPlayerState.text = getString(R.string.state_play)
        }
    }
    private val onPause: () -> Unit = {
        runOnUiThread {
            binding.textPlayerState.text = getString(R.string.state_pause)
        }
    }

    // ---------------------------------------------------------------------------------------------
    // UI 바인딩 객체 변수
    private lateinit var binding: ActivityAudioPlayerBinding

    // ---------------------------------------------------------------------------------------------
    // 생명주기 콜백 메서드
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAudioPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configureAudioPlayer()
        initEventListener()
    }

    override fun onDestroy() {
        super.onDestroy()
        audioPlayer?.release()
        audioPlayer = null
    }

    // ---------------------------------------------------------------------------------------------
    // AudioPlayer 객체 구성
    private fun configureAudioPlayer(
        inputType: Int = AudioPlayer.MEDIA_EXTRACTOR,
        decodeType: Int = AudioPlayer.MEDIA_CODEC,
        outputType: Int = AudioPlayer.AUDIO_TRACK
    ) {
        audioPlayer = AudioPlayer(
            applicationContext,
            inputType,
            decodeType,
            outputType,
            onConfigure,
            onInputFileReady,
            onPlay,
            onPause
        ).apply {
            prepare(FILE_NAME)
        }
    }

    // 이벤트 리스너 등록.
    private fun initEventListener() {
        binding.run {
            buttonPlay.setOnClickListener {
                playMusic()
            }
            buttonPause.setOnClickListener {
                pauseMusic()
            }
            buttonAudioTrack.setOnClickListener {
                val isPlayed = audioPlayer?.isPlaying ?: false
                pauseMusic()
                configureAudioPlayer(outputType = AudioPlayer.AUDIO_TRACK)
                if (isPlayed) {
                    playMusic()
                }
            }
            buttonOboe.setOnClickListener {
                val isPlayed = audioPlayer?.isPlaying ?: false
                pauseMusic()
                configureAudioPlayer(outputType = AudioPlayer.OBOE)
                if (isPlayed) {
                    playMusic()
                }
            }
            seekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                var desiredPosition: Long = 0L
                var isPlayed: Boolean = false

                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        desiredPosition = (progress * 1000 * 1000).toLong()
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {
                    isPlayed = audioPlayer?.isPlaying ?: false
                    pauseMusic()
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    audioPlayer?.seek(desiredPosition)
                    if (isPlayed) {
                        playMusic()
                    }
                }
            })
        }
    }

    // ---------------------------------------------------------------------------------------------
    // AudioPlayer를 사용해 오디오 파일을 재생하는 메서드
    private fun playMusic() {
        audioPlayer?.play()
    }

    // ---------------------------------------------------------------------------------------------
    // AudioPlayer를 사용해 오디오 파일 재생을 일시정지하는 메서드
    private fun pauseMusic() {
        audioPlayer?.pause()
    }

    companion object {
        const val FILE_NAME = "oasis.mp3"
    }
}