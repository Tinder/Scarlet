/*
 * © 2013 - 2018 Tinder, Inc., ALL RIGHTS RESERVED
 */

package com.tinder.scarlet.internal.connection.subscriber

import com.tinder.scarlet.Event
import com.tinder.scarlet.WebSocket
import com.tinder.scarlet.internal.connection.Connection
import io.reactivex.subscribers.DisposableSubscriber

internal class WebSocketEventSubscriber(
    private val stateManager: Connection.StateManager
) : DisposableSubscriber<WebSocket.Event>() {
    override fun onNext(webSocketEvent: WebSocket.Event) =
        stateManager.handleEvent(Event.OnWebSocket.Event(webSocketEvent))

    override fun onComplete() = stateManager.handleEvent(Event.OnWebSocket.Terminate)

    override fun onError(throwable: Throwable) = throw throwable
}
