package com.trailcurrent.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TrailCurrentApp : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
