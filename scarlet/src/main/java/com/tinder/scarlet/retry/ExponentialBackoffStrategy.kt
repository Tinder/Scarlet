/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.retry

class ExponentialBackoffStrategy(
    val initialDurationMillis: Long,
    val maxDurationMillis: Long
) : BackoffStrategy {

    init {
        require(initialDurationMillis > 0) { "initialDurationMillis, $initialDurationMillis, must be positive" }
        require(maxDurationMillis > 0) { "maxDurationMillis, $maxDurationMillis, must be positive" }
    }

    override fun backoffDurationMillisAt(retryCount: Int): Long =
            // This will not overflow because double "flushes to infinity"
        Math.min(
            maxDurationMillis.toDouble(),
            initialDurationMillis.toDouble() * Math.pow(2.0, retryCount.toDouble())
        )
            .toLong()
}
