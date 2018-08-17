/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.websocket

import com.tinder.scarlet.Message

sealed class WebSocketEvent {
    /**
     * Invoked when a WebSocket has been accepted by the remote peer and may begin transmitting messages.
     *
     * @property webSocket The `WebSocket` instance used for this connection.
     */
    data class OnConnectionOpened(val webSocket: Any) : WebSocketEvent()

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
}
