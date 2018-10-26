/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.lifecycle

import com.tinder.scarlet.LifecycleState
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
        val lifecycleRegistry = LifecycleRegistry(throttleScheduler = testScheduler)

        // When
        val testSubscriber = Flowable.fromPublisher(lifecycleRegistry).test()
        testScheduler.triggerActions()

        // Then
        testSubscriber.assertNoValues()
    }

    @Test
    fun observeState_shouldEmitLastLifecycleState() {
        // Given
        val lifecycleRegistry = LifecycleRegistry(throttleScheduler = testScheduler)
        lifecycleRegistry.onNext(LifecycleState.Started)

        // When
        val testSubscriber = Flowable.fromPublisher(lifecycleRegistry).test()
        testScheduler.triggerActions()

        // Then
        testSubscriber.assertValues(LifecycleState.Started)
    }

    @Test
    fun onNext_shouldEmitLifecycleState() {
        // Given
        val lifecycleRegistry = LifecycleRegistry(throttleScheduler = testScheduler)
        val testSubscriber = Flowable.fromPublisher(lifecycleRegistry).test()

        // When
        lifecycleRegistry.onNext(LifecycleState.Started)
        testScheduler.triggerActions()

        // Then
        testSubscriber.assertValues(LifecycleState.Started)
    }

    @Test
    fun onNext_shouldDedupeLifecycleState() {
        // Given
        val lifecycleRegistry = LifecycleRegistry(throttleScheduler = testScheduler)
        val testSubscriber = Flowable.fromPublisher(lifecycleRegistry).test()

        // When
        lifecycleRegistry.onNext(LifecycleState.Started)
        lifecycleRegistry.onNext(LifecycleState.Started)
        testScheduler.triggerActions()

        // Then
        testSubscriber.assertValues(LifecycleState.Started)

        // When
        lifecycleRegistry.onNext(LifecycleState.Stopped)
        lifecycleRegistry.onNext(LifecycleState.Stopped)
        testScheduler.triggerActions()

        // Then
        testSubscriber.assertValues(LifecycleState.Started, LifecycleState.Stopped)
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
        ReplayProcessor.create<LifecycleState>()
            .apply { onNext(LifecycleState.Started) }
            .delay(lifecycle1Delay, TimeUnit.MILLISECONDS, testScheduler)
            .subscribe(lifecycleRegistry)
        ReplayProcessor.create<LifecycleState>()
            .apply { onNext(LifecycleState.Stopped) }
            .delay(lifecycle2Delay, TimeUnit.MILLISECONDS, testScheduler)
            .subscribe(lifecycleRegistry)
        ReplayProcessor.create<LifecycleState>()
            .apply { onNext(LifecycleState.Started) }
            .delay(lifecycle3Delay, TimeUnit.MILLISECONDS, testScheduler)
            .subscribe(lifecycleRegistry)
        ReplayProcessor.create<LifecycleState>()
            .apply { onNext(LifecycleState.Stopped) }
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
        testSubscriber.assertValues(LifecycleState.Started)

        // When
        testScheduler.advanceTimeTo(lifecycle4Delay, TimeUnit.MILLISECONDS)
        testScheduler.triggerActions()

        // Then
        testSubscriber.assertValues(LifecycleState.Started)

        // When
        testScheduler.advanceTimeTo(lifecycle4Delay + throttleDurationMillis, TimeUnit.MILLISECONDS)
        testScheduler.triggerActions()

        // Then
        testSubscriber.assertValues(LifecycleState.Started, LifecycleState.Stopped)
    }

//    @Test
//    fun onComplete_shouldTerminateLifecycleStateStream() {
//        // Given
//        val lifecycleRegistry = LifecycleRegistry(throttleScheduler = testScheduler)
//        val testSubscriber = Flowable.fromPublisher(lifecycleRegistry).test()
//
//        // When
//        lifecycleRegistry.onComplete()
//        testScheduler.triggerActions()
//
//        // Then
//        testSubscriber.assertValues(LifecycleState.Completed)
//        testSubscriber.assertComplete()
//    }

    @Test
    fun onComplete_shouldThrottle() {
        // Given
        val throttleDurationMillis = 100L
        val lifecycleRegistry = LifecycleRegistry(throttleDurationMillis, testScheduler)
        val lifecycle1Delay = 0L
        val lifecycle2Delay = 50L
        val lifecycle3Delay = 60L
        val lifecycle4Delay = 260L
        Flowable.empty<LifecycleState>()
            .delay(lifecycle1Delay, TimeUnit.MILLISECONDS, testScheduler)
            .subscribe(lifecycleRegistry)
        Flowable.empty<LifecycleState>()
            .delay(lifecycle2Delay, TimeUnit.MILLISECONDS, testScheduler)
            .subscribe(lifecycleRegistry)
        ReplayProcessor.create<LifecycleState>()
            .apply { onNext(LifecycleState.Started) }
            .delay(lifecycle3Delay, TimeUnit.MILLISECONDS, testScheduler)
            .subscribe(lifecycleRegistry)
        Flowable.empty<LifecycleState>()
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
        testSubscriber.assertValues(LifecycleState.Started)
        testSubscriber.assertNotComplete()

        // When
        testScheduler.advanceTimeTo(lifecycle4Delay, TimeUnit.MILLISECONDS)
        testScheduler.triggerActions()

        // Then
        testSubscriber.assertValues(LifecycleState.Started)
        testSubscriber.assertNotComplete()

        // When
        testScheduler.advanceTimeTo(lifecycle4Delay + throttleDurationMillis, TimeUnit.MILLISECONDS)
        testScheduler.triggerActions()

        // Then
        testSubscriber.assertValues(LifecycleState.Started, LifecycleState.Completed)
//        testSubscriber.assertComplete()
    }
}
