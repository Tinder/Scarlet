/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.internal.coordinator

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.then
import io.reactivex.schedulers.TestScheduler
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class CoordinatorTest {

    private val stateMachineFactory = StateMachineFactory()
    private val session = mock<Session>()
    private val lifecycleEventSource = mock<LifecycleEventSource>()
    private val timerEventSource = mock<TimerEventSource>()
    private val scheduler = TestScheduler()

    private val coordinator =
        Coordinator(stateMachineFactory, session, lifecycleEventSource, timerEventSource, scheduler)

    @Test
    fun start_shouldStartEventSource() {
        // When
        coordinator.start()
        scheduler.triggerActions()

        // Then
        then(session).should().start(coordinator)
        then(lifecycleEventSource).should().start(coordinator)
    }
}