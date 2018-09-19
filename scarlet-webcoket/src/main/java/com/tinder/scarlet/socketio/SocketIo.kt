/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.socketio

import com.tinder.scarlet.Message
import com.tinder.scarlet.v2.Channel
import com.tinder.scarlet.v2.DefaultTopic
import com.tinder.scarlet.v2.MessageQueue
import com.tinder.scarlet.v2.Protocol
import com.tinder.scarlet.v2.Topic
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject

class SocketIo(
    val options: IO.Options
) : Protocol {
    override fun createChannelFactory(): Channel.Factory {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createMessageQueueFactory(): MessageQueue.Factory {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createChannelOpenRequestFactory(channel: Channel): Protocol.OpenRequest.Factory {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createChannelCloseRequestFactory(channel: Channel): Protocol.CloseRequest.Factory {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createMessageMetaDataFactory(channel: Channel): Protocol.MessageMetaData.Factory {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createEventAdapterFactory(channel: Channel): Protocol.EventAdapter.Factory {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    data class OpenRequest(val url: String) : Protocol.OpenRequest

    object Empty : Protocol.OpenResponse, Protocol.CloseRequest, Protocol.CloseResponse

}

class SocketIoChannel(
    private val options: IO.Options,
    private val listener: Channel.Listener
) : Channel {
    override val topic: Topic = DefaultTopic
    private var socket: Socket? = null

    override fun open(openRequest: Protocol.OpenRequest) {
        val openRequest = openRequest as SocketIo.OpenRequest
        val socket = IO.socket(openRequest.url, options)
        socket
            .on(Socket.EVENT_CONNECT) {
                listener.onOpened(this, SocketIo.Empty)
            }
            .on(Socket.EVENT_DISCONNECT) {
                listener.onClosed(this, SocketIo.Empty)
            }
            .on(Socket.EVENT_ERROR) {
                listener.onCanceled(this, null)
            }

        socket.open()
        this.socket = socket
    }

    override fun close(closeRequest: Protocol.CloseRequest) {
        socket?.disconnect()
    }

    override fun forceClose() {
        socket?.disconnect()
    }

    fun addMessageQueue(topic: Topic, messageQueueListener: MessageQueue.Listener): MessageQueue {
        return InnerMessageQueue(topic, messageQueueListener)
    }

    inner class InnerMessageQueue(
        override val topic: Topic,
        private var messageQueueListener: MessageQueue.Listener
    ) : Channel, MessageQueue {

        override fun open(openRequest: Protocol.OpenRequest) {
            socket?.on(topic.id) {
                val jsonObject = it[0] as JSONObject
                messageQueueListener.onMessageReceived(this, Message.Text(jsonObject.toString()))
            }
        }

        override fun close(closeRequest: Protocol.CloseRequest) {
            socket?.off(topic.id)
        }

        override fun forceClose() {
            socket?.off(topic.id)
        }

        override fun send(message: Message, messageMetaData: Protocol.MessageMetaData) {
            when (message) {
                is Message.Text -> socket?.send(topic.id, message.value)
                is Message.Bytes -> {
                    socket?.send(topic.id, message.value)
                }
            }
        }
    }

    class Factory(
        private val options: IO.Options
    ) : Channel.Factory, MessageQueue.Factory {

        override fun create(topic: Topic, listener: Channel.Listener): Channel? {
            if (topic == DefaultTopic) {
                return SocketIoChannel(
                    options,
                    listener
                )
            }
            // sub channel
            return SocketIoChannel,
        }

        override fun create(channel: Channel, listener: MessageQueue.Listener): MessageQueue? {
            if (channel !is SocketIoChannel) {
                return null
            }
            return channel.addMessageQueue(channel.topic, listener)
        }
    }
}
