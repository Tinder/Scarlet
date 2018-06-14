/*
 * © 2013 - 2018 Tinder, Inc., ALL RIGHTS RESERVED
 */

package com.tinder.scarlet.lifecycle.android

import android.app.Application
import android.arch.lifecycle.LifecycleOwner
import com.tinder.scarlet.Lifecycle
import com.tinder.scarlet.lifecycle.LifecycleRegistry

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
