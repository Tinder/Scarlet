package com.tinder.scarlet.stomp.okhttp

import com.tinder.scarlet.Channel
import com.tinder.scarlet.Protocol
import com.tinder.scarlet.stomp.core.StompCommand
import com.tinder.scarlet.stomp.core.StompHeader
import com.tinder.scarlet.stomp.core.StompListener
import com.tinder.scarlet.stomp.core.StompMessage
import com.tinder.scarlet.stomp.core.StompSender
import com.tinder.scarlet.stomp.core.StompSubscriber
import com.tinder.scarlet.stomp.support.StompHeaderAccessor
import com.tinder.scarlet.stomp.support.StompMessageDecoder
import com.tinder.scarlet.stomp.support.StompMessageEncoder
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class OkHttpStompMainChannel(
    private val webSocketFactory: WebSocketFactory,
    private val listener: Channel.Listener
) : Channel, StompSender, StompSubscriber {

    companion object {

        private const val ACCEPT_VERSION = "1.1,1.2"
    }

    private val topicIds = ConcurrentHashMap<String, String>()
    private val subscriptions = ConcurrentHashMap<String, StompListener>()
    private var webSocket: WebSocket? = null

    override fun open(openRequest: Protocol.OpenRequest) {
        val clientOpenRequest = openRequest as OkHttpStompClient.ClientOpenRequest
        webSocketFactory.createWebSocket(
            clientOpenRequest.okHttpRequest,
            InnerWebSocketListener(clientOpenRequest)
        )
    }

    override fun forceClose() {
        disconnectMessage()
        webSocket?.cancel()
        webSocket = null
    }

    override fun close(closeRequest: Protocol.CloseRequest) {
        disconnectMessage()
        val clientCloseRequest = closeRequest as OkHttpStompClient.ClientCloseRequest
        webSocket?.close(clientCloseRequest.code, clientCloseRequest.reason)
        webSocket = null
    }

    override fun convertAndSend(
        payload: String,
        destination: String,
        headers: StompHeader?
    ): Boolean {
        val stompHeader = StompHeaderAccessor.of(headers.orEmpty())
            .apply { destination(destination) }
            .createHeader()

        val stompMessage = StompMessage.Builder()
            .withPayload(payload)
            .withHeaders(stompHeader)
            .create(StompCommand.SEND)

        return sendStompMessage(stompMessage)
    }

    override fun subscribe(
        destination: String,
        headers: StompHeader?,
        listener: StompListener
    ) {
        check(!topicIds.containsKey(destination)) { "Already has subscription to destination=$destination" }
        check(!subscriptions.containsKey(destination)) { "Already has subscription to destination=$destination" }
        val generateId = UUID.randomUUID().toString()
        val stompHeader = StompHeaderAccessor.of(headers.orEmpty())
            .apply {
                subscriptionId(generateId)
                destination(destination)
            }
            .createHeader()

        val stompMessage = StompMessage.Builder()
            .withHeaders(stompHeader)
            .create(StompCommand.SUBSCRIBE)

        sendStompMessage(stompMessage)

        topicIds[destination] = generateId
        subscriptions[destination] = listener
    }

    override fun unsubscribe(destination: String) {
        val subscriptionId = topicIds.remove(destination)
                ?: throw IllegalStateException("Unknown destination=$destination")

        val stompHeader = StompHeaderAccessor.of()
            .apply {
                subscriptionId(subscriptionId)
                destination(destination)
            }
            .createHeader()

        val stompMessage = StompMessage.Builder()
            .withHeaders(stompHeader)
            .create(StompCommand.UNSUBSCRIBE)

        sendStompMessage(stompMessage)
        subscriptions.remove(destination)
    }

    private fun handleIncome(stompMessage: StompMessage) = when (stompMessage.command) {
        StompCommand.CONNECTED -> listener.onOpened(this)
        StompCommand.MESSAGE -> {
            val destination = stompMessage.header.destination ?: throw IllegalStateException()
            val listener = subscriptions[destination]
            listener?.invoke(stompMessage)
        }
        StompCommand.UNKNOWN -> Unit //heart beat
        StompCommand.ERROR -> listener.onFailed(this, true, null)
        else -> Unit //not a server message
    }

    private fun sendStompMessage(stompMessage: StompMessage): Boolean {
        val text = StompMessageEncoder.encode(stompMessage)
        return webSocket?.send(text) ?: false
    }

    inner class InnerWebSocketListener(
        private val openRequest: OkHttpStompClient.ClientOpenRequest
    ) : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            this@OkHttpStompMainChannel.webSocket = webSocket
            val (host, login, passcode) = openRequest
            connectMessage(host, login, passcode)
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            val stompMessage = StompMessageDecoder.decode(bytes.utf8())
            handleIncome(stompMessage)
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            val stompMessage = StompMessageDecoder.decode(text)
            handleIncome(stompMessage)
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

    private fun connectMessage(host: String, login: String? = null, passcode: String? = null) {
        val stompHeader = StompHeaderAccessor.of()
            .apply {
                host(host)
                acceptVersion(ACCEPT_VERSION)
                login?.let(::login)
                passcode?.let(::passcode)
            }
            .createHeader()

        val stompMessage = StompMessage.Builder()
            .withHeaders(stompHeader)
            .create(StompCommand.CONNECT)

        sendStompMessage(stompMessage)
    }

    private fun disconnectMessage() {
        val stompMessage = StompMessage.Builder()
            .create(StompCommand.DISCONNECT)

        sendStompMessage(stompMessage)
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


