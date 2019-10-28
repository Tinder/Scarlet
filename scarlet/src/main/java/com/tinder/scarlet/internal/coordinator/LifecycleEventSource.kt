/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.internal.coordinator

import com.tinder.scarlet.Event
import com.tinder.scarlet.Lifecycle
import com.tinder.scarlet.LifecycleState
import io.reactivex.Flowable
import io.reactivex.Scheduler
import io.reactivex.subscribers.DisposableSubscriber
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

internal class LifecycleEventSource(
    private val scheduler: Scheduler,
    private val lifecycle: Lifecycle
) {

    private var lifecycleStateSubscriber: LifecycleStateSubscriber? = null
    private var eventCallback: EventCallback? = null

    fun start(eventCallback: EventCallback) {
        this.eventCallback = eventCallback
        lifecycleStateSubscriber = LifecycleStateSubscriber()
        Flowable.fromPublisher(lifecycle)
            .observeOn(scheduler)
            .subscribe(lifecycleStateSubscriber)
        resume()
    }

    fun resume() {
        lifecycleStateSubscriber?.resume()
    }

    fun pause() {
        lifecycleStateSubscriber?.pause()
    }

    fun stop() {
        eventCallback = null
        lifecycleStateSubscriber?.dispose()
        lifecycleStateSubscriber = null
    }

    private inner class LifecycleStateSubscriber : DisposableSubscriber<LifecycleState>() {

        private val lastUndeliveredLifecycleState = AtomicReference<LifecycleState>()
        private val isResumed = AtomicBoolean()

        override fun onNext(lifecycleState: LifecycleState) {
            lastUndeliveredLifecycleState.set(lifecycleState)
            flushIfNeeded()
        }

        override fun onComplete() {
            lastUndeliveredLifecycleState.set(LifecycleState.Completed)
            flushIfNeeded()
        }

        override fun onError(throwable: Throwable) = throw throwable

        fun resume() {
            if (isResumed.get()) {
                return
            }
            isResumed.set(true)
            flushIfNeeded()
        }

        fun pause() {
            isResumed.set(false)
        }

        private fun flushIfNeeded() {
            if (!isResumed.get()) {
                return
            }
            val lifecycleState = lastUndeliveredLifecycleState.getAndSet(null)
            if (lifecycleState != null) {
                eventCallback?.onEvent(Event.OnLifecycleStateChange(lifecycleState))
            }
        }
    }
}
