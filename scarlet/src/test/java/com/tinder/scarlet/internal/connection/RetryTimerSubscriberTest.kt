/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.internal.connection

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.then
import com.tinder.scarlet.Event
import com.tinder.scarlet.internal.connection.subscriber.RetryTimerSubscriber
import io.reactivex.Flowable
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.Test

internal class RetryTimerSubscriberTest {
    private val connectionStateManager = mock<Connection.StateManager>()
    private val retryTimerSubscriber = RetryTimerSubscriber(connectionStateManager)

    @Test
    fun onNext_shouldEmitOnRetryEvent() {
        // Given
        val flowable = Flowable.just(1L)

        // When
        flowable.subscribe(retryTimerSubscriber)

        // Then
        then(connectionStateManager).should().handleEvent(Event.OnRetry)
    }

    @Test
    fun onError_shouldThrowException() {
        // Given
        val exception = RuntimeException("")
        val flowable = Flowable.error<Long>(exception)

        // Then
        assertThatExceptionOfType(RuntimeException::class.java)
            .isThrownBy { flowable.subscribe(retryTimerSubscriber) }
    }
}
