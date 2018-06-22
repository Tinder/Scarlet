/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.retry

/**
 * Used to customize how often Scarlet retries WebSocket connections.
 */
interface BackoffStrategy {
    /**
     * Returns a duration in milliseconds.
     */
    fun backoffDurationMillisAt(retryCount: Int): Long
}
