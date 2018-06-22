/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.internal.connection.subscriber

import com.tinder.scarlet.Event
import com.tinder.scarlet.Lifecycle
import com.tinder.scarlet.internal.connection.Connection
import io.reactivex.subscribers.DisposableSubscriber
import java.util.concurrent.atomic.AtomicInteger

internal class LifecycleStateSubscriber(
    private val stateManager: Connection.StateManager
) : DisposableSubscriber<Lifecycle.State>() {
    private val pendingRequestCount = AtomicInteger()

    override fun onStart() = request(1)

    override fun onNext(lifecycleState: Lifecycle.State) {
        val value = pendingRequestCount.decrementAndGet()
        if (value < 0) {
            pendingRequestCount.set(0)
        }
        stateManager.handleEvent(Event.OnLifecycle.StateChange(lifecycleState))
    }

    override fun onComplete() = stateManager.handleEvent(Event.OnLifecycle.Terminate)

    override fun onError(throwable: Throwable) = throw throwable

    fun requestNext() {
        if (pendingRequestCount.get() == 0) {
            pendingRequestCount.incrementAndGet()
            request(1)
        }
    }
}
