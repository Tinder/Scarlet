/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.internal.connection.subscriber

import com.tinder.scarlet.Event
import com.tinder.scarlet.internal.connection.Connection
import io.reactivex.subscribers.DisposableSubscriber

internal class RetryTimerSubscriber(
    private val stateManager: Connection.StateManager
) : DisposableSubscriber<Long>() {
    override fun onNext(t: Long) = stateManager.handleEvent(Event.OnRetry)

    override fun onComplete() {
    }

    override fun onError(throwable: Throwable) = throw throwable
}
