package com.vimosoft.audioplayer

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.appcompat.app.AppCompatActivity
import com.vimosoft.audioplayer.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    // ---------------------------------------------------------------------------------------------
    // 음악 재생을 위한 변수들
    private var audioThread: AudioThread? = null
    private var isSeek = false
    private var desiredPosition = 0L

    // ---------------------------------------------------------------------------------------------
    // UI를 갱신하기 위한 Handler와 Runnable
    private val uiUpdateHandler = Handler(Looper.getMainLooper())
    private val uiUpdateRunnable = object : Runnable {
        override fun run() {
            // AudioThread의 생사 여부에 따라 UI를 갱신한다.
            if (audioThread?.isAlive == true) {
                val duration = (audioThread!!.duration / 1000 / 1000).toInt()
                if (binding.seekBar.max != duration && duration != 0) {
                    binding.seekBar.max = duration
                }

                binding.seekBar.progress = (audioThread!!.playbackPosition / 1000 / 1000).toInt()
                uiUpdateHandler.postDelayed(this, 1000)
            } else {
                binding.textPlayerState.text = getString(R.string.state_stop)
                uiUpdateHandler.removeCallbacks(this)
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
                        desiredPosition = (progress * 1000 * 1000).toLong()
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {
                    stopMusic()
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    playMusic()
                }
            })
        }
    }

    // ---------------------------------------------------------------------------------------------
    // AudioThread를 사용해 음악을 재생하는 메서드
    private fun playMusic() {
        if (audioThread?.isAlive != true) {
            audioThread = when (isSeek) {
                true -> AudioThread(applicationContext, isSeek, desiredPosition)
                false -> AudioThread(applicationContext)
            }
            isSeek = false
            audioThread?.start()
            uiUpdateHandler.post(uiUpdateRunnable)

            binding.textPlayerState.text = getString(R.string.state_play)
        }
    }

    // ---------------------------------------------------------------------------------------------
    // AudioThread를 사용해 음악을 재생하던 것을 중지하는 메서드
    private fun stopMusic() {
        if (audioThread?.isAlive == true) {
            audioThread?.interrupt()
            audioThread = null
            uiUpdateHandler.removeCallbacks(uiUpdateRunnable)

            binding.seekBar.progress = 0
            binding.textPlayerState.text = getString(R.string.state_stop)
        }
    }
}