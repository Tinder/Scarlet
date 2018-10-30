/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.stomp

import com.tinder.scarlet.Channel
import com.tinder.scarlet.Message
import com.tinder.scarlet.MessageQueue
import com.tinder.scarlet.Protocol
import com.tinder.scarlet.ProtocolEventAdapter
import net.ser1.stomp.Client
import javax.security.auth.login.LoginException

class GozirraStompClient(
    private val openRequestFactory: RequestFactory
) : Protocol {

    override fun createChannelFactory(): Channel.Factory {
        return object : Channel.Factory {
            override fun create(
                listener: Channel.Listener,
                parent: Channel?
            ): Channel {
                return StompMainChannel(listener)
            }
        }
    }

    override fun createOpenRequestFactory(channel: Channel): Protocol.OpenRequest.Factory {
        return object : Protocol.OpenRequest.Factory {
            override fun create(channel: Channel): Protocol.OpenRequest {
                return openRequestFactory.createClientOpenRequest()
            }
        }
    }

    override fun createEventAdapterFactory(): ProtocolEventAdapter.Factory {
        return object : ProtocolEventAdapter.Factory {}
    }

    interface RequestFactory {
        fun createClientOpenRequest(): ClientOpenRequest
    }


    open class SimpleRequestFactory(
        private val createClientOpenRequestCallable: () -> ClientOpenRequest
    ) : RequestFactory {
        override fun createClientOpenRequest(): ClientOpenRequest {
            return createClientOpenRequestCallable()
        }
    }

    data class ClientOpenRequest(
        val url: String,
        val port: Int,
        val login: String,
        val password: String
    ) : Protocol.OpenRequest

    data class MessageMetaData(val headers: Map<String, String>) : Protocol.MessageMetaData
}

class GozirraStompDestination(
    val destination: String,
    private val openRequestFactory: RequestFactory
) : Protocol {

    override fun createChannelFactory(): Channel.Factory {
        return object : Channel.Factory {
            override fun create(
                listener: Channel.Listener,
                parent: Channel?
            ): Channel {
                require(parent is StompMainChannel)
                return StompMessageChannel(parent as StompMainChannel, destination, listener)
            }
        }
    }

    override fun createOpenRequestFactory(channel: Channel): Protocol.OpenRequest.Factory {
        return object : Protocol.OpenRequest.Factory {
            override fun create(channel: Channel): Protocol.OpenRequest {
                return DestinationOpenRequest(
                    openRequestFactory.createDestinationOpenRequestHeader(destination)
                )
            }
        }
    }

    override fun createEventAdapterFactory(): ProtocolEventAdapter.Factory {
        return object : ProtocolEventAdapter.Factory {}
    }

    interface RequestFactory {
        fun createDestinationOpenRequestHeader(destination: String): Map<String, String>
    }


    open class SimpleRequestFactory(
        private val createDestinationOpenRequestHeaderCallable: (String) -> Map<String, String>
    ) : RequestFactory {

        override fun createDestinationOpenRequestHeader(destination: String): Map<String, String> {
            return createDestinationOpenRequestHeaderCallable(destination)
        }
    }

    data class DestinationOpenRequest(
        val headers: Map<String, String>
    ) : Protocol.OpenRequest

}

class StompMainChannel(
    private val listener: Channel.Listener
) : Channel {
    var client: Client? = null

    override fun open(openRequest: Protocol.OpenRequest) {
        val (url, port, login, password) = openRequest as GozirraStompClient.ClientOpenRequest
        try {
            val client = Client(url, port, login, password)
            client.addErrorListener { _, _ ->
                listener.onFailed(this, true, null)
            }
            this.client = client
            listener.onOpened(this)
        } catch (e: LoginException) {
            listener.onFailed(this, true, e)
        } catch (e: Throwable) {
            listener.onFailed(this, true, e)
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
            listener.onFailed(this, true, e)
        } catch (e: Throwable) {
            listener.onFailed(this, true, e)
        }
    }

    override fun createMessageQueue(listener: MessageQueue.Listener): MessageQueue? {
        return null
    }
}

class StompMessageChannel(
    val mainChannel : StompMainChannel,
    val destination: String,
    private val listener: Channel.Listener
) : Channel, MessageQueue {

    private var client: Client? = null
    private var messageQueueListener: MessageQueue.Listener? = null

    override fun open(openRequest: Protocol.OpenRequest) {
        val destinationOpenRequest = openRequest as GozirraStompDestination.DestinationOpenRequest
        client = mainChannel.client
        client?.subscribe(
            destination,
            { headers, message ->
                messageQueueListener?.onMessageReceived(
                    this,
                    this,
                    Message.Text(message),
                    GozirraStompClient.MessageMetaData(headers as Map<String, String>)
                )
            },
            // This map is modified by gozirra internally
            destinationOpenRequest.headers.toMutableMap()
        )
        listener.onOpened(this)
    }

    override fun close(closeRequest: Protocol.CloseRequest) {
        forceClose()
    }

    override fun forceClose() {
        client?.unsubscribe(destination)
        client = null
        listener.onClosed(this)
    }

    override fun createMessageQueue(listener: MessageQueue.Listener): MessageQueue {
        require(messageQueueListener == null)
        messageQueueListener = listener
        return this
    }

    override fun send(message: Message, messageMetaData: Protocol.MessageMetaData): Boolean {
        val client = client ?: return false
        when (message) {
            is Message.Text -> client.send(destination, message.value)
            is Message.Bytes -> throw IllegalArgumentException("Bytes are not supported")
        }
        return true
    }
}
