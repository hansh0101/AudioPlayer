package com.vimosoft.audioplayer.model

import android.media.MediaFormat
import java.nio.ByteBuffer

abstract class AudioOutputUnit {
    abstract fun configure(mediaFormat: MediaFormat)
    abstract fun release()
    abstract fun outputAudio(outputBuffer: ByteBuffer, size: Int)
}