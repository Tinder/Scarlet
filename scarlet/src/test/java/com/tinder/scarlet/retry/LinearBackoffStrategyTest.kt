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
internal class LinearBackoffStrategyTest {

    @RunWith(Parameterized::class)
    class WithIllegalArguments(
        private val duration: Long
    ) {

        @Test
        fun shouldThrowIllegalArgumentException() {
            assertThatExceptionOfType(IllegalArgumentException::class.java)
                .isThrownBy {
                    LinearBackoffStrategy(duration)
                }
        }

        companion object {
            @Parameterized.Parameters(name = "{index}: LinearBackoffRetryStrategy(durationMillis = {0}) should throw exception")
            @JvmStatic
            fun data() = listOf(
                param(duration = -1),
                param(duration = -2),
                param(duration = 0)
            )

            private fun param(duration: Long) = arrayOf(duration)
        }
    }

    @RunWith(Parameterized::class)
    class WithLegalArguments(
        duration: Long,
        private val retryCount: Int,
        private val expectedWaitDuration: Long
    ) {
        private val retryStrategy = LinearBackoffStrategy(duration)

        @Test
        fun waitDurationMillis() {
            // When
            val waitDuration = retryStrategy.backoffDurationMillisAt(retryCount)

            // Then
            assertThat(waitDuration).isEqualTo(expectedWaitDuration)
        }

        companion object {
            @Parameterized.Parameters(name = "{index}: durationMillis = {0}, backoffDurationMillisAt(retryCount = {1}) = {2}")
            @JvmStatic
            fun data() = listOf(
                param(duration = 1, retryCount = 0, expectedWaitDuration = 1),
                param(duration = 1, retryCount = 1, expectedWaitDuration = 1),
                param(duration = 1, retryCount = 2, expectedWaitDuration = 1),
                param(duration = 2, retryCount = 0, expectedWaitDuration = 2),
                param(duration = 2, retryCount = 1, expectedWaitDuration = 2),
                param(duration = 2, retryCount = 2, expectedWaitDuration = 2),
                param(duration = 2, retryCount = 3, expectedWaitDuration = 2),
                param(duration = 2, retryCount = 4, expectedWaitDuration = 2),
                param(duration = 2, retryCount = 5, expectedWaitDuration = 2),
                param(duration = 2, retryCount = 10, expectedWaitDuration = 2),
                param(duration = 2, retryCount = 50, expectedWaitDuration = 2),
                param(duration = 2, retryCount = 100, expectedWaitDuration = 2),
                param(duration = 2, retryCount = 500, expectedWaitDuration = 2),
                param(duration = 2, retryCount = 1000, expectedWaitDuration = 2),
                param(duration = 2, retryCount = 10000, expectedWaitDuration = 2),
                param(duration = 2, retryCount = 100000, expectedWaitDuration = 2),
                param(duration = 2, retryCount = 1000000, expectedWaitDuration = 2),
                param(duration = 2, retryCount = 10000000, expectedWaitDuration = 2)
            )

            private fun param(duration: Long, retryCount: Int, expectedWaitDuration: Long) =
                arrayOf(duration, retryCount, expectedWaitDuration)
        }
    }
}
