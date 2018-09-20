/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.websocket

import com.tinder.scarlet.Message
import com.tinder.scarlet.utils.getRawType
import com.tinder.scarlet.v2.Protocol
import com.tinder.scarlet.v2.ProtocolEvent
import okhttp3.Response
import okhttp3.WebSocket
import java.lang.reflect.Type

sealed class WebSocketEvent {
    /**
     * Invoked when a WebSocket has been accepted by the remote peer and may begin transmitting messages.
     *
     * @property webSocket The `WebSocket` instance used for this connection.
     */
    data class OnConnectionOpened(val okHttpWebSocket: WebSocket, val okHttpResponse: Response) : WebSocketEvent()

    /**
     * Invoked when a [text message][Message.Text] or [binary message][Message.Bytes] has been received.
     *
     * @property message The raw message.
     */
    data class OnMessageReceived(val message: Message) : WebSocketEvent()

    /**
     * Invoked when the peer has indicated that no more incoming messages will be transmitted.
     *
     * @property shutdownReason Reason to shutdown from the peer.
     */
    data class OnConnectionClosing(val shutdownReason: ShutdownReason) : WebSocketEvent()

    /**
     * Invoked when both peers have indicated that no more messages will be transmitted and the connection has been
     * successfully released. No further calls to this listener will be made.
     *
     * @property shutdownReason Reason to shutdown from the peer.
     */
    data class OnConnectionClosed(val shutdownReason: ShutdownReason) : WebSocketEvent()

    /**
     * Invoked when a web socket has been closed due to an error reading from or writing to the network. Both outgoing
     * and incoming messages may have been lost. No further calls to this listener will be made.
     *
     * @property throwable The error causing the failure.
     */
    data class OnConnectionFailed(val throwable: Throwable) : WebSocketEvent()

    class Adapter : ProtocolEvent.Adapter<WebSocketEvent> {
        override fun fromEvent(event: ProtocolEvent): WebSocketEvent {
            return when (event) {
                is ProtocolEvent.OnOpened -> {
                    val response = event.response as OkHttpWebSocket.OpenResponse
                    WebSocketEvent.OnConnectionOpened(response.okHttpWebSocket, response.okHttpResponse)
                }
                is ProtocolEvent.OnMessageReceived -> {
                    WebSocketEvent.OnMessageReceived(event.message)
                }
                is ProtocolEvent.OnClosing -> {
                    val response = event.response as OkHttpWebSocket.CloseResponse
                    WebSocketEvent.OnConnectionClosing(response.shutdownReason)
                }
                is ProtocolEvent.OnClosed -> {
                    val response = event.response as OkHttpWebSocket.CloseResponse
                    WebSocketEvent.OnConnectionClosed(response.shutdownReason)
                }
                is ProtocolEvent.OnFailed -> {
                    WebSocketEvent.OnConnectionFailed(event.throwable ?: Throwable())
                }
                else -> throw IllegalArgumentException()
            }
        }

        class Factory : ProtocolEvent.Adapter.Factory {
            override fun create(type: Type, annotations: Array<Annotation>): ProtocolEvent.Adapter<*> {
                val receivingClazz = type.getRawType()
                require(WebSocketEvent::class.java.isAssignableFrom(receivingClazz)) {
                    "Only subclasses of WebSocketEvent are supported"
                }
                return Adapter()
            }
        }
    }
}
