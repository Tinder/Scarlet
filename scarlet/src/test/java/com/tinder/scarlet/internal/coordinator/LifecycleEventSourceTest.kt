/*
 * © 2019 Match Group, LLC.
 */

package com.tinder.scarlet.internal.coordinator

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.only
import com.nhaarman.mockito_kotlin.then
import com.tinder.scarlet.Event
import com.tinder.scarlet.LifecycleState
import com.tinder.scarlet.lifecycle.LifecycleRegistry
import io.reactivex.schedulers.TestScheduler
import org.junit.Test

class LifecycleEventSourceTest {

    private val testScheduler = TestScheduler()
    private val lifecycleRegistry = LifecycleRegistry()
    private val lifecycleEventSource = LifecycleEventSource(testScheduler, lifecycleRegistry)

    private val eventCallback = mock<EventCallback>()

    @Test
    fun start_givenLifecycleState_shouldNotify() {
        // Given
        lifecycleRegistry.onNext(LifecycleState.Started)

        // When
        lifecycleEventSource.start(eventCallback)
        testScheduler.triggerActions()

        // Then
        then(eventCallback).should().onEvent(Event.OnLifecycleStateChange(LifecycleState.Started))
    }

    @Test
    fun start_givenNoLifecycleState_shouldNotNotify() {
        // When
        lifecycleEventSource.start(eventCallback)
        testScheduler.triggerActions()

        // Then
        then(eventCallback).shouldHaveZeroInteractions()
    }

    @Test
    fun stop_givenLifecycleState_shouldNotNotify() {
        // Given
        lifecycleRegistry.onNext(LifecycleState.Started)
        lifecycleEventSource.start(eventCallback)
        testScheduler.triggerActions()

        // When
        lifecycleEventSource.stop()
        lifecycleRegistry.onNext(LifecycleState.Stopped)
        testScheduler.triggerActions()

        // Then
        then(eventCallback).should(only())
            .onEvent(Event.OnLifecycleStateChange(LifecycleState.Started))
    }

    @Test
    fun pause_givenLifecycleState_shouldNotNotify() {
        // Given
        lifecycleRegistry.onNext(LifecycleState.Started)
        lifecycleEventSource.start(eventCallback)
        testScheduler.triggerActions()

        // When
        lifecycleEventSource.pause()
        lifecycleRegistry.onNext(LifecycleState.Stopped)
        testScheduler.triggerActions()

        // Then
        then(eventCallback).should(only())
            .onEvent(Event.OnLifecycleStateChange(LifecycleState.Started))
    }

    @Test
    fun resume_givenLifecycleState_shouldNotify() {
        // Given
        lifecycleRegistry.onNext(LifecycleState.Started)
        lifecycleEventSource.start(eventCallback)
        testScheduler.triggerActions()

        lifecycleEventSource.pause()
        lifecycleRegistry.onNext(LifecycleState.Stopped)

        // When
        lifecycleEventSource.resume()
        testScheduler.triggerActions()

        // Then
        then(eventCallback).should().onEvent(Event.OnLifecycleStateChange(LifecycleState.Started))
        then(eventCallback).should().onEvent(Event.OnLifecycleStateChange(LifecycleState.Stopped))
    }

}