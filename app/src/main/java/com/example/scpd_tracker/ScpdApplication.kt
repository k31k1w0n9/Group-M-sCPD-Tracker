package com.example.scpd_tracker

import android.app.Application
import com.example.scpd_tracker.di.AppContainer

class ScpdApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
