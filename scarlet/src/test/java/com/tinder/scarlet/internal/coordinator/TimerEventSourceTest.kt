/*
 * © 2019 Match Group, LLC.
 */

package com.tinder.scarlet.internal.coordinator

import com.nhaarman.mockitokotlin2.given
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.then
import com.tinder.scarlet.Event
import com.tinder.scarlet.retry.BackoffStrategy
import io.reactivex.schedulers.TestScheduler
import org.junit.Test
import java.util.concurrent.TimeUnit

class TimerEventSourceTest {

    private val testScheduler = TestScheduler()
    private val backoffStrategy = mock<BackoffStrategy>()
    private val timerEventSource = TimerEventSource(testScheduler, backoffStrategy)

    private val eventCallback = mock<EventCallback>()

    @Test
    fun start_shouldNotify() {
        // When
        timerEventSource.start(0, eventCallback)
        testScheduler.triggerActions()

        // Then
        then(eventCallback).should().onEvent(Event.OnShouldConnect)
    }

    @Test
    fun start_givenRetryCount_shouldNotify() {
        // Given
        val retryCount = 1
        val backoffDurationMillis = 100L
        given(backoffStrategy.backoffDurationMillisAt(retryCount)).willReturn(backoffDurationMillis)

        // When
        timerEventSource.start(retryCount, eventCallback)
        testScheduler.triggerActions()

        // Then
        then(eventCallback).should(never()).onEvent(Event.OnShouldConnect)

        // When
        testScheduler.advanceTimeBy(backoffDurationMillis, TimeUnit.MILLISECONDS)
        testScheduler.triggerActions()

        // Then
        then(eventCallback).should().onEvent(Event.OnShouldConnect)
    }

    @Test
    fun start_givenNoLifecycleState_shouldNotNotify() {
        // When
        testScheduler.triggerActions()

        // Then
        then(eventCallback).shouldHaveZeroInteractions()
    }

    @Test
    fun stop_givenLifecycleState_shouldNotNotify() {
        // Given
        val retryCount = 1
        val backoffDurationMillis = 100L
        given(backoffStrategy.backoffDurationMillisAt(retryCount)).willReturn(backoffDurationMillis)
        timerEventSource.start(retryCount, eventCallback)
        testScheduler.advanceTimeBy(backoffDurationMillis / 2, TimeUnit.MILLISECONDS)
        testScheduler.triggerActions()

        // When
        timerEventSource.stop()
        testScheduler.advanceTimeBy(backoffDurationMillis, TimeUnit.MILLISECONDS)
        testScheduler.triggerActions()

        // Then
        then(eventCallback).should(never()).onEvent(Event.OnShouldConnect)
    }
}