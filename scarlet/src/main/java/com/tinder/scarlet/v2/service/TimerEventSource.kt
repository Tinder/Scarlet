/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.v2.service

import com.tinder.scarlet.retry.BackoffStrategy
import io.reactivex.Flowable
import io.reactivex.Scheduler
import io.reactivex.subscribers.DisposableSubscriber
import java.util.concurrent.TimeUnit

internal class TimerEventSource(
    private val scheduler: Scheduler,
    private val backoffStrategy: BackoffStrategy
) {

    private var subscriber: RetryTimerSubscriber? = null
    private var eventSourceCallback: EventCallback? = null

    fun start(retryCount: Int, eventSourceCallback: EventCallback) {
        this.eventSourceCallback = eventSourceCallback
        val backoffDuration = backoffStrategy.backoffDurationMillisAt(retryCount)
        subscriber = RetryTimerSubscriber()
        Flowable.timer(backoffDuration, TimeUnit.MILLISECONDS, scheduler)
            .subscribe(subscriber)
    }

    fun stop() {
        eventSourceCallback = null
        subscriber?.dispose()
    }

    private inner class RetryTimerSubscriber(
    ) : DisposableSubscriber<Long>() {
        override fun onNext(t: Long) {
            eventSourceCallback?.onEvent(Event.OnShouldConnect)
        }

        override fun onComplete() {
        }

        override fun onError(throwable: Throwable) = throw throwable
    }
}


