/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.app.websocket.gdax.api

import com.tinder.app.websocket.gdax.api.model.Subscribe
import com.tinder.app.websocket.gdax.api.model.Ticker
import com.tinder.scarlet.websocket.WebSocketEvent
import com.tinder.scarlet.ws.Receive
import com.tinder.scarlet.ws.Send
import io.reactivex.Flowable

interface GdaxService {
    @Receive
    fun observeWebSocketEvent(): Flowable<WebSocketEvent>

    @Send
    fun sendSubscribe(subscribe: Subscribe)

    @Receive
    fun observeTicker(): Flowable<Ticker>
}
