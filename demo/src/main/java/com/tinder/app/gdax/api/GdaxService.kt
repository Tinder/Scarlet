/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.app.gdax.api

import com.tinder.app.gdax.api.model.Subscribe
import com.tinder.app.gdax.api.model.Ticker
import com.tinder.scarlet.WebSocket
import com.tinder.scarlet.ws.Receive
import com.tinder.scarlet.ws.Send
import io.reactivex.Flowable

interface GdaxService {
    @Receive
    fun observeWebSocketEvent(): Flowable<WebSocket.Event>

    @Send
    fun sendSubscribe(subscribe: Subscribe)

    @Receive
    fun observeTicker(): Flowable<Ticker>
}
