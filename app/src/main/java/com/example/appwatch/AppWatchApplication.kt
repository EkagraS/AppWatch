package com.example.appwatch

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AppWatchApplication : Application(){
    override fun onCreate() {
        super.onCreate()

    }

}