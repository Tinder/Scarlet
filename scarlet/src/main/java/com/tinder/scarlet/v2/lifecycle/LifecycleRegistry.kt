/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.v2.lifecycle

import com.tinder.scarlet.v2.Lifecycle
import com.tinder.scarlet.v2.LifecycleState
import io.reactivex.Scheduler
import io.reactivex.processors.BehaviorProcessor
import io.reactivex.processors.FlowableProcessor
import io.reactivex.processors.PublishProcessor
import io.reactivex.schedulers.Schedulers
import io.reactivex.subscribers.DisposableSubscriber
import org.reactivestreams.Subscriber
import java.util.concurrent.TimeUnit

/**
 * Used to trigger the start and stop of a WebSocket connection.
 */
class LifecycleRegistry internal constructor(
    private val upstreamProcessor: FlowableProcessor<LifecycleState>,
    private val downstreamProcessor: FlowableProcessor<LifecycleState>,
    throttleDurationMillis: Long,
    throttleScheduler: Scheduler
) : Lifecycle by FlowableLifecycle(downstreamProcessor.onBackpressureLatest(), throttleScheduler),
    Subscriber<LifecycleState> by upstreamProcessor {

    internal constructor(throttleTimeoutMillis: Long = 0, scheduler: Scheduler) : this(
        PublishProcessor.create(),
        BehaviorProcessor.create(),
        throttleTimeoutMillis,
        scheduler
    )

    constructor(throttleDurationMillis: Long = 0) : this(
        throttleDurationMillis,
        Schedulers.computation()
    )

    init {
        upstreamProcessor
            .onBackpressureLatest()
            .distinctUntilChanged(LifecycleState::isEquivalentTo)
            .compose {
                if (throttleDurationMillis != 0L) {
                    it.throttleWithTimeout(throttleDurationMillis, TimeUnit.MILLISECONDS, throttleScheduler)
                } else {
                    it
                }
            }
            .distinctUntilChanged(LifecycleState::isEquivalentTo)
            .subscribe(LifecycleStateSubscriber())
    }

    override fun onComplete() {
        upstreamProcessor.onNext(LifecycleState.Completed)
    }

    override fun onError(t: Throwable?) {
        upstreamProcessor.onNext(LifecycleState.Completed)
    }

    private inner class LifecycleStateSubscriber : DisposableSubscriber<LifecycleState>() {
        override fun onNext(state: LifecycleState) {
            downstreamProcessor.onNext(state)
            if (state == LifecycleState.Completed) {
                downstreamProcessor.onComplete()
                dispose()
            }
        }

        override fun onError(throwable: Throwable) {
            throw IllegalStateException("Stream is terminated", throwable)
        }

        override fun onComplete() {
            throw IllegalStateException("Stream is terminated")
        }
    }
}
