/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.websocket.oksse

import com.here.oksse.ServerSentEvent
import com.squareup.moshi.KotlinJsonAdapterFactory
import com.squareup.moshi.Moshi
import com.tinder.scarlet.Message
import com.tinder.scarlet.ShutdownReason
import com.tinder.scarlet.Stream
import com.tinder.scarlet.WebSocket
import com.tinder.scarlet.utils.toStream
import com.tinder.scarlet.websocket.oksse.model.ServerSentMessage

class OkSseWebSocket internal constructor(
    private val serverSentEventHolder: ServerSentEventHolder,
    private val okSseWebSocketEventObserver: OkSseWebSocketEventObserver,
    private val connectionEstablisher: ConnectionEstablisher

) : WebSocket {

    override fun open(): Stream<WebSocket.Event> {
        return okSseWebSocketEventObserver.observe()
            .doOnSubscribe {
                connectionEstablisher.establishConnection(okSseWebSocketEventObserver)
            }
            .doOnNext(::handleWebSocketEvent)
            .toStream()
    }

    override fun send(message: Message): Boolean {
        return false
    }

    override fun close(shutdownReason: ShutdownReason): Boolean {
        serverSentEventHolder.close()
        return true
    }

    override fun cancel() {
        serverSentEventHolder.close()
    }

    private fun handleWebSocketEvent(event: WebSocket.Event) {
        when (event) {
            is WebSocket.Event.OnConnectionOpened<*> ->
                serverSentEventHolder.initiate(event.webSocket as ServerSentEvent)
            is WebSocket.Event.OnConnectionClosed, is WebSocket.Event.OnConnectionFailed -> handleConnectionShutdown()
        }
    }

    @Synchronized
    private fun handleConnectionShutdown() {
        serverSentEventHolder.shutdown()
        okSseWebSocketEventObserver.terminate()
    }

    interface ConnectionEstablisher {
        fun establishConnection(sseListener: ServerSentEvent.Listener)
    }

    class Factory(
        private val connectionEstablisher: ConnectionEstablisher
    ) : WebSocket.Factory {
        override fun create(): WebSocket {
            return OkSseWebSocket(
                ServerSentEventHolder(),
                OkSseWebSocketEventObserver(MESSAGE_JSON_ADAPTER),
                connectionEstablisher
            )
        }

        private companion object {
            private val MOSHI = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()
            private val MESSAGE_JSON_ADAPTER = MOSHI.adapter(ServerSentMessage::class.java)
        }
    }
}
