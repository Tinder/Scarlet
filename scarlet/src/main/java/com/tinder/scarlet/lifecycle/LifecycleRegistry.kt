/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.lifecycle

import com.tinder.scarlet.Lifecycle
import com.tinder.scarlet.LifecycleState
import io.reactivex.Flowable
import io.reactivex.Scheduler
import io.reactivex.processors.BehaviorProcessor
import io.reactivex.processors.FlowableProcessor
import io.reactivex.schedulers.Schedulers
import org.reactivestreams.Subscriber
import java.util.concurrent.TimeUnit

/**
 * Used to trigger the start and stop of a WebSocket connection.
 */
class LifecycleRegistry private constructor(
    private val processor: FlowableProcessor<LifecycleState>,
    throttleDurationMillis: Long,
    throttleScheduler: Scheduler
) : Subscriber<LifecycleState> by processor,
    Lifecycle by FlowableLifecycle(
        processor.throttle(
            throttleDurationMillis,
            throttleScheduler
        )
    ) {

    internal constructor(throttleDurationMillis: Long = 0, throttleScheduler: Scheduler) : this(
        BehaviorProcessor.create<LifecycleState>(),
        throttleDurationMillis,
        throttleScheduler
    )

    constructor(throttleDurationMillis: Long = 0) : this(
        throttleDurationMillis,
        Schedulers.computation()
    )

    override fun onNext(state: LifecycleState) {
        processor.onNext(state)
    }

    override fun onComplete() {
        onNext(LifecycleState.Completed)
    }

    override fun onError(t: Throwable?) {
        onNext(LifecycleState.Completed)
    }

    private companion object {
        private fun Flowable<LifecycleState>.throttle(
            throttleDurationMillis: Long,
            throttleScheduler: Scheduler
        ): Flowable<LifecycleState> {
            return onBackpressureLatest()
                .distinctUntilChanged()
                .compose {
                    if (throttleDurationMillis > 0L) {
                        it.throttleWithTimeout(throttleDurationMillis, TimeUnit.MILLISECONDS, throttleScheduler)
                    } else {
                        it
                    }
                }
                .distinctUntilChanged()
        }
    }
}
