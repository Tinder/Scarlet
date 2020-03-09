/*
 * © 2018 Match Group, LLC.
 */
package com.tinder.scarlet.stomp.okhttp.client

import com.tinder.scarlet.Channel
import com.tinder.scarlet.Protocol
import com.tinder.scarlet.stomp.okhttp.core.Connection
import com.tinder.scarlet.stomp.okhttp.core.IdGenerator
import com.tinder.scarlet.stomp.okhttp.core.MessageHandler
import com.tinder.scarlet.stomp.okhttp.core.StompListener
import com.tinder.scarlet.stomp.okhttp.core.StompSender
import com.tinder.scarlet.stomp.okhttp.core.StompSubscriber
import com.tinder.scarlet.stomp.okhttp.core.WebSocketFactory
import com.tinder.scarlet.stomp.okhttp.models.StompCommand
import com.tinder.scarlet.stomp.okhttp.models.StompHeader
import com.tinder.scarlet.stomp.okhttp.models.StompMessage
import com.tinder.scarlet.stomp.okhttp.support.StompHeaderAccessor
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max

/**
 * The main channel, which is responsible for connecting and disconnecting to the stomp server.
 * And also for sending messages and the logic of subscriptions.
 */
class OkHttpStompMainChannel(
    private val configuration: Configuration,
    private val idGenerator: IdGenerator,
    private val webSocketFactory: WebSocketFactory,
    private val listener: Channel.Listener
) : Channel, StompSender,
    StompSubscriber {

    companion object {

        /** STOMP recommended error of margin for receiving heartbeats.  */
        private const val HEARTBEAT_MULTIPLIER = 3

        private const val ACCEPT_VERSION = "1.1,1.2"
    }

    private val topicIds = ConcurrentHashMap<String, String>()
    private val subscriptions = ConcurrentHashMap<String, StompListener>()

    private var messageHandler: MessageHandler? = null
    private var connection: Connection? = null

    override fun open(openRequest: Protocol.OpenRequest) {
        val clientOpenRequest = openRequest as OkHttpStompClient.ClientOpenRequest

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
        payload: ByteArray,
        destination: String,
        headers: StompHeader?
    ): Boolean {
        val stompHeaders = StompHeaderAccessor.of(headers.orEmpty())
            .apply { this.destination = destination }
            .createHeader()

        val stompMessage = StompMessage.Builder()
            .withPayload(payload)
            .withHeaders(stompHeaders)
            .create(StompCommand.SEND)

        return connection?.sendMessage(stompMessage) ?: false
    }

    override fun subscribe(
        destination: String,
        headers: StompHeader?,
        listener: StompListener
    ) {
        check(!topicIds.containsKey(destination)) { "Already has subscription to destination=$destination" }
        check(!subscriptions.containsKey(destination)) { "Already has subscription to destination=$destination" }
        val generateId = idGenerator.generateId()
        val stompHeaders = StompHeaderAccessor.of(headers.orEmpty())
            .apply {
                this.subscriptionId = generateId
                this.destination = destination
            }
            .createHeader()

        val stompMessage = StompMessage.Builder()
            .withHeaders(stompHeaders)
            .create(StompCommand.SUBSCRIBE)

        connection?.sendMessage(stompMessage)

        topicIds[destination] = generateId
        subscriptions[destination] = listener
    }

    override fun unsubscribe(destination: String) {
        val subscriptionId = topicIds.remove(destination) ?: return

        val stompHeaders = StompHeaderAccessor.of()
            .apply { this.subscriptionId = subscriptionId }
            .createHeader()

        val stompMessage = StompMessage.Builder()
            .withHeaders(stompHeaders)
            .create(StompCommand.UNSUBSCRIBE)

        connection?.sendMessage(stompMessage)
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
        else -> Unit // not a server message
    }

    private fun setupHeartBeat(stompMessage: StompMessage) {
        val (serverSendInterval, serverReceiveInterval) = stompMessage.headers.heartBeat

        val clientSendInterval = configuration.heartbeatSendInterval
        val clientReceiveInterval = configuration.heartbeatReceiveInterval

        if (clientSendInterval > 0 && serverReceiveInterval > 0) {
            val interval = max(clientSendInterval, serverReceiveInterval)
            connection?.onWriteInactivity(interval, ::sendHeartBeat)
        }

        if (clientReceiveInterval > 0 && serverSendInterval > 0) {
            val interval = max(clientReceiveInterval, serverSendInterval) * HEARTBEAT_MULTIPLIER
            connection?.onReceiveInactivity(interval) {
                sendErrorMessage("No messages received in $interval ms.")
                connection?.close()
                listener.onFailed(this@OkHttpStompMainChannel, true, null)
            }
        }
    }

    private fun sendHeartBeat() {
        val stompMessage = StompMessage.Builder()
            .create(StompCommand.HEARTBEAT)

        connection?.sendMessage(stompMessage)
    }

    private fun sendErrorMessage(error: String) {
        val headers = StompHeaderAccessor.of()
            .apply { message(error) }
            .createHeader()

        val stompMessage = StompMessage.Builder()
            .withHeaders(headers)
            .create(StompCommand.ERROR)

        connection?.sendMessage(stompMessage)
    }

    inner class InnerWebSocketListener(
        private val openRequest: OkHttpStompClient.ClientOpenRequest
    ) : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            val webSocketConnection = WebSocketConnection(webSocket)

            this@OkHttpStompMainChannel.connection = webSocketConnection
            this@OkHttpStompMainChannel.messageHandler = webSocketConnection

            val host = configuration.host
            val login = openRequest.login
            val passcode = openRequest.passcode

            sendConnectMessage(host, login, passcode)
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            messageHandler?.handle(bytes.toByteArray())?.let(::handleIncome)
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            messageHandler?.handle(text.toByteArray(Charsets.UTF_8))?.let(::handleIncome)
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
        val stompHeaderAccessor = StompHeaderAccessor.of()
            .apply {
                this.host = host
                this.acceptVersion =
                    ACCEPT_VERSION
                this.login = login
                this.passcode = passcode
            }

        val clientSendInterval = configuration.heartbeatSendInterval
        val clientReceiveInterval = configuration.heartbeatReceiveInterval

        if (clientSendInterval > 0 && clientReceiveInterval > 0) {
            stompHeaderAccessor.heartBeat = clientSendInterval to clientReceiveInterval
        }

        val stompMessage = StompMessage.Builder()
            .withHeaders(stompHeaderAccessor.createHeader())
            .create(StompCommand.CONNECT)

        connection?.sendMessage(stompMessage)
    }

    private fun sendDisconnectMessage() {
        val stompMessage = StompMessage.Builder()
            .create(StompCommand.DISCONNECT)

        connection?.sendMessage(stompMessage)
    }

    data class Configuration(
        val host: String,
        val heartbeatSendInterval: Long = 0,
        val heartbeatReceiveInterval: Long = 0
    )

    class Factory(
        private val idGenerator: IdGenerator,
        private val configuration: Configuration,
        private val webSocketFactory: WebSocketFactory
    ) : Channel.Factory {

        override fun create(
            listener: Channel.Listener,
            parent: Channel?
        ): Channel? {
            return OkHttpStompMainChannel(
                configuration = configuration,
                idGenerator = idGenerator,
                webSocketFactory = webSocketFactory,
                listener = listener
            )
        }
    }
}
