/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.lifecycle

import com.tinder.scarlet.LifecycleState

internal fun List<LifecycleState>.combine(): LifecycleState {
    val shouldStop = any { it == LifecycleState.Stopped }
    if (shouldStop) {
        return LifecycleState.Stopped
    }
    return LifecycleState.Started
}
