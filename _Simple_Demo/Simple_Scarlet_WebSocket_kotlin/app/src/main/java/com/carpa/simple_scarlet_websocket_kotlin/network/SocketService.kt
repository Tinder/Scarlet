package com.carpa.simple_scarlet_websocket_kotlin.network

import com.tinder.scarlet.WebSocket
import com.tinder.scarlet.ws.Receive
import com.tinder.scarlet.ws.Send
import io.reactivex.Flowable

/**
 * An interface is needed to make the WebSocket work:
 * - send data
 * - receive data
 * (As in Retrofit library)
 */
interface SocketService {
    @Receive
    fun observeText(): Flowable<Msg>
    @Send
    fun sendText(text: Msg)
    @Receive
    fun observeWebSocketEvent():
            Flowable<WebSocket.Event>
}