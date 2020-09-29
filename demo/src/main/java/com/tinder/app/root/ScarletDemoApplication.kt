/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.app.root

import android.app.Application
import androidx.multidex.MultiDex
import com.facebook.stetho.Stetho
import com.tinder.app.root.koin.appModule
import com.tinder.app.socketio.chatroom.koin.chatRoomModule
import com.tinder.app.sse.stockprice.koin.sseModule
import com.tinder.app.websocket.echo.koin.echoModule
import com.tinder.app.websocket.gdax.koin.gdaxModule
import org.koin.android.ext.android.inject
import org.koin.android.ext.android.startKoin
import timber.log.Timber

class ScarletDemoApplication : Application() {

    val stethoInitializer: Stetho.Initializer by inject()

    override fun onCreate() {
        super.onCreate()
        startKoin(this, listOf(appModule, sseModule, gdaxModule, echoModule, chatRoomModule))

        MultiDex.install(this)
        Timber.plant(Timber.DebugTree())
        Stetho.initialize(stethoInitializer)
    }
}
