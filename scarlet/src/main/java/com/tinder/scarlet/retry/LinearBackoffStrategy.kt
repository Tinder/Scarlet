/*
 * © 2013 - 2018 Tinder, Inc., ALL RIGHTS RESERVED
 */

package com.tinder.scarlet.retry

class LinearBackoffStrategy(
    val durationMillis: Long
) : BackoffStrategy {

    init {
        require(durationMillis > 0) { "durationMillis, $durationMillis, must be positive" }
    }

    override fun backoffDurationMillisAt(retryCount: Int): Long = durationMillis
}
