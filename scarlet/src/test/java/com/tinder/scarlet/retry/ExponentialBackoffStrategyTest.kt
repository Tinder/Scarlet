/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.retry

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Enclosed::class)
internal class ExponentialBackoffStrategyTest {

    @RunWith(Parameterized::class)
    class WithIllegalArguments(
        private val baseDuration: Long,
        private val maxDuration: Long
    ) {

        @Test
        fun shouldThrowIllegalArgumentException() {
            assertThatExceptionOfType(IllegalArgumentException::class.java)
                .isThrownBy {
                    ExponentialBackoffStrategy(baseDuration, maxDuration)
                }
        }

        companion object {
            @Parameterized.Parameters(
                name = "{index}: ExponentialBackoffRetryStrategy(baseDurationMillis = {0}, maxDurationMillis = {1}) " +
                        "should throw exception"
            )
            @JvmStatic
            fun data() = listOf(
                param(baseDuration = 0, maxDuration = 0),
                param(baseDuration = 0, maxDuration = 1),
                param(baseDuration = 0, maxDuration = 2),
                param(baseDuration = 1, maxDuration = 0),
                param(baseDuration = 2, maxDuration = 0)
            )

            private fun param(baseDuration: Long, maxDuration: Long) = arrayOf(baseDuration, maxDuration)
        }
    }

    @RunWith(Parameterized::class)
    class WithLegalArguments(
        baseDuration: Long,
        maxDuration: Long,
        private val retryCount: Int,
        private val expectedWaitDuration: Long
    ) {
        private val retryStrategy = ExponentialBackoffStrategy(baseDuration, maxDuration)

        @Test
        fun backoffDurationMillisAt() {
            // When
            val waitDuration = retryStrategy.backoffDurationMillisAt(retryCount)

            // Then
            assertThat(waitDuration).isEqualTo(expectedWaitDuration)
        }

        companion object {
            @Parameterized.Parameters(
                name = "{index}: baseDurationMillis = {0}, maxDurationMillis = {1}, " +
                        "backoffDurationMillisAt(retryCount = {2}) = {3}"
            )
            @JvmStatic
            fun data() = listOf(
                param(baseDuration = 1, maxDuration = 1, retryCount = 0, expectedWaitDuration = 1),
                param(baseDuration = 1, maxDuration = 1, retryCount = 1, expectedWaitDuration = 1),
                param(baseDuration = 2, maxDuration = 1, retryCount = 1, expectedWaitDuration = 1),
                param(baseDuration = 2, maxDuration = 10, retryCount = 0, expectedWaitDuration = 2),
                param(baseDuration = 2, maxDuration = 10, retryCount = 1, expectedWaitDuration = 4),
                param(baseDuration = 2, maxDuration = 10, retryCount = 2, expectedWaitDuration = 8),
                param(baseDuration = 2, maxDuration = 10, retryCount = 3, expectedWaitDuration = 10),
                param(baseDuration = 2, maxDuration = 10, retryCount = 4, expectedWaitDuration = 10),
                param(baseDuration = 2, maxDuration = 10, retryCount = 5, expectedWaitDuration = 10),
                param(baseDuration = 2, maxDuration = 10, retryCount = 10, expectedWaitDuration = 10),
                param(baseDuration = 2, maxDuration = 10, retryCount = 50, expectedWaitDuration = 10),
                param(baseDuration = 2, maxDuration = 10, retryCount = 100, expectedWaitDuration = 10),
                param(baseDuration = 2, maxDuration = 10, retryCount = 500, expectedWaitDuration = 10),
                param(baseDuration = 2, maxDuration = 10, retryCount = 1000, expectedWaitDuration = 10),
                param(baseDuration = 2, maxDuration = 10, retryCount = 10000, expectedWaitDuration = 10),
                param(baseDuration = 2, maxDuration = 10, retryCount = 100000, expectedWaitDuration = 10),
                param(baseDuration = 2, maxDuration = 10, retryCount = 1000000, expectedWaitDuration = 10),
                param(baseDuration = 2, maxDuration = 10, retryCount = 10000000, expectedWaitDuration = 10)
            )

            private fun param(baseDuration: Long, maxDuration: Long, retryCount: Int, expectedWaitDuration: Long) =
                arrayOf(baseDuration, maxDuration, retryCount, expectedWaitDuration)
        }
    }
}
