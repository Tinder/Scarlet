package com.tinder.scarlet.stomp.okhttp

import com.tinder.scarlet.Channel
import com.tinder.scarlet.Message
import com.tinder.scarlet.MessageQueue
import com.tinder.scarlet.Protocol
import com.tinder.scarlet.stomp.core.StompCommand
import com.tinder.scarlet.stomp.core.StompListener
import com.tinder.scarlet.stomp.core.StompMessage
import com.tinder.scarlet.stomp.core.StompSubscriber
import com.tinder.scarlet.stomp.support.StompHeaderAccessor
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.util.concurrent.ConcurrentHashMap

class OkHttpStompMainChannel(
    private val webSocketFactory: WebSocketFactory,
    private val listener: Channel.Listener
) : Channel, MessageQueue, StompSubscriber {

    private val topicIds = ConcurrentHashMap<String, String>()
    private val stompListeners = ConcurrentHashMap<String, StompListener>()
    private var webSocket: WebSocket? = null

    override fun open(openRequest: Protocol.OpenRequest) {
        val clientOpenRequest = openRequest as OkHttpStompClient.ClientOpenRequest
        webSocketFactory.createWebSocket(clientOpenRequest.okHttpRequest, InnerWebSocketListener())
    }

    override fun forceClose() {
        TODO("Implement send disconnect message")
        webSocket?.cancel()
        webSocket = null
    }

    override fun close(closeRequest: Protocol.CloseRequest) {
        TODO("Implement send disconnect message")

        val clientCloseRequest = closeRequest as OkHttpStompClient.ClientCloseRequest
        webSocket?.close(clientCloseRequest.code, clientCloseRequest.reason)
        webSocket = null
    }

    override fun send(message: Message, messageMetaData: Protocol.MessageMetaData): Boolean {
        val metaData = messageMetaData as OkHttpStompClient.MessageMetaData
        val messageValue = message as Message.Text
        val stompHeader = StompHeaderAccessor.ofHeaders(StompCommand.SEND)
            .apply {
                destination(metaData.destination)
                contentType(metaData.contentType)
            }
            .createHeader()
        val stompMessage = StompMessage(messageValue.value, stompHeader)
        return sendStompMessage(stompMessage)
    }

    override fun subscribe(
        destination: String,
        headers: Map<String, String>,
        listener: StompListener
    ) {
        val stompHeader = StompHeaderAccessor.ofHeaders(StompCommand.SUBSCRIBE, headers)
            .apply { destination(destination) }
            .createHeader()
        topicIds[destination] = stompHeader.id
        val stompMessage = StompMessage("", stompHeader)
        sendStompMessage(stompMessage)
    }

    override fun unsubscribe(destination: String) {
        TODO("send unsubscribe message")
        stompListeners.remove(destination)
    }

    private fun sendStompMessage(stompMessage: StompMessage): Boolean {
        val text = StompMessageEncoder.encode(stompMessage)
        return webSocket?.send(text) ?: false
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


