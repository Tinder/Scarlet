/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.stomp

import com.tinder.scarlet.Message
import com.tinder.scarlet.v2.Channel
import com.tinder.scarlet.v2.MessageQueue
import com.tinder.scarlet.v2.Protocol
import com.tinder.scarlet.v2.ProtocolEvent
import com.tinder.scarlet.v2.Topic
import net.ser1.stomp.Client
import javax.security.auth.login.LoginException

class StompProtocol(
    private val openRequestFactory: StompProtocol.RequestFactory
) : Protocol {
    private var mainChannel: StompMainChannel? = null

    override fun createChannelFactory(): Channel.Factory {
        return object : Channel.Factory {
            override fun create(topic: Topic, listener: Channel.Listener): Channel {
                if (topic == Topic.Main) {
                    mainChannel = StompMainChannel(listener)
                    return mainChannel!!
                }
                return StompMessageChannel(topic, listener)
            }
        }
    }

    override fun createOpenRequestFactory(channel: Channel): Protocol.OpenRequest.Factory {
        return object : Protocol.OpenRequest.Factory {
            override fun create(channel: Channel): Protocol.OpenRequest {
                if (channel.topic == Topic.Main) {
                    return openRequestFactory.createClientOpenRequest()
                }
                return StompProtocol.DestinationOpenRequest(
                    requireNotNull(mainChannel?.client),
                    openRequestFactory.createDestinationOpenRequestHeader(channel.topic.id)
                )
            }
        }
    }

    override fun createEventAdapterFactory(channel: Channel): ProtocolEvent.Adapter.Factory {
        return object : ProtocolEvent.Adapter.Factory {}
    }

    interface RequestFactory {
        fun createClientOpenRequest(): ClientOpenRequest
        fun createDestinationOpenRequestHeader(destination: String): Map<String, String>
    }

    data class ClientOpenRequest(
        val url: String,
        val port: Int,
        val login: String,
        val password: String
    ) : Protocol.OpenRequest

    data class DestinationOpenRequest(
        val client: Client,
        val headers: Map<String, String>
    ) : Protocol.OpenRequest

    data class MessageMetaData(val headers: Map<String, String>) : Protocol.MessageMetaData
}

class StompMainChannel(
    private val listener: Channel.Listener
) : Channel {
    var client: Client? = null

    override fun open(openRequest: Protocol.OpenRequest) {
        val (url, port, login, password) = openRequest as StompProtocol.ClientOpenRequest
        try {
            val client = Client(url, port, login, password)
            client.addErrorListener { _, _ ->
                listener.onFailed(this, null)
            }
            this.client = client
            listener.onOpened(this)
        } catch (e: LoginException) {
            listener.onFailed(this, e)
        } catch (e: Throwable) {
            listener.onFailed(this, e)
        }
    }

    override fun close(closeRequest: Protocol.CloseRequest) {
        forceClose()
    }

    override fun forceClose() {
        try {
            client?.disconnect()
            client = null
            listener.onClosed(this)
        } catch (e: LoginException) {
            listener.onFailed(this, e)
        } catch (e: Throwable) {
            listener.onFailed(this, e)
        }
    }

    override fun createMessageQueue(listener: MessageQueue.Listener): MessageQueue? {
        return null
    }
}

class StompMessageChannel(
    override val topic: Topic,
    private val listener: Channel.Listener
) : Channel, MessageQueue {

    private var client: Client? = null
    private var messageQueueListener: MessageQueue.Listener? = null

    override fun open(openRequest: Protocol.OpenRequest) {
        val openRequest = openRequest as StompProtocol.DestinationOpenRequest
        client = openRequest.client
        client?.subscribe(
            topic.id,
            { headers, message ->
                messageQueueListener?.onMessageReceived(
                    this,
                    this,
                    Message.Text(message),
                    StompProtocol.MessageMetaData(headers as Map<String, String>)
                )
            }, openRequest.headers
        )
        listener.onOpened(this)
    }

    override fun close(closeRequest: Protocol.CloseRequest) {
        forceClose()
    }

    override fun forceClose() {
        client?.unsubscribe(topic.id)
        client = null
        listener.onClosed(this)
    }

    override fun createMessageQueue(listener: MessageQueue.Listener): MessageQueue {
        require(messageQueueListener == null)
        messageQueueListener = listener
        return this
    }

    override fun send(message: Message, messageMetaData: Protocol.MessageMetaData) {
        when (message) {
            is Message.Text -> client?.send(topic.id, message.value)
            is Message.Bytes -> throw IllegalArgumentException("Bytes are not supported")
        }
    }
}

