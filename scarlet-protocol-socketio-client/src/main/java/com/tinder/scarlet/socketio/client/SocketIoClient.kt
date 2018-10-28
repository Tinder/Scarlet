/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.socketio.client

import com.tinder.scarlet.Channel
import com.tinder.scarlet.Message
import com.tinder.scarlet.MessageQueue
import com.tinder.scarlet.Protocol
import com.tinder.scarlet.ProtocolEventAdapter
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject

class SocketIoClient(
    private val url: () -> String,
    private val options: IO.Options = IO.Options()
) : Protocol {

    override fun createChannelFactory(): Channel.Factory {
        return object : Channel.Factory {
            override fun create(
                listener: Channel.Listener,
                parent: Channel?
            ): Channel {
                return SocketIoMainChannel(
                    options,
                    listener
                )
            }
        }
    }

    override fun createOpenRequestFactory(channel: Channel): Protocol.OpenRequest.Factory {
        return object : Protocol.OpenRequest.Factory {
            override fun create(channel: Channel): Protocol.OpenRequest {
                return MainChannelOpenRequest(url())
            }
        }
    }

    override fun createEventAdapterFactory(): ProtocolEventAdapter.Factory {
        return object : ProtocolEventAdapter.Factory {}
    }

    data class MainChannelOpenRequest(val url: String) : Protocol.OpenRequest
}

class SocketIoTopic(
    private val topic: String
) : Protocol {

    override fun createChannelFactory(): Channel.Factory {
        return object : Channel.Factory {
            override fun create(
                listener: Channel.Listener,
                parent: Channel?
            ): Channel {
                require(parent is SocketIoMainChannel)
                return SocketIoMessageChannel(
                    parent as SocketIoMainChannel,
                    topic,
                    listener
                )
            }
        }
    }

    override fun createEventAdapterFactory(): ProtocolEventAdapter.Factory {
        return object : ProtocolEventAdapter.Factory {}
    }

}

internal class SocketIoMainChannel(
    private val options: IO.Options,
    private val listener: Channel.Listener
) : Channel {
    var socket: Socket? = null

    override fun open(openRequest: Protocol.OpenRequest) {
        val mainChannelOpenRequest = openRequest as SocketIoClient.MainChannelOpenRequest
        val socket = IO.socket(mainChannelOpenRequest.url, options)
        socket
            .on(Socket.EVENT_CONNECT) {
                listener.onOpened(this)
            }
            .on(Socket.EVENT_DISCONNECT) {
                listener.onClosed(this)
            }
            .on(Socket.EVENT_ERROR) {
                listener.onFailed(this, null)
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
    private val topic: String,
    private val listener: Channel.Listener
) : Channel, MessageQueue {

    private var socket: Socket? = null
    private var messageQueueListener: MessageQueue.Listener? = null

    override fun open(openRequest: Protocol.OpenRequest) {
        socket = parent.socket
        if (socket == null) {
            listener.onFailed(this, IllegalStateException("main topic is null"))
            return
        }
        socket?.on(topic) {
            val jsonObject = it[0] as JSONObject
            messageQueueListener?.onMessageReceived(this, this,
                Message.Text(jsonObject.toString())
            )
        }
        listener.onOpened(this)
    }

    override fun close(closeRequest: Protocol.CloseRequest) {
        socket?.off(topic)
        socket = null
        listener.onClosed(this)
    }

    override fun forceClose() {
        socket?.off(topic)
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
            is Message.Text -> socket.emit(topic, message.value)
            is Message.Bytes -> socket.emit(topic, message.value)

        }
        return true
    }
}