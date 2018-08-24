/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.websocket.okhttp

import com.tinder.scarlet.Message
import com.tinder.scarlet.ShutdownReason
import com.tinder.scarlet.Stream
import com.tinder.scarlet.WebSocket
import com.tinder.scarlet.utils.toStream
import okhttp3.WebSocketListener
import okio.ByteString

class OkHttpWebSocket internal constructor(
    private val okHttpWebSocketHolder: OkHttpWebSocketHolder,
    private val okHttpWebSocketEventObserver: OkHttpWebSocketEventObserver,
    private val connectionEstablisher: ConnectionEstablisher
) : WebSocket {

    override fun open(): Stream<WebSocket.Event> = okHttpWebSocketEventObserver.observe()
        .doOnSubscribe {
            connectionEstablisher.establishConnection(okHttpWebSocketEventObserver)
        }
        .doOnNext(this::handleWebSocketEvent)
        .toStream()

    @Synchronized
    override fun send(message: Message): Boolean = when (message) {
        is Message.Text -> okHttpWebSocketHolder.send(message.value)
        is Message.Bytes -> {
            val bytes = message.value
            val byteString = ByteString.of(bytes, 0, bytes.size)
            okHttpWebSocketHolder.send(byteString)
        }
    }

    @Synchronized
    override fun close(shutdownReason: ShutdownReason): Boolean {
        val (code, reasonText) = shutdownReason
        return okHttpWebSocketHolder.close(code, reasonText)
    }

    @Synchronized
    override fun cancel() = okHttpWebSocketHolder.cancel()

    private fun handleWebSocketEvent(event: WebSocket.Event) {
        when (event) {
            is WebSocket.Event.OnConnectionOpened<*> ->
                okHttpWebSocketHolder.initiate(event.webSocket as okhttp3.WebSocket)
            is WebSocket.Event.OnConnectionClosing -> close(ShutdownReason.GRACEFUL)
            is WebSocket.Event.OnConnectionClosed, is WebSocket.Event.OnConnectionFailed -> handleConnectionShutdown()
        }
    }

    @Synchronized
    private fun handleConnectionShutdown() {
        okHttpWebSocketHolder.shutdown()
        okHttpWebSocketEventObserver.terminate()
    }

    interface ConnectionEstablisher {
        fun establishConnection(webSocketListener: WebSocketListener)
    }

    class Factory(
        private val connectionEstablisher: ConnectionEstablisher
    ) : WebSocket.Factory {
        override fun create(): WebSocket =
            OkHttpWebSocket(OkHttpWebSocketHolder(), OkHttpWebSocketEventObserver(), connectionEstablisher)
    }
}
