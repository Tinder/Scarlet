package com.carpa.simple_scarlet_websocket_kotlin.lifecycle

import android.content.res.Configuration
import com.tinder.scarlet.Lifecycle
import com.tinder.scarlet.lifecycle.LifecycleRegistry
import io.reactivex.Flowable
import io.reactivex.processors.BehaviorProcessor
import timber.log.Timber

/**
 * Mock-up lifecycle just to see how it should be created.
 * If we add .combineWith(CustomLifecycle...) in scarlet declaration (in MyWebSocketAPI) in lifecycle,
 * this will stop the WebSocket whenever we are not in portrait mode :)
 * NOT Implemented: if you want, try it (follow instruction below)
 * - Add .combineWith(CustomLifecycle...) in scarlet declaration (in MyWebSocketAPI) in lifecycle
 * - override onConfigurationChanged in MyApplication and from there call
 *      PortraitModeObserver.updateConfig(newConfig.orientation)
 * BEWARE: I am not sure if this is the best way to do it, it is just a silly example. :)
 * For more info check the loggedInLifecycle at:
 * https://github.com/Tinder/Scarlet/blob/master/demo/src/main/java/com/tinder/app/echo/domain/LoggedInLifecycle.kt
 */
class CustomLifecycle(portraitModeObserver: PortraitModeObserver,
    private val lifecycleRegistry: LifecycleRegistry = LifecycleRegistry()
) : Lifecycle by lifecycleRegistry {
    init{
        portraitModeObserver.observeMode().map{
            when(it){
                Configuration.ORIENTATION_PORTRAIT -> {
                    Lifecycle.State.Started
                }
                else -> {
                    Timber.d("Configuration changed: stopping connection")
                    Lifecycle.State.Stopped.WithReason()
                }
            }
        }.subscribe(lifecycleRegistry)
    }
}

object PortraitModeObserver{
    private val portraitModeBehavProc = BehaviorProcessor.createDefault(Configuration.ORIENTATION_PORTRAIT)

    fun observeMode() : Flowable<Int> = portraitModeBehavProc

    fun updateConfig(newConf: Int) {
        portraitModeBehavProc.onNext(newConf)
    }
}