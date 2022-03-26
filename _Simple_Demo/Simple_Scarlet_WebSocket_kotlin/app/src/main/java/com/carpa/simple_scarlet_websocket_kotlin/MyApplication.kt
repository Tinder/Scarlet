package com.carpa.simple_scarlet_websocket_kotlin

import android.app.Application
import timber.log.Timber

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        //useful for setting up Timber for logging information.
        //Remember to add the dependency in the manifest :)
        Timber.plant(Timber.DebugTree())
    }
}