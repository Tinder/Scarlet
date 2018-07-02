/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.retry

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doAnswer
import com.nhaarman.mockito_kotlin.mock
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.Random

@RunWith(Enclosed::class)
internal class ExponentialBackoffWithJitterBackoffStrategyTest {

    @RunWith(Parameterized::class)
    class WithIllegalArguments(
        private val baseDuration: Long,
        private val maxDuration: Long
    ) {

        @Test
        fun shouldThrowIllegalArgumentException() {
            assertThatExceptionOfType(IllegalArgumentException::class.java)
                .isThrownBy {
                    ExponentialWithJitterBackoffStrategy(baseDuration, maxDuration)
                }
        }

        companion object {
            @Parameterized.Parameters(
                name = "{index}: ExponentialBackoffWithJitterRetryStrategy(baseDurationMillis = {0}, maxDurationMillis = {1})" +
                        " should throw exception"
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
        jitterPercent: Int,
        private val retryCount: Int,
        private val expectedWaitDuration: Long
    ) {
        private val random = mock<Random> {
            on { nextInt(any()) } doAnswer { it.getArgument<Int>(0) * jitterPercent / 100 }
        }
        private val retryStrategy = ExponentialWithJitterBackoffStrategy(baseDuration, maxDuration, random)

        @Test
        fun waitDurationMillis() {
            // When
            val waitDuration = retryStrategy.backoffDurationMillisAt(retryCount)

            // Then
            assertThat(waitDuration).isEqualTo(expectedWaitDuration)
        }

        companion object {
            @Parameterized.Parameters(
                name = "{index}: baseDurationMillis = {0}, maxDurationMillis = {1}, jitterPercentage = {2}, " +
                        "backoffDurationMillisAt(retryCount = {3}) = {4}"
            )
            @JvmStatic
            fun data() = listOf(
                param(
                    baseDuration = 10, maxDuration = 10, jitterPercentage = 10, retryCount = 0,
                    expectedWaitDuration = 10
                ),
                param(
                    baseDuration = 10, maxDuration = 10, jitterPercentage = 10, retryCount = 1,
                    expectedWaitDuration = 10
                ),
                param(
                    baseDuration = 20, maxDuration = 10, jitterPercentage = 10, retryCount = 1,
                    expectedWaitDuration = 10
                ),
                param(
                    baseDuration = 20, maxDuration = 100, jitterPercentage = 10, retryCount = 0,
                    expectedWaitDuration = 2
                ),
                param(
                    baseDuration = 20, maxDuration = 100, jitterPercentage = 10, retryCount = 1,
                    expectedWaitDuration = 4
                ),
                param(
                    baseDuration = 20, maxDuration = 100, jitterPercentage = 10, retryCount = 2,
                    expectedWaitDuration = 8
                ),
                param(
                    baseDuration = 20, maxDuration = 100, jitterPercentage = 10, retryCount = 3,
                    expectedWaitDuration = 100
                ),
                param(
                    baseDuration = 20, maxDuration = 100, jitterPercentage = 10, retryCount = 4,
                    expectedWaitDuration = 100
                ),
                param(
                    baseDuration = 20, maxDuration = 100, jitterPercentage = 10, retryCount = 5,
                    expectedWaitDuration = 100
                ),
                param(
                    baseDuration = 20, maxDuration = 100, jitterPercentage = 10, retryCount = 10,
                    expectedWaitDuration = 100
                ),
                param(
                    baseDuration = 20, maxDuration = 100, jitterPercentage = 10, retryCount = 50,
                    expectedWaitDuration = 100
                ),
                param(
                    baseDuration = 20, maxDuration = 100, jitterPercentage = 10, retryCount = 100,
                    expectedWaitDuration = 100
                ),
                param(
                    baseDuration = 20, maxDuration = 100, jitterPercentage = 10, retryCount = 500,
                    expectedWaitDuration = 100
                ),
                param(
                    baseDuration = 20, maxDuration = 100, jitterPercentage = 10, retryCount = 1000,
                    expectedWaitDuration = 100
                ),
                param(
                    baseDuration = 20, maxDuration = 100, jitterPercentage = 10, retryCount = 10000,
                    expectedWaitDuration = 100
                ),
                param(
                    baseDuration = 20, maxDuration = 100, jitterPercentage = 10, retryCount = 100000,
                    expectedWaitDuration = 100
                ),
                param(
                    baseDuration = 20, maxDuration = 100, jitterPercentage = 10, retryCount = 1000000,
                    expectedWaitDuration = 100
                ),
                param(
                    baseDuration = 20, maxDuration = 100, jitterPercentage = 10, retryCount = 10000000,
                    expectedWaitDuration = 100
                ),
                param(
                    baseDuration = 10, maxDuration = 10, jitterPercentage = 50, retryCount = 0,
                    expectedWaitDuration = 10
                ),
                param(
                    baseDuration = 10, maxDuration = 10, jitterPercentage = 50, retryCount = 1,
                    expectedWaitDuration = 10
                ),
                param(
                    baseDuration = 20, maxDuration = 10, jitterPercentage = 50, retryCount = 1,
                    expectedWaitDuration = 10
                ),
                param(
                    baseDuration = 20, maxDuration = 100, jitterPercentage = 50, retryCount = 0,
                    expectedWaitDuration = 10
                ),
                param(
                    baseDuration = 20, maxDuration = 100, jitterPercentage = 50, retryCount = 1,
                    expectedWaitDuration = 20
                ),
                param(
                    baseDuration = 20, maxDuration = 100, jitterPercentage = 50, retryCount = 2,
                    expectedWaitDuration = 40
                ),
                param(
                    baseDuration = 20, maxDuration = 100, jitterPercentage = 50, retryCount = 3,
                    expectedWaitDuration = 100
                ),
                param(
                    baseDuration = 20, maxDuration = 100, jitterPercentage = 50, retryCount = 4,
                    expectedWaitDuration = 100
                ),
                param(
                    baseDuration = 20, maxDuration = 100, jitterPercentage = 50, retryCount = 5,
                    expectedWaitDuration = 100
                ),
                param(
                    baseDuration = 20, maxDuration = 100, jitterPercentage = 50, retryCount = 10,
                    expectedWaitDuration = 100
                ),
                param(
                    baseDuration = 20, maxDuration = 100, jitterPercentage = 50, retryCount = 50,
                    expectedWaitDuration = 100
                ),
                param(
                    baseDuration = 20, maxDuration = 100, jitterPercentage = 50, retryCount = 100,
                    expectedWaitDuration = 100
                ),
                param(
                    baseDuration = 20, maxDuration = 100, jitterPercentage = 50, retryCount = 500,
                    expectedWaitDuration = 100
                ),
                param(
                    baseDuration = 20, maxDuration = 100, jitterPercentage = 50, retryCount = 1000,
                    expectedWaitDuration = 100
                ),
                param(
                    baseDuration = 20, maxDuration = 100, jitterPercentage = 50, retryCount = 10000,
                    expectedWaitDuration = 100
                ),
                param(
                    baseDuration = 20, maxDuration = 100, jitterPercentage = 50, retryCount = 100000,
                    expectedWaitDuration = 100
                ),
                param(
                    baseDuration = 20, maxDuration = 100, jitterPercentage = 50, retryCount = 1000000,
                    expectedWaitDuration = 100
                ),
                param(
                    baseDuration = 20, maxDuration = 100, jitterPercentage = 50, retryCount = 10000000,
                    expectedWaitDuration = 100
                )
            )

            private fun param(
                baseDuration: Long,
                maxDuration: Long,
                jitterPercentage: Int,
                retryCount: Int,
                expectedWaitDuration: Long
            ) =
                arrayOf(baseDuration, maxDuration, jitterPercentage, retryCount, expectedWaitDuration)
        }
    }
}
