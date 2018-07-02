/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.lifecycle

import com.tinder.scarlet.Lifecycle
import io.reactivex.Flowable
import io.reactivex.processors.ReplayProcessor
import io.reactivex.schedulers.TestScheduler
import org.junit.Test
import java.util.concurrent.TimeUnit

internal class LifecycleRegistryTest {
    private val testScheduler = TestScheduler()

    @Test
    fun observeState_shouldEmitInitialLifecycleState() {
        // Given
        val lifecycleRegistry = LifecycleRegistry(scheduler = testScheduler)

        // When
        val testSubscriber = Flowable.fromPublisher(lifecycleRegistry).test()
        testScheduler.triggerActions()

        // Then
        testSubscriber.assertNoValues()
    }

    @Test
    fun observeState_shouldEmitLastLifecycleState() {
        // Given
        val lifecycleRegistry = LifecycleRegistry(scheduler = testScheduler)
        lifecycleRegistry.onNext(Lifecycle.State.Started)

        // When
        val testSubscriber = Flowable.fromPublisher(lifecycleRegistry).test()
        testScheduler.triggerActions()

        // Then
        testSubscriber.assertValues(Lifecycle.State.Started)
    }

    @Test
    fun onNext_shouldEmitLifecycleState() {
        // Given
        val lifecycleRegistry = LifecycleRegistry(scheduler = testScheduler)
        val testSubscriber = Flowable.fromPublisher(lifecycleRegistry).test()

        // When
        lifecycleRegistry.onNext(Lifecycle.State.Started)
        testScheduler.triggerActions()

        // Then
        testSubscriber.assertValues(Lifecycle.State.Started)
    }

    @Test
    fun onNext_shouldDedupeLifecycleState() {
        // Given
        val lifecycleRegistry = LifecycleRegistry(scheduler = testScheduler)
        val testSubscriber = Flowable.fromPublisher(lifecycleRegistry).test()

        // When
        lifecycleRegistry.onNext(Lifecycle.State.Started)
        lifecycleRegistry.onNext(Lifecycle.State.Started)
        testScheduler.triggerActions()

        // Then
        testSubscriber.assertValues(Lifecycle.State.Started)

        // When
        lifecycleRegistry.onNext(Lifecycle.State.Stopped.WithReason())
        lifecycleRegistry.onNext(Lifecycle.State.Stopped.AndAborted)
        testScheduler.triggerActions()

        // Then
        testSubscriber.assertValues(Lifecycle.State.Started, Lifecycle.State.Stopped.WithReason())
    }

    @Test
    fun onNext_shouldThrottleLifecycleState() {
        // Given
        val throttleDurationMillis = 100L
        val lifecycleRegistry = LifecycleRegistry(throttleDurationMillis, testScheduler)
        val lifecycle1Delay = 0L
        val lifecycle2Delay = 50L
        val lifecycle3Delay = 60L
        val lifecycle4Delay = 260L
        ReplayProcessor.create<Lifecycle.State>()
            .apply { onNext(Lifecycle.State.Started) }
            .delay(lifecycle1Delay, TimeUnit.MILLISECONDS, testScheduler)
            .subscribe(lifecycleRegistry)
        ReplayProcessor.create<Lifecycle.State>()
            .apply { onNext(Lifecycle.State.Stopped.AndAborted) }
            .delay(lifecycle2Delay, TimeUnit.MILLISECONDS, testScheduler)
            .subscribe(lifecycleRegistry)
        ReplayProcessor.create<Lifecycle.State>()
            .apply { onNext(Lifecycle.State.Started) }
            .delay(lifecycle3Delay, TimeUnit.MILLISECONDS, testScheduler)
            .subscribe(lifecycleRegistry)
        ReplayProcessor.create<Lifecycle.State>()
            .apply { onNext(Lifecycle.State.Stopped.AndAborted) }
            .delay(lifecycle4Delay, TimeUnit.MILLISECONDS, testScheduler)
            .subscribe(lifecycleRegistry)

        // When
        val testSubscriber = Flowable.fromPublisher(lifecycleRegistry).test()
        testScheduler.advanceTimeTo(lifecycle1Delay, TimeUnit.MILLISECONDS)
        testScheduler.triggerActions()

        // Then
        testSubscriber.assertNoValues()

        // When
        testScheduler.advanceTimeTo(lifecycle2Delay, TimeUnit.MILLISECONDS)
        testScheduler.triggerActions()

        // Then
        testSubscriber.assertNoValues()

        // When
        testScheduler.advanceTimeTo(lifecycle3Delay, TimeUnit.MILLISECONDS)
        testScheduler.triggerActions()

        // Then
        testSubscriber.assertNoValues()

        // When
        testScheduler.advanceTimeTo(lifecycle3Delay + throttleDurationMillis, TimeUnit.MILLISECONDS)
        testScheduler.triggerActions()

        // Then
        testSubscriber.assertValues(Lifecycle.State.Started)

        // When
        testScheduler.advanceTimeTo(lifecycle4Delay, TimeUnit.MILLISECONDS)
        testScheduler.triggerActions()

        // Then
        testSubscriber.assertValues(Lifecycle.State.Started)

        // When
        testScheduler.advanceTimeTo(lifecycle4Delay + throttleDurationMillis, TimeUnit.MILLISECONDS)
        testScheduler.triggerActions()

        // Then
        testSubscriber.assertValues(Lifecycle.State.Started, Lifecycle.State.Stopped.AndAborted)
    }

    @Test
    fun onComplete_shouldTerminateLifecycleStateStream() {
        // Given
        val lifecycleRegistry = LifecycleRegistry(scheduler = testScheduler)
        val testSubscriber = Flowable.fromPublisher(lifecycleRegistry).test()

        // When
        lifecycleRegistry.onComplete()
        testScheduler.triggerActions()

        // Then
        testSubscriber.assertValues(Lifecycle.State.Destroyed)
        testSubscriber.assertComplete()
    }

    @Test
    fun onComplete_shouldThrottle() {
        // Given
        val throttleDurationMillis = 100L
        val lifecycleRegistry = LifecycleRegistry(throttleDurationMillis, testScheduler)
        val lifecycle1Delay = 0L
        val lifecycle2Delay = 50L
        val lifecycle3Delay = 60L
        val lifecycle4Delay = 260L
        Flowable.empty<Lifecycle.State>()
            .delay(lifecycle1Delay, TimeUnit.MILLISECONDS, testScheduler)
            .subscribe(lifecycleRegistry)
        Flowable.empty<Lifecycle.State>()
            .delay(lifecycle2Delay, TimeUnit.MILLISECONDS, testScheduler)
            .subscribe(lifecycleRegistry)
        ReplayProcessor.create<Lifecycle.State>()
            .apply { onNext(Lifecycle.State.Started) }
            .delay(lifecycle3Delay, TimeUnit.MILLISECONDS, testScheduler)
            .subscribe(lifecycleRegistry)
        Flowable.empty<Lifecycle.State>()
            .delay(lifecycle4Delay, TimeUnit.MILLISECONDS, testScheduler)
            .subscribe(lifecycleRegistry)

        // When
        val testSubscriber = Flowable.fromPublisher(lifecycleRegistry).test()
        testScheduler.advanceTimeTo(lifecycle1Delay, TimeUnit.MILLISECONDS)
        testScheduler.triggerActions()

        // Then
        testSubscriber.assertNoValues()
        testSubscriber.assertNotComplete()

        // When
        testScheduler.advanceTimeTo(lifecycle2Delay, TimeUnit.MILLISECONDS)
        testScheduler.triggerActions()

        // Then
        testSubscriber.assertNoValues()
        testSubscriber.assertNotComplete()

        // When
        testScheduler.advanceTimeTo(lifecycle3Delay, TimeUnit.MILLISECONDS)
        testScheduler.triggerActions()

        // Then
        testSubscriber.assertNoValues()
        testSubscriber.assertNotComplete()

        // When
        testScheduler.advanceTimeTo(lifecycle3Delay + throttleDurationMillis, TimeUnit.MILLISECONDS)
        testScheduler.triggerActions()

        // Then
        testSubscriber.assertValues(Lifecycle.State.Started)
        testSubscriber.assertNotComplete()

        // When
        testScheduler.advanceTimeTo(lifecycle4Delay, TimeUnit.MILLISECONDS)
        testScheduler.triggerActions()

        // Then
        testSubscriber.assertValues(Lifecycle.State.Started)
        testSubscriber.assertNotComplete()

        // When
        testScheduler.advanceTimeTo(lifecycle4Delay + throttleDurationMillis, TimeUnit.MILLISECONDS)
        testScheduler.triggerActions()

        // Then
        testSubscriber.assertValues(Lifecycle.State.Started, Lifecycle.State.Destroyed)
        testSubscriber.assertComplete()
    }
}
