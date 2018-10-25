/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.v2

/**
 * Used to trigger the start and stop of WebSocket connections.
 */
sealed class LifecycleState {
    /**
     * Start and maintain a WebSocket connection.
     */
    object Started : LifecycleState()

    /**
     * Stop the web socket connection.
     */
    object Stopped : LifecycleState()

    object Completed : LifecycleState()
}
