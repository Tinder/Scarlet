/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.socketio.client

import com.tinder.scarlet.Channel
import com.tinder.scarlet.Message
import com.tinder.scarlet.MessageQueue
import com.tinder.scarlet.Protocol
import com.tinder.scarlet.ProtocolSpecificEventAdapter
import com.tinder.scarlet.socketio.SocketIoEvent
import com.tinder.scarlet.utils.SimpleChannelFactory
import com.tinder.scarlet.utils.SimpleProtocolOpenRequestFactory
import io.socket.client.IO
import io.socket.client.Manager
import io.socket.client.Socket
import io.socket.engineio.client.Transport
import org.json.JSONObject

class SocketIoClient(
    private val url: () -> String,
    private val requestHeaders: () -> RequestHeaders = { RequestHeaders(mapOf()) },
    private val options: IO.Options = IO.Options()
) : Protocol {

    override fun createChannelFactory(): Channel.Factory {
        return SimpleChannelFactory { listener, _ ->
            SocketIoMainChannel(
                options,
                listener
            )
        }
    }

    override fun createOpenRequestFactory(channel: Channel): Protocol.OpenRequest.Factory {
        return SimpleProtocolOpenRequestFactory {
            MainChannelOpenRequest(url(), requestHeaders())
        }
    }

    override fun createEventAdapterFactory(): ProtocolSpecificEventAdapter.Factory {
        return SocketIoEvent.Adapter.Factory()
    }

    data class MainChannelOpenRequest(
        val url: String,
        val requestHeaders: RequestHeaders
    ) : Protocol.OpenRequest

    data class RequestHeaders(val headers: Map<String, String>)
}

class SocketIoEventName(
    private val eventName: String
) : Protocol {

    override fun createChannelFactory(): Channel.Factory {
        return SimpleChannelFactory { listener, parent ->
            require(parent is SocketIoMainChannel)
            SocketIoMessageChannel(
                parent as SocketIoMainChannel,
                eventName,
                listener
            )
        }
    }

    override fun createEventAdapterFactory(): ProtocolSpecificEventAdapter.Factory {
        return SocketIoEvent.Adapter.Factory()
    }
}

internal class SocketIoMainChannel(
    private val options: IO.Options,
    private val listener: Channel.Listener
) : Channel {
    var socket: Socket? = null

    override fun open(openRequest: Protocol.OpenRequest) {
        val mainChannelOpenRequest = openRequest as SocketIoClient.MainChannelOpenRequest
        val socket = IO.socket(mainChannelOpenRequest.url, options).apply {
            addRequestHeaders(mainChannelOpenRequest.requestHeaders)
        }

        socket
            .on(Socket.EVENT_CONNECT) {
                listener.onOpened(this)
            }
            .on(Socket.EVENT_DISCONNECT) {
                listener.onClosed(this)
            }
            .on(Socket.EVENT_ERROR) {
                listener.onFailed(this, true, null)
            }
        socket.open()
        this.socket = socket
    }

    override fun close(closeRequest: Protocol.CloseRequest) {
        socket?.disconnect()
        socket = null
    }

    override fun forceClose() {
        socket?.disconnect()
        socket = null
    }

    override fun createMessageQueue(listener: MessageQueue.Listener): MessageQueue? {
        return null
    }
}

internal class SocketIoMessageChannel(
    private val parent: SocketIoMainChannel,
    private val eventName: String,
    private val listener: Channel.Listener
) : Channel, MessageQueue {

    private var socket: Socket? = null
    private var messageQueueListener: MessageQueue.Listener? = null

    override fun open(openRequest: Protocol.OpenRequest) {
        socket = parent.socket
        if (socket == null) {
            listener.onFailed(this, true, IllegalStateException("socket is null"))
            return
        }
        socket?.on(eventName) {
            val value = it[0]
            when (value) {
                is JSONObject -> {
                    messageQueueListener?.onMessageReceived(
                        this, this,
                        Message.Text(value.toString())
                    )
                }
                is String -> {
                    messageQueueListener?.onMessageReceived(
                        this, this,
                        Message.Text(value.toString())
                    )
                }
                is ByteArray -> {
                    messageQueueListener?.onMessageReceived(
                        this, this,
                        Message.Bytes(value)
                    )
                }
            }
        }
        listener.onOpened(this)
    }

    override fun close(closeRequest: Protocol.CloseRequest) {
        socket?.off(eventName)
        socket = null
        listener.onClosed(this)
    }

    override fun forceClose() {
        socket?.off(eventName)
        socket = null
        listener.onClosed(this)
    }

    override fun createMessageQueue(listener: MessageQueue.Listener): MessageQueue {
        require(messageQueueListener == null)
        messageQueueListener = listener
        return this
    }

    override fun send(message: Message, messageMetaData: Protocol.MessageMetaData): Boolean {
        val socket = socket ?: return false
        when (message) {
            is Message.Text -> socket.emit(eventName, message.value)
            is Message.Bytes -> socket.emit(eventName, message.value)
        }
        return true
    }
}

fun Socket.addRequestHeaders(requestHeaders: SocketIoClient.RequestHeaders) {
    io().on(Manager.EVENT_TRANSPORT) { transportData ->
        val transport = transportData[0] as Transport

        transport.on(Transport.EVENT_REQUEST_HEADERS) { headersData ->
            @Suppress("UNCHECKED_CAST")
            val headersOut = headersData[0] as MutableMap<String, List<String>>

            requestHeaders.headers.forEach { (key, value) ->
                headersOut[key] = listOf(value)
            }
        }
    }
}