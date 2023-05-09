package com.vimosoft.audioplayer

import android.media.MediaPlayer
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.vimosoft.audioplayer.databinding.ActivityMainBinding
import timber.log.Timber

class MainActivity : AppCompatActivity() {
    // ---------------------------------------------------------------------------------------------
    // 음악 재생을 위한 변수
    private val mediaPlayer = MediaPlayer()

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
        }
    }

    private fun playMusic() {
        if (mediaPlayer.isPlaying) return

        runCatching {
            val assetFd = assets.openFd(FILE_NAME)
            mediaPlayer.setDataSource(assetFd.fileDescriptor, assetFd.startOffset, assetFd.length)
            assetFd.close()
            mediaPlayer.run {
                prepare()
                start()
            }
        }.onFailure { Timber.e(it) }
    }

    private fun stopMusic() {
        if (!mediaPlayer.isPlaying) return

        mediaPlayer.run {
            stop()
            reset()
        }
    }

    companion object {
        const val FILE_NAME = "music.mp3"
    }
}