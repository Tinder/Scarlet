/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.app.sse.api

import com.tinder.app.sse.api.model.SseMessage
import com.tinder.scarlet.websocket.WebSocketEvent
import com.tinder.scarlet.ws.Receive
import io.reactivex.Flowable

interface StockMarketService {
    @Receive
    fun observeWebSocketEvent(): Flowable<WebSocketEvent>

    @Receive
    fun observeMessage(): Flowable<SseMessage>
}
