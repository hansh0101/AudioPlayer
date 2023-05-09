package com.vimosoft.audioplayer

import android.app.Application
import timber.log.Timber

class AudioPlayerApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
    }
}