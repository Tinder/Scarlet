/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.lifecycle

import com.tinder.scarlet.LifecycleState
import io.reactivex.Flowable
import io.reactivex.Scheduler
import io.reactivex.schedulers.Schedulers
import org.junit.Test
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class LifecycleRegistryBackpressureTest {

    @Test
    fun givenBlockedSubscriber_shouldNotThrowException() {
        // Given
        val lifecycleRegistry = LifecycleRegistry()
        val blockedLifecycleFlowable = Flowable.fromPublisher(lifecycleRegistry)
            .observeOn(createBlockedScheduler())

        // When
        val testSubscriber = blockedLifecycleFlowable.test()
        generateLifecycleStates().subscribe(lifecycleRegistry)

        // Then
        testSubscriber.awaitDone(BLOCK_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
        testSubscriber.assertValueSequence(getExpectedLifecycleStates())
        testSubscriber.assertNoErrors()
    }

    private fun createBlockedScheduler(): Scheduler = Schedulers.from(createBlockedExecutor())

    private fun createBlockedExecutor(): Executor {
        val executorService = Executors.newSingleThreadExecutor()
        executorService.execute {
            try {
                Thread.sleep(BLOCK_TIMEOUT_MILLIS)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
        return executorService
    }

    private fun generateLifecycleStates(): Flowable<LifecycleState> = Flowable.range(0, 1000)
        .map {
            if (it % 2 == 0) {
                LifecycleState.Started
            } else {
                LifecycleState.Stopped
            }
        }

    private fun getExpectedLifecycleStates(): List<LifecycleState> {
        val ringBufferSize = Flowable.bufferSize().toLong()
        return generateLifecycleStates()
            .take(ringBufferSize)
            .concatWith(Flowable.just(LifecycleState.Completed))
            .blockingIterable().toList()
    }

    companion object {
        private const val BLOCK_TIMEOUT_MILLIS = 100L
    }
}
