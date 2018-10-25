/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.v2

import org.reactivestreams.Publisher

/**
 * Used to control when to start and stop WebSocket connections.
 */
interface Lifecycle : Publisher<LifecycleState> {

    /**
     * Returns a lifecycle that starts only when all source Lifecycles are start.
     */
    fun combineWith(vararg others: Lifecycle): Lifecycle {
        return combineWith(others.toList())
    }

    /**
     * Returns a lifecycle that starts only when all source Lifecycles are start.
     */
    fun combineWith(others: List<Lifecycle>): Lifecycle
}
