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
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max

class OkHttpStompMainChannel(
    private val webSocketFactory: WebSocketFactory,
    private val listener: Channel.Listener
) : Channel, StompSender, StompSubscriber {

    companion object {

        /** STOMP recommended error of margin for receiving heartbeats.  */
        private const val HEARTBEAT_MULTIPLIER = 3

        private const val ACCEPT_VERSION = "1.1,1.2"
    }

    private val topicIds = ConcurrentHashMap<String, String>()
    private val subscriptions = ConcurrentHashMap<String, StompListener>()

    private var messageHandler: MessageHandler? = null
    private var connection: Connection? = null

    private var clientSendInterval: Long = 0
    private var clientReceiveInterval: Long = 0

    override fun open(openRequest: Protocol.OpenRequest) {
        val clientOpenRequest = openRequest as OkHttpStompClient.ClientOpenRequest

        this.clientSendInterval = clientOpenRequest.heartbeatSendInterval
        this.clientReceiveInterval = clientOpenRequest.heartbeatReceiveInterval

        webSocketFactory.createWebSocket(
            clientOpenRequest.okHttpRequest,
            InnerWebSocketListener(clientOpenRequest)
        )
    }

    override fun forceClose() {
        topicIds.clear()
        subscriptions.clear()

        sendDisconnectMessage()
        connection?.forceClose()

        connection = null
        messageHandler = null
    }

    override fun close(closeRequest: Protocol.CloseRequest) {
        topicIds.clear()
        subscriptions.clear()

        sendDisconnectMessage()

        connection?.close()

        connection = null
        messageHandler = null
    }

    override fun convertAndSend(
        payload: String,
        destination: String,
        headers: StompHeader?
    ): Boolean {
        val stompHeaders = StompHeaderAccessor.of(headers.orEmpty())
            .apply { destination(destination) }
            .createHeader()

        val stompMessage = StompMessage.Builder()
            .withPayload(payload)
            .withHeaders(stompHeaders)
            .create(StompCommand.SEND)

        return connection?.send(stompMessage) ?: false
    }

    override fun subscribe(
        destination: String,
        headers: StompHeader?,
        listener: StompListener
    ) {
        check(!topicIds.containsKey(destination)) { "Already has subscription to destination=$destination" }
        check(!subscriptions.containsKey(destination)) { "Already has subscription to destination=$destination" }
        val generateId = UUID.randomUUID().toString()
        val stompHeaders = StompHeaderAccessor.of(headers.orEmpty())
            .apply {
                subscriptionId(generateId)
                destination(destination)
            }
            .createHeader()

        val stompMessage = StompMessage.Builder()
            .withHeaders(stompHeaders)
            .create(StompCommand.SUBSCRIBE)

        connection?.send(stompMessage)

        topicIds[destination] = generateId
        subscriptions[destination] = listener
    }

    override fun unsubscribe(destination: String) {
        val subscriptionId = topicIds.remove(destination)
                ?: throw IllegalStateException("Unknown destination=$destination")

        val stompHeaders = StompHeaderAccessor.of()
            .apply { subscriptionId(subscriptionId) }
            .createHeader()

        val stompMessage = StompMessage.Builder()
            .withHeaders(stompHeaders)
            .create(StompCommand.UNSUBSCRIBE)

        connection?.send(stompMessage)
        subscriptions.remove(destination)
    }

    private fun handleIncome(stompMessage: StompMessage) = when (stompMessage.command) {
        StompCommand.CONNECTED -> {
            setupHeartBeat(stompMessage)
            listener.onOpened(this)
        }
        StompCommand.MESSAGE -> stompMessage.headers.destination
            ?.let { destination ->
                val listener = subscriptions[destination]
                listener?.invoke(stompMessage)
            }
        StompCommand.ERROR -> listener.onFailed(this, true, null)
        else -> Unit //not a server message
    }

    private fun setupHeartBeat(stompMessage: StompMessage) {
        val (serverSendInterval, serverReceiveInterval) = stompMessage.headers.heartBeat

        if (clientSendInterval > 0 && serverReceiveInterval > 0) {
            val interval = max(clientSendInterval, serverReceiveInterval)
            connection?.onWriteInactivity(interval) { sendHeartBeat() }
        }

        if (clientReceiveInterval > 0 && serverSendInterval > 0) {
            val interval = max(clientReceiveInterval, serverSendInterval) * HEARTBEAT_MULTIPLIER;
            connection?.onReadInactivity(interval) {
                sendErrorMessage("No messages received in $interval ms.")
                connection?.close()
                listener.onFailed(this@OkHttpStompMainChannel, true, null)
            }
        }

    }

    private fun sendHeartBeat() {
        val stompMessage = StompMessage.Builder()
            .create(StompCommand.UNKNOWN)

        connection?.send(stompMessage)
    }

    private fun sendErrorMessage(error: String) {
        val headers = StompHeaderAccessor.of()
            .apply { message(error) }
            .createHeader()

        val stompMessage = StompMessage.Builder()
            .withHeaders(headers)
            .create(StompCommand.ERROR)

        connection?.send(stompMessage)
    }

    inner class InnerWebSocketListener(
        private val openRequest: OkHttpStompClient.ClientOpenRequest
    ) : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            val webSocketConnection = WebSocketConnection(webSocket)

            this@OkHttpStompMainChannel.connection = webSocketConnection
            this@OkHttpStompMainChannel.messageHandler = webSocketConnection

            val host = openRequest.host
            val login = openRequest.login
            val passcode = openRequest.passcode

            sendConnectMessage(host, login, passcode)
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            messageHandler?.handle(bytes.utf8())?.let(::handleIncome)
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            messageHandler?.handle(text)?.let(::handleIncome)
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            listener.onClosing(this@OkHttpStompMainChannel)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            listener.onClosed(this@OkHttpStompMainChannel)
            this@OkHttpStompMainChannel.connection = null
        }

        override fun onFailure(webSocket: WebSocket, throwable: Throwable, response: Response?) {
            listener.onFailed(this@OkHttpStompMainChannel, true, throwable)
            this@OkHttpStompMainChannel.connection = null
        }

    }

    private fun sendConnectMessage(host: String, login: String? = null, passcode: String? = null) {
        val stompHeaders = StompHeaderAccessor.of()
            .apply {
                host(host)
                acceptVersion(ACCEPT_VERSION)
                login?.let(::login)
                passcode?.let(::passcode)

                if (clientSendInterval > 0 && clientReceiveInterval > 0) {
                    heartBeat(clientSendInterval, clientReceiveInterval)
                }

            }
            .createHeader()

        val stompMessage = StompMessage.Builder()
            .withHeaders(stompHeaders)
            .create(StompCommand.CONNECT)

        connection?.send(stompMessage)
    }

    private fun sendDisconnectMessage() {
        val stompMessage = StompMessage.Builder()
            .create(StompCommand.DISCONNECT)

        connection?.send(stompMessage)
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
