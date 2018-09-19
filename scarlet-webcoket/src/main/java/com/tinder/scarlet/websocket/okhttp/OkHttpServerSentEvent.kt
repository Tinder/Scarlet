/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.websocket.okhttp

import com.tinder.scarlet.Message
import com.tinder.scarlet.v2.Channel
import com.tinder.scarlet.v2.DefaultTopic
import com.tinder.scarlet.v2.MessageQueue
import com.tinder.scarlet.v2.Protocol
import com.tinder.scarlet.v2.Topic
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources

class OkHttpEventSource(
    private val okHttpClient: OkHttpClient,
    private val requestFactory: RequestFactory
) : Protocol {
    private val factory = OkHttpEventSourceChannel.Factory(okHttpClient)

    override fun createChannelFactory(): Channel.Factory {
        return factory
    }

    override fun createMessageQueueFactory(): MessageQueue.Factory {
        return factory
    }

    override fun createChannelOpenRequestFactory(channel: Channel): Protocol.OpenRequest.Factory {
        return object : Protocol.OpenRequest.Factory {
            override fun create(channel: Channel) = requestFactory.createOpenRequest()
        }
    }

    override fun createChannelCloseRequestFactory(channel: Channel): Protocol.CloseRequest.Factory {
        return object : Protocol.CloseRequest.Factory {
            override fun create(channel: Channel) = Empty
        }
    }

    override fun createMessageMetaDataFactory(channel: Channel): Protocol.MessageMetaData.Factory {
        return object : Protocol.MessageMetaData.Factory {}
    }

    override fun createEventAdapterFactory(channel: Channel): Protocol.EventAdapter.Factory {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


    data class OpenRequest(val okHttpRequest: Request) : Protocol.OpenRequest

    data class OpenResponse(val eventSource: EventSource, val okHttpResponse: Response) : Protocol.OpenResponse

    object Empty : Protocol.CloseRequest, Protocol.CloseResponse

    data class MessageMetaData(val id: String?, val type: String?) : Protocol.MessageMetaData

    interface RequestFactory {
        fun createOpenRequest(): OpenRequest
    }
}

class OkHttpEventSourceChannel(
    private val okHttpClient: OkHttpClient,
    private val listener: Channel.Listener
) : Channel {
    override val topic: Topic = DefaultTopic
    private var eventSource: EventSource? = null
    private var messageQueueListener: MessageQueue.Listener? = null

    override fun open(openRequest: Protocol.OpenRequest) {
        val openRequest = openRequest as OkHttpEventSource.OpenRequest
        eventSource = EventSources.createFactory(okHttpClient)
            .newEventSource(openRequest.okHttpRequest, InnerEventSourceListener())
    }

    override fun close(closeRequest: Protocol.CloseRequest) {
        eventSource?.cancel()
    }

    override fun forceClose() {
        eventSource?.cancel()
    }

    fun addMessageQueue(messageQueueListener: MessageQueue.Listener): MessageQueue {
        require(this.messageQueueListener == null)
        this.messageQueueListener = messageQueueListener
        return InnerMessageQueue()
    }

    inner class InnerMessageQueue : MessageQueue {
        override fun send(message: Message, messageMetaData: Protocol.MessageMetaData) {
        }
    }

    inner class InnerEventSourceListener : EventSourceListener() {
        override fun onOpen(eventSource: EventSource, response: Response) =
            listener.onOpened(this@OkHttpEventSourceChannel, OkHttpEventSource.OpenResponse(eventSource, response))

        override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
            messageQueueListener?.onMessageReceived(
                this@OkHttpEventSourceChannel,
                Message.Text(data),
                OkHttpEventSource.MessageMetaData(id, type)
            )
        }

        override fun onClosed(eventSource: EventSource?) {
            listener.onClosed(
                this@OkHttpEventSourceChannel, OkHttpEventSource.Empty
            )
        }

        override fun onFailure(eventSource: EventSource?, t: Throwable?, response: Response?) {
            listener.onCanceled(this@OkHttpEventSourceChannel, t)
        }
    }

    class Factory(
        private val okHttpClient: OkHttpClient
    ) : Channel.Factory, MessageQueue.Factory {

        override fun create(topic: Topic, listener: Channel.Listener): Channel? {
            if (topic != DefaultTopic) {
                return null
            }
            return OkHttpEventSourceChannel(
                okHttpClient,
                listener
            )
        }

        override fun create(channel: Channel, listener: MessageQueue.Listener): MessageQueue? {
            if (channel !is OkHttpEventSourceChannel) {
                return null
            }
            return channel.addMessageQueue(listener)
        }
    }
}

