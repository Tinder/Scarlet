/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.socketio.server

import com.corundumstudio.socketio.Configuration
import com.corundumstudio.socketio.SocketIOServer
import com.tinder.scarlet.Channel
import com.tinder.scarlet.Message
import com.tinder.scarlet.MessageQueue
import com.tinder.scarlet.Protocol
import com.tinder.scarlet.ProtocolEventAdapter

class MockSocketIoServer(
    private val configuration: Configuration = Configuration()
) : Protocol {

    override fun createChannelFactory(): Channel.Factory {
        return object : Channel.Factory {
            override fun create(
                listener: Channel.Listener,
                parent: Channel?
            ): Channel {
                return SocketIoMainChannel(
                    configuration,
                    listener
                )
            }
        }
    }

    override fun createEventAdapterFactory(): ProtocolEventAdapter.Factory {
        return object : ProtocolEventAdapter.Factory {}
    }
}

class SocketIoEventName(
    private val eventName: String
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
                    eventName,
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
    val configuration: Configuration,
    private val listener: Channel.Listener
) : Channel {

    var socketIoServer: SocketIOServer? = null

    override fun open(openRequest: Protocol.OpenRequest) {
        socketIoServer = SocketIOServer(configuration)
        socketIoServer?.startAsync()?.addListener {
            if (it.isSuccess) {
                listener.onOpened(this@SocketIoMainChannel)
            } else {
                listener.onFailed(
                    this@SocketIoMainChannel,
                    shouldRetry = true,
                    throwable = it.cause()
                )
            }
        }
    }

    override fun close(closeRequest: Protocol.CloseRequest) {
        forceClose()
    }

    override fun forceClose() {
        socketIoServer?.stop()
        listener.onClosed(this)
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

    private var server: SocketIOServer? = null
    private var messageQueueListener: MessageQueue.Listener? = null

    override fun open(openRequest: Protocol.OpenRequest) {
        server = parent.socketIoServer
        if (server == null) {
            listener.onFailed(this, true, IllegalStateException("main eventName is null"))
            return
        }
        server?.removeAllListeners(eventName)
        server?.addEventListener(
            eventName,
            Any::class.java
        ) { client, data, ackRequest ->
            when (data) {
                is String -> messageQueueListener?.onMessageReceived(this, this, Message.Text(data))
                is ByteArray -> messageQueueListener?.onMessageReceived(
                    this,
                    this,
                    Message.Bytes(data)
                )
            }
        }
        listener.onOpened(this)
    }

    override fun close(closeRequest: Protocol.CloseRequest) {
        forceClose()
    }

    override fun forceClose() {
        server?.removeAllListeners(eventName)
        server = null
        listener.onClosed(this)
    }

    override fun createMessageQueue(listener: MessageQueue.Listener): MessageQueue {
        require(messageQueueListener == null)
        messageQueueListener = listener
        return this
    }

    override fun send(message: Message, messageMetaData: Protocol.MessageMetaData): Boolean {
        val server = server ?: return false
        when (message) {
            is Message.Text -> server.broadcastOperations.sendEvent(eventName, message.value)
            is Message.Bytes -> server.broadcastOperations.sendEvent(eventName, message.value)
        }
        return true
    }
}