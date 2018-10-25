/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.lifecycle.android.v2

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.tinder.scarlet.v2.Lifecycle
import com.tinder.scarlet.v2.LifecycleState
import com.tinder.scarlet.v2.lifecycle.LifecycleRegistry

internal class ApplicationResumedLifecycle(
    application: Application,
    private val lifecycleRegistry: LifecycleRegistry
) : Lifecycle by lifecycleRegistry {

    init {
        lifecycleRegistry.onNext(LifecycleState.Started)
        application.registerActivityLifecycleCallbacks(ActivityLifecycleCallbacks())
    }

    private inner class ActivityLifecycleCallbacks : Application.ActivityLifecycleCallbacks {
        override fun onActivityPaused(activity: Activity?) {
            lifecycleRegistry.onNext(LifecycleState.Stopped)
        }

        override fun onActivityResumed(activity: Activity?) {
            lifecycleRegistry.onNext(LifecycleState.Started)
        }

        override fun onActivityStarted(activity: Activity?) {}

        override fun onActivityDestroyed(activity: Activity?) {}

        override fun onActivitySaveInstanceState(activity: Activity?, outState: Bundle?) {}

        override fun onActivityStopped(activity: Activity?) {}

        override fun onActivityCreated(activity: Activity?, savedInstanceState: Bundle?) {}
    }
}
