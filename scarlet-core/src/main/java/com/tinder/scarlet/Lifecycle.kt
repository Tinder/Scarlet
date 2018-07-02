/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet

import org.reactivestreams.Publisher

/**
 * Used to control when to start and stop WebSocket connections.
 */
interface Lifecycle : Publisher<Lifecycle.State> {

    /**
     * Returns a lifecycle that starts only when all source Lifecycles are start.
     */
    fun combineWith(vararg others: Lifecycle): Lifecycle

    /**
     * Used to trigger the start and stop of WebSocket connections.
     */
    sealed class State {
        /**
         * Start and maintain a WebSocket connection.
         */
        object Started : State()

        /**
         * Stop the web socket connection.
         */
        sealed class Stopped : State() {
            /**
             * Stop after sending all pending messages.
             */
            data class WithReason(
                val shutdownReason: ShutdownReason = ShutdownReason.GRACEFUL
            ) : Stopped()

            /**
             * Stop and discard all pending messages.
             */
            object AndAborted : Stopped()
        }

        object Destroyed : State()
    }
}
