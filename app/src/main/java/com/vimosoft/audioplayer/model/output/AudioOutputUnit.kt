package com.vimosoft.audioplayer.model.output

import android.media.MediaFormat
import java.nio.ByteBuffer

abstract class AudioOutputUnit() {
    abstract val name: String

    abstract fun configure(mediaFormat: MediaFormat)
    abstract fun release()
    abstract fun outputAudio(outputBuffer: ByteBuffer, size: Int)
}