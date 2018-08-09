/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.internal.connection

import com.nhaarman.mockito_kotlin.argThat
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.then
import com.nhaarman.mockito_kotlin.times
import com.tinder.scarlet.Event
import com.tinder.scarlet.Lifecycle
import com.tinder.scarlet.internal.connection.subscriber.LifecycleStateSubscriber
import io.reactivex.Flowable
import io.reactivex.processors.ReplayProcessor
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.Test

internal class LifecycleStateSubscriberTest {
    private val connectionStateManager = mock<Connection.StateManager>()
    private val lifecycleStateSubscriber = LifecycleStateSubscriber(connectionStateManager)

    @Test
    fun onNext_shouldHonorBackpressure() {
        // Given
        val lifecycleState = mock<Lifecycle.State>()
        val flowable = ReplayProcessor.create<Lifecycle.State>()
            .apply { onNext(lifecycleState) }
            .apply { onNext(lifecycleState) }

        // When
        flowable.subscribe(lifecycleStateSubscriber)

        // Then
        then(connectionStateManager).should().handleEvent(
            argThat<Event.OnLifecycle.StateChange<*>> { state == lifecycleState })

        // When
        lifecycleStateSubscriber.requestNext()

        // Then
        then(connectionStateManager).should(times(2)).handleEvent(
            argThat<Event.OnLifecycle.StateChange<*>> { state == lifecycleState })
    }

    @Test
    fun onNext_shouldEmitOnLifecycleStateChange() {
        // Given
        val lifecycleState = mock<Lifecycle.State>()
        val flowable = ReplayProcessor.create<Lifecycle.State>()
            .apply { onNext(lifecycleState) }

        // When
        flowable.subscribe(lifecycleStateSubscriber)

        // Then
        then(connectionStateManager).should().handleEvent(
            argThat<Event.OnLifecycle.StateChange<*>> { state == lifecycleState })
    }

    @Test
    fun onComplete_shouldEmitOnLifecycleTerminate() {
        // Given
        val flowable = Flowable.empty<Lifecycle.State>()

        // When
        flowable.subscribe(lifecycleStateSubscriber)

        // Then
        then(connectionStateManager).should().handleEvent(Event.OnLifecycle.Terminate)
    }

    @Test
    fun onError_shouldThrowException() {
        // Given
        val exception = RuntimeException("")
        val flowable = Flowable.error<Lifecycle.State>(exception)

        // Then
        assertThatExceptionOfType(RuntimeException::class.java)
            .isThrownBy { flowable.subscribe(lifecycleStateSubscriber) }
    }
}
