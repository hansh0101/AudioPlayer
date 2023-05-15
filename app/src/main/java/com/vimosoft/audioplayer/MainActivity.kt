package com.vimosoft.audioplayer

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
    // 음악 재생을 위한 AudioPlayer 객체
    private var audioPlayer: AudioPlayer? = null

    // ---------------------------------------------------------------------------------------------
    // UI를 갱신하기 위한 Handler와 Runnable
    private val uiUpdateHandler = Handler(Looper.getMainLooper())
    private val uiUpdateRunnable = object : Runnable {
        override fun run() {
            Timber.tag("isPlaying?").i("${audioPlayer?.isPlaying}")

            // AudioThread의 생사 여부에 따라 UI를 갱신한다.
            if (audioPlayer?.isPlaying == true) {
                val duration = (audioPlayer!!.duration / 1000 / 1000).toInt()
                if (binding.seekBar.max != duration && duration != 0) {
                    binding.seekBar.max = duration
                }

                binding.seekBar.progress = (audioPlayer!!.playbackPosition / 1000 / 1000).toInt()
                uiUpdateHandler.postDelayed(this, 1000)
            } else {
                uiUpdateHandler.removeCallbacks(this)
                binding.textPlayerState.text = getString(R.string.state_pause)
            }
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

        configureAudioPlayer()
        initEventListener()
    }

    override fun onDestroy() {
        super.onDestroy()
        audioPlayer?.releaseResources()
        audioPlayer = null
    }

    private fun configureAudioPlayer() {
        audioPlayer = AudioPlayer(applicationContext).apply {
            prepare("music.mp3")
        }
    }

    // 이벤트 리스너 등록.
    private fun initEventListener() {
        binding.run {
            buttonPlay.setOnClickListener { playMusic() }
            buttonPause.setOnClickListener { pauseMusic() }
            seekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                var desiredPosition: Long = 0L
                val isPlayed: Boolean = audioPlayer?.isPlaying ?: false

                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        desiredPosition = (progress * 1000 * 1000).toLong()
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {
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
    // AudioThread를 사용해 음악을 재생하는 메서드
    private fun playMusic() {
        Timber.tag("playMusic()").i("called")

        audioPlayer?.play()
        uiUpdateHandler.post(uiUpdateRunnable)
        binding.textPlayerState.text = getString(R.string.state_play)
    }

    // ---------------------------------------------------------------------------------------------
    // AudioThread를 사용해 음악을 재생하던 것을 중지하는 메서드
    private fun pauseMusic() {
        audioPlayer?.pause()
        binding.textPlayerState.text = getString(R.string.state_pause)
    }
}