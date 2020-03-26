/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.lifecycle.android

import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import com.tinder.scarlet.Lifecycle
import com.tinder.scarlet.lifecycle.LifecycleRegistry

internal class ServiceStartedLifecycle(
    private val lifecycleOwner: LifecycleOwner,
    private val lifecycleRegistry: LifecycleRegistry
) : Lifecycle by lifecycleRegistry {

    init {
        lifecycleOwner.lifecycle.addObserver(ALifecycleObserver())
    }

    private inner class ALifecycleObserver : LifecycleObserver {
        @OnLifecycleEvent(androidx.lifecycle.Lifecycle.Event.ON_START)
        fun onResume() = lifecycleRegistry.onNext(Lifecycle.State.Started)

        @OnLifecycleEvent(androidx.lifecycle.Lifecycle.Event.ON_STOP)
        fun onStop() = lifecycleRegistry.onNext(Lifecycle.State.Stopped.AndAborted)

        @OnLifecycleEvent(androidx.lifecycle.Lifecycle.Event.ON_DESTROY)
        fun onDestroy() {
            lifecycleRegistry.onComplete()
        }
    }
}