/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.v2.lifecycle

import com.tinder.scarlet.v2.LifecycleState
import io.reactivex.schedulers.Timed

internal fun List<Timed<LifecycleState>>.combine(): LifecycleState {
    val shouldStop = any { it.value().isStopped() }
    if (shouldStop) {
        return LifecycleState.Stopped
    }
    return LifecycleState.Started
}

internal fun LifecycleState.isEquivalentTo(other: LifecycleState): Boolean =
    this == other || isStopped() && other.isStopped()

private fun LifecycleState.isStopped(): Boolean = this is LifecycleState.Stopped
