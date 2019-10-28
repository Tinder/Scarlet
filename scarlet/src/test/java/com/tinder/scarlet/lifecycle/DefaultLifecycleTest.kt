/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.lifecycle

import com.tinder.scarlet.LifecycleState
import io.reactivex.Flowable
import org.junit.Test

internal class DefaultLifecycleTest {

    private val defaultLifecycle = DefaultLifecycle()

    @Test
    fun observeState_shouldEmitStartedLifecycleState() {
        // When
        val testSubscriber = Flowable.fromPublisher(defaultLifecycle).test()

        // Then
        testSubscriber.assertValues(LifecycleState.Started)
    }

    @Test
    fun observeState_shouldNotTerminate() {
        // When
        val testSubscriber = Flowable.fromPublisher(defaultLifecycle).test()

        // Then
        testSubscriber.assertNotTerminated()
    }
}
