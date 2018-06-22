/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.lifecycle

import com.tinder.scarlet.Lifecycle
import io.reactivex.schedulers.Timed

internal fun List<Timed<Lifecycle.State>>.combine(): Lifecycle.State {
    val shouldStopAndAbort = any { it.value().isStoppedAndAborted() }
    if (shouldStopAndAbort) {
        return Lifecycle.State.Stopped.AndAborted
    }

    val shouldStop = any { it.value().isStopped() }
    if (shouldStop) {
        return filter { it.value().isStopped() }
            .sortedBy { it.time() }
            .first()
            .value()
    }

    return Lifecycle.State.Started
}

internal fun Lifecycle.State.isEquivalentTo(other: Lifecycle.State): Boolean =
    this == other || isStopped() && other.isStopped()

private fun Lifecycle.State.isStopped(): Boolean = this is Lifecycle.State.Stopped

private fun Lifecycle.State.isStoppedAndAborted(): Boolean = this == Lifecycle.State.Stopped.AndAborted
