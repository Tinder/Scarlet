/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.lifecycle

import com.tinder.scarlet.Lifecycle
import io.reactivex.Flowable
import io.reactivex.schedulers.TestScheduler
import org.junit.Test

class FlowableLifecycleTest {
    private val testScheduler = TestScheduler()

    @Test
    fun combineWith_shouldCombineStates() {
        // Given
        val lifecycleRegistry1 = LifecycleRegistry(scheduler = testScheduler)
        val lifecycleRegistry2 = LifecycleRegistry(scheduler = testScheduler)
        val combinedLifecycle = lifecycleRegistry1.combineWith(lifecycleRegistry2)
        val testSubscriber = Flowable.fromPublisher(combinedLifecycle).test()

        // When
        lifecycleRegistry1.onNext(Lifecycle.State.Stopped.WithReason())
        lifecycleRegistry2.onNext(Lifecycle.State.Stopped.AndAborted)
        testScheduler.triggerActions()

        // Then
        testSubscriber.assertValues(Lifecycle.State.Stopped.AndAborted)

        // When
        lifecycleRegistry2.onNext(Lifecycle.State.Started)
        testScheduler.triggerActions()

        // Then
        testSubscriber.assertValues(Lifecycle.State.Stopped.AndAborted, Lifecycle.State.Stopped.WithReason())

        // When
        lifecycleRegistry1.onNext(Lifecycle.State.Started)
        testScheduler.triggerActions()

        // Then
        testSubscriber.assertValues(
            Lifecycle.State.Stopped.AndAborted,
            Lifecycle.State.Stopped.WithReason(),
            Lifecycle.State.Started
        )
    }
}
