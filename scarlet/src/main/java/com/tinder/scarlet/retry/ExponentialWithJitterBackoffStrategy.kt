/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.retry

import java.util.Random

class ExponentialWithJitterBackoffStrategy(
    val baseDurationMillis: Long,
    val maxDurationMillis: Long,
    private val random: Random = Random()
) : BackoffStrategy {

    private val exponentialBackoffRetryStrategy = ExponentialBackoffStrategy(baseDurationMillis, maxDurationMillis)

    override fun backoffDurationMillisAt(retryCount: Int): Long {
        val duration = exponentialBackoffRetryStrategy.backoffDurationMillisAt(retryCount)
        if (duration == maxDurationMillis) {
            return duration
        }
        return duration.withJitter()
    }

    private fun Long.withJitter(): Long = (0..this).random()

    private fun ClosedRange<Long>.random() = random.nextInt((endInclusive - start).toInt()) + start
}
