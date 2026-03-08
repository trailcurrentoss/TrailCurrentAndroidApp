package com.trailcurrentoutbound.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TrailCurrentOutboundApp : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
