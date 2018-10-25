/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.lifecycle.android.v2

import android.app.Application
import androidx.lifecycle.LifecycleOwner
import com.tinder.scarlet.v2.Lifecycle
import com.tinder.scarlet.v2.lifecycle.LifecycleRegistry

object AndroidLifecycle {
    private const val APPLICATION_THROTTLE_TIMEOUT_MILLIS = 1000L
    private const val ACTIVITY_THROTTLE_TIMEOUT_MILLIS = 500L

    @JvmStatic
    @JvmOverloads
    fun ofApplicationForeground(
        application: Application,
        throttleTimeoutMillis: Long = APPLICATION_THROTTLE_TIMEOUT_MILLIS
    ): Lifecycle =
        ApplicationResumedLifecycle(application, LifecycleRegistry(throttleTimeoutMillis))
            .combineWith(ConnectivityOnLifecycle(application))

    @JvmStatic
    @JvmOverloads
    fun ofLifecycleOwnerForeground(
        application: Application,
        lifecycleOwner: LifecycleOwner,
        throttleTimeoutMillis: Long = ACTIVITY_THROTTLE_TIMEOUT_MILLIS
    ): Lifecycle =
        LifecycleOwnerResumedLifecycle(lifecycleOwner, LifecycleRegistry(throttleTimeoutMillis))
            .combineWith(ConnectivityOnLifecycle(application))
}
