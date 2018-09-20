/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.v2.service

import com.tinder.scarlet.v2.Event
import com.tinder.scarlet.v2.Lifecycle
import io.reactivex.subscribers.DisposableSubscriber
import java.util.concurrent.atomic.AtomicInteger

internal class LifecycleEventSource(
    private val lifecycle: Lifecycle
) {

    private lateinit var eventCallback: EventCallback
    private lateinit var lifecycleStateSubscriber: LifecycleStateSubscriber

    fun start(eventCallback: EventCallback) {
        this.eventCallback = eventCallback
        lifecycleStateSubscriber = LifecycleStateSubscriber()
        lifecycle.subscribe(lifecycleStateSubscriber)
    }

    private inner class LifecycleStateSubscriber(
    ) : DisposableSubscriber<Lifecycle.State>() {
        private val pendingRequestCount = AtomicInteger()

        override fun onStart() = request(1)

        override fun onNext(lifecycleState: Lifecycle.State) {
            val value = pendingRequestCount.decrementAndGet()
            if (value < 0) {
                pendingRequestCount.set(0)
            }
            eventCallback.onEvent(Event.OnLifecycleStateChange(lifecycleState))
        }

        override fun onComplete() {
            eventCallback.onEvent(Event.OnLifecycleStateChange(Lifecycle.State.Completed))
        }

        override fun onError(throwable: Throwable) = throw throwable

        fun requestNext() {
            if (pendingRequestCount.get() == 0) {
                pendingRequestCount.incrementAndGet()
                request(1)
            }
        }
    }
}

