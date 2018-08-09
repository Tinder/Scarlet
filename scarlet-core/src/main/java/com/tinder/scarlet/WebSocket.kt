/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet

/**
 * A non-blocking interface to a WebSocket.
 */
interface WebSocket {

    /**
     * Opens the connection and return a [stream][Stream] of [events][Event] that terminates when the connection is
     * closed.
     */
    fun open(): Stream<Event>

    /**
     * Sends a message.
     *
     * @return true if the message is enqueued.
     */
    fun send(message: Message): Boolean

    /**
     * Closes the connection after sending all pending messages.
     *
     * @return true if a graceful shutdown is initiated.
     */
    fun close(shutdownReason: ShutdownReason): Boolean

    /**
     * Closes the connection and discard all pending messages
     */
    fun cancel()

    /**
     * Represents the lifecycle of a [WebSocket] connection.
     */
    sealed class Event {
        /**
         * Invoked when a WebSocket has been accepted by the remote peer and may begin transmitting messages.
         *
         * @property webSocket The `WebSocket` instance used for this connection.
         */
        data class OnConnectionOpened<out WEB_SOCKET : Any>(val webSocket: WEB_SOCKET) : Event()

        /**
         * Invoked when a [text message][Message.Text] or [binary message][Message.Bytes] has been received.
         *
         * @property message The raw message.
         */
        data class OnMessageReceived(val message: Message) : Event()

        /**
         * Invoked when the peer has indicated that no more incoming messages will be transmitted.
         *
         * @property shutdownReason Reason to shutdown from the peer.
         */
        data class OnConnectionClosing(val shutdownReason: ShutdownReason) : Event()

        /**
         * Invoked when both peers have indicated that no more messages will be transmitted and the connection has been
         * successfully released. No further calls to this listener will be made.
         *
         * @property shutdownReason Reason to shutdown from the peer.
         */
        data class OnConnectionClosed(val shutdownReason: ShutdownReason) : Event()

        /**
         * Invoked when a web socket has been closed due to an error reading from or writing to the network. Both outgoing
         * and incoming messages may have been lost. No further calls to this listener will be made.
         *
         * @property throwable The error causing the failure.
         */
        data class OnConnectionFailed(val throwable: Throwable) : Event()
    }

    interface Factory {
        fun create(): WebSocket
    }
}
