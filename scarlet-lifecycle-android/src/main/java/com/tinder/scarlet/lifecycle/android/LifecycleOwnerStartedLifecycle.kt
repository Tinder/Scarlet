/*
 * © 2019 Match Group, LLC.
 */

package com.tinder.scarlet.lifecycle.android

import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import com.tinder.scarlet.Lifecycle
import com.tinder.scarlet.LifecycleState
import com.tinder.scarlet.lifecycle.LifecycleRegistry

internal class LifecycleOwnerStartedLifecycle(
        private val lifecycleOwner: LifecycleOwner,
        private val lifecycleRegistry: LifecycleRegistry
) : LifecycleObserver, Lifecycle by lifecycleRegistry {

    init {
        lifecycleOwner.lifecycle.addObserver(this)
    }

    @OnLifecycleEvent(androidx.lifecycle.Lifecycle.Event.ON_STOP)
    fun onPause() {
        lifecycleRegistry.onNext(LifecycleState.Stopped)
    }

    @OnLifecycleEvent(androidx.lifecycle.Lifecycle.Event.ON_START)
    fun onResume() {
        lifecycleRegistry.onNext(LifecycleState.Started)
    }

    @OnLifecycleEvent(androidx.lifecycle.Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        lifecycleRegistry.onComplete()
        lifecycleOwner.lifecycle.removeObserver(this)
    }
}