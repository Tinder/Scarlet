/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.lifecycle

import com.tinder.scarlet.LifecycleState
import io.reactivex.Flowable
import io.reactivex.schedulers.TestScheduler
import org.junit.Test

class FlowableLifecycleTest {
    private val testScheduler = TestScheduler()

    @Test
    fun combineWith_shouldCombineStates() {
        // Given
        val lifecycleRegistry1 = LifecycleRegistry(throttleScheduler = testScheduler)
        val lifecycleRegistry2 = LifecycleRegistry(throttleScheduler = testScheduler)
        val combinedLifecycle = lifecycleRegistry1.combineWith(lifecycleRegistry2)
        val testSubscriber = Flowable.fromPublisher(combinedLifecycle).test()

        // When
        lifecycleRegistry1.onNext(LifecycleState.Stopped)
        lifecycleRegistry2.onNext(LifecycleState.Stopped)
        testScheduler.triggerActions()

        // Then
        testSubscriber.assertValues(LifecycleState.Stopped)

        // When
        lifecycleRegistry2.onNext(LifecycleState.Started)
        testScheduler.triggerActions()

        // Then
        testSubscriber.assertValues(
            LifecycleState.Stopped,
            LifecycleState.Stopped
        )

        // When
        lifecycleRegistry1.onNext(LifecycleState.Started)
        testScheduler.triggerActions()

        // Then
        testSubscriber.assertValues(
            LifecycleState.Stopped,
            LifecycleState.Stopped,
            LifecycleState.Started
        )
    }
}
