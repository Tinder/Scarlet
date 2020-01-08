package com.tinder.scarlet.stomp.okhttp

import com.tinder.scarlet.Channel
import com.tinder.scarlet.Protocol
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

class OkHttpStompMainChannel(
    private val webSocketFactory: WebSocketFactory,
    private val listener: Channel.Listener
) : Channel {

    private var webSocket: WebSocket? = null

    override fun open(openRequest: Protocol.OpenRequest) {
        val openSocketRequest = openRequest as OkHttpStompClient.ClientOpenSocketRequest
        webSocketFactory.createWebSocket(openSocketRequest.okHttpRequest, InnerWebSocketListener())
    }

    override fun forceClose() {
        TODO("Implement send disconnect message")
        webSocket?.cancel()
        webSocket = null
    }

    override fun close(closeRequest: Protocol.CloseRequest) {
        TODO("Implement send disconnect message")
        webSocket?.close(1000, "")//todo add code and reason
        webSocket = null
    }

    fun sendMessage(destination: String, message: String, headers: Map<String, String>): Boolean {
        TODO("Implement send message to destination")
    }

    fun subscribe(
        destination: String,
        headers: Map<String, String>,
        listener: (String, Map<String, String>) -> Unit
    ) {
        TODO("Implement subscribe to destination")
    }

    fun unSubscribe(destination: String) {
        TODO("Implement un subscribe from destination")
    }

    inner class InnerWebSocketListener : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            this@OkHttpStompMainChannel.webSocket = webSocket
            TODO("Send connect message")
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            TODO("Implement handle stomp message")
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            TODO("Implement handle stomp message")
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            listener.onClosing(this@OkHttpStompMainChannel)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            listener.onClosed(this@OkHttpStompMainChannel)
            this@OkHttpStompMainChannel.webSocket = null
        }

        override fun onFailure(webSocket: WebSocket, throwable: Throwable, response: Response?) {
            listener.onFailed(this@OkHttpStompMainChannel, true, throwable)
            this@OkHttpStompMainChannel.webSocket = null
        }
    }

    class Factory(
        private val webSocketFactory: WebSocketFactory
    ) : Channel.Factory {

        override fun create(
            listener: Channel.Listener,
            parent: Channel?
        ): Channel? {
            return OkHttpStompMainChannel(
                webSocketFactory,
                listener
            )
        }
    }


}


