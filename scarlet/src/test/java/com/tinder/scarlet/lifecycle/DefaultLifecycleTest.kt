/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.lifecycle

import com.tinder.scarlet.Lifecycle
import io.reactivex.Flowable
import org.junit.Test

internal class DefaultLifecycleTest {

    private val defaultLifecycle = DefaultLifecycle()

    @Test
    fun observeState_shouldEmitStartedLifecycleState() {
        // When
        val testSubscriber = Flowable.fromPublisher(defaultLifecycle).test()

        // Then
        testSubscriber.assertValues(Lifecycle.State.Started)
    }
}
