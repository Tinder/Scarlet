/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.lifecycle

import com.tinder.scarlet.Lifecycle
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
    private val upstreamProcessor: FlowableProcessor<Lifecycle.State>,
    private val downstreamProcessor: FlowableProcessor<Lifecycle.State>,
    throttleDurationMillis: Long,
    throttleScheduler: Scheduler
) : Lifecycle by FlowableLifecycle(downstreamProcessor.onBackpressureLatest(), throttleScheduler),
    Subscriber<Lifecycle.State> by upstreamProcessor {

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
            .distinctUntilChanged(Lifecycle.State::isEquivalentTo)
            .compose {
                if (throttleDurationMillis != 0L) {
                    it.throttleWithTimeout(throttleDurationMillis, TimeUnit.MILLISECONDS, throttleScheduler)
                } else {
                    it
                }
            }
            .distinctUntilChanged(Lifecycle.State::isEquivalentTo)
            .subscribe(LifecycleStateSubscriber())
    }

    override fun onComplete() {
        upstreamProcessor.onNext(Lifecycle.State.Destroyed)
    }

    override fun onError(t: Throwable?) {
        upstreamProcessor.onNext(Lifecycle.State.Destroyed)
    }

    private inner class LifecycleStateSubscriber : DisposableSubscriber<Lifecycle.State>() {
        override fun onNext(state: Lifecycle.State) {
            downstreamProcessor.onNext(state)
            if (state == Lifecycle.State.Destroyed) {
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
