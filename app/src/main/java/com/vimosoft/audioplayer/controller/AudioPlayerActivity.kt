package com.vimosoft.audioplayer.controller

import android.os.Bundle
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.appcompat.app.AppCompatActivity
import com.vimosoft.audioplayer.R
import com.vimosoft.audioplayer.databinding.ActivityAudioPlayerBinding
import com.vimosoft.audioplayer.model.AudioPlayer

/**
 * AudioPlayer를 통해 오디오 파일을 재생하는 Activity 객체.
 */
class AudioPlayerActivity : AppCompatActivity() {
    // ---------------------------------------------------------------------------------------------
    // 오디오 파일 재생을 담당하는 AudioPlayer 관련 클래스 변수들.
    /**
     * 오디오 파일 재생을 담당하는 AudioPlayer 객체.
     */
    private var audioPlayer: AudioPlayer? = null

    /**
     * AudioPlayer 객체 구성 이후 AudioPlayer 관련 정보를 UI에 표시하는 콜백.
     */
    private val onConfigure: (String, String, String, Int) -> Unit =
        { inputUnit, decodeUnit, outputUnit, duration ->
            runOnUiThread {
                binding.textInputUnit.text = getString(R.string.label_input_unit, inputUnit)
                binding.textDecodeUnit.text = getString(R.string.label_decode_unit, decodeUnit)
                binding.textOutputUnit.text = getString(R.string.label_output_unit, outputUnit)
                binding.textDuration.text = resources.getString(R.string.label_duration, duration)
            }
        }

    /**
     * AudioPlayer 객체로 재생할 오디오 파일이 준비되면 UI의 SeekBar max를 설정하는 콜백.
     */
    private val onInputFileReady: (Int) -> Unit = { duration ->
        runOnUiThread { binding.seekBar.max = duration }
    }

    /**
     * AudioPlayer 재생 시 UI의 SeekBar와 TextView를 갱신하는 콜백.
     */
    private val onPlay: (Int) -> Unit = { playbackPosition ->
        runOnUiThread {
            binding.seekBar.progress = playbackPosition
            binding.textPlayerState.text = getString(R.string.state_play)
        }
    }

    /**
     * AudioPlayer 일시정지 시 UI의 TextView를 갱신하는 콜백.
     */
    private val onPause: () -> Unit = {
        runOnUiThread {
            binding.textPlayerState.text = getString(R.string.state_pause)
        }
    }

    // ---------------------------------------------------------------------------------------------
    // UI 관련 클래스 변수.
    /**
     * UI 바인딩 객체.
     */
    private lateinit var binding: ActivityAudioPlayerBinding

    // ---------------------------------------------------------------------------------------------
    // Lifecycle callback methods.
    /**
     * UI를 만들고, AudioPlayer 객체를 구성하고, 이벤트 리스너를 등록한다.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAudioPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configureAudioPlayer()
        initEventListener()
    }

    /**
     * AudioPlayer 객체에 대한 리소스를 해제한다.
     */
    override fun onDestroy() {
        super.onDestroy()
        audioPlayer?.release()
        audioPlayer = null
    }

    // ---------------------------------------------------------------------------------------------
    // AudioPlayer 관련 private methods.
    /**
     * AudioPlayer 객체를 구성한다.
     */
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

    /**
     * AudioPlayer 객체의 재생을 지시한다.
     */
    private fun playMusic() {
        audioPlayer?.play()
    }

    /**
     * AudioPlayer 객체의 일시정지를 지시한다.
     */
    private fun pauseMusic() {
        audioPlayer?.pause()
    }

    // ---------------------------------------------------------------------------------------------
    // 이벤트 리스너 관련 private methods.
    /**
     * 이벤트 리스너를 등록한다.
     */
    private fun initEventListener() {
        binding.run {
            buttonPlay.setOnClickListener {
                playMusic()
            }
            buttonPause.setOnClickListener {
                pauseMusic()
            }
            buttonAudioTrack.setOnClickListener {
                audioPlayer?.changeOutputUnit(AudioPlayer.AUDIO_TRACK)
            }
            buttonOboe.setOnClickListener {
                audioPlayer?.changeOutputUnit(AudioPlayer.OBOE)
            }
            seekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                // 원하는 재생 위치를 나타내는 Long 변수.
                var desiredPosition: Long = 0L
                // SeekBar 탐색 시작 시 AudioPlayer가 재생 중이었는지를 나타내는 Boolean 변수.
                var isPlayed: Boolean = false

                // 사용자에 의해 SeekBar progress가 변화할 경우 desiredPosition을 갱신한다.
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        desiredPosition = (progress * 1000 * 1000).toLong()
                    }
                }

                // SeekBar 터치를 시작하면, isPlayed 값을 임시로 저장하고 AudioPlayer 재생을 일시정지한다.
                override fun onStartTrackingTouch(seekBar: SeekBar) {
                    isPlayed = audioPlayer?.isPlaying ?: false
                    pauseMusic()
                }

                // SeekBar 터치가 끝나면, 재생 위치를 조정하고 AudioPlayer가 재생 중이었다면 이어서 재생되게 한다.
                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    audioPlayer?.seek(desiredPosition)
                    if (isPlayed) {
                        playMusic()
                    }
                }
            })
        }
    }

    companion object {
        const val FILE_NAME = "oasis.mp3"
    }
}