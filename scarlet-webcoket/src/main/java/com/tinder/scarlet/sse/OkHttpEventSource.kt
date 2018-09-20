/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.sse

import com.tinder.scarlet.Message
import com.tinder.scarlet.v2.Channel
import com.tinder.scarlet.v2.MessageQueue
import com.tinder.scarlet.v2.Protocol
import com.tinder.scarlet.v2.ProtocolEventAdapter
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
    override fun createChannelFactory(): Channel.Factory {
        return OkHttpEventSourceChannel.Factory(okHttpClient)
    }

    override fun createOpenRequestFactory(channel: Channel): Protocol.OpenRequest.Factory {
        return object : Protocol.OpenRequest.Factory {
            override fun create(channel: Channel) = requestFactory.createOpenRequest()
        }
    }

    override fun createEventAdapterFactory(): ProtocolEventAdapter.Factory {
        return EventSourceEvent.Adapter.Factory()
    }

    data class OpenRequest(val okHttpRequest: Request) : Protocol.OpenRequest

    data class OpenResponse(val eventSource: EventSource, val okHttpResponse: Response) : Protocol.OpenResponse

    data class ReceivedMessageMetaData(val id: String?, val type: String?) : Protocol.MessageMetaData

    interface RequestFactory {
        fun createOpenRequest(): OpenRequest
    }
}

class OkHttpEventSourceChannel(
    private val okHttpClient: OkHttpClient,
    private val listener: Channel.Listener
) : Channel, MessageQueue {
    override val topic: Topic = Topic.Main
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

    override fun createMessageQueue(listener: MessageQueue.Listener): MessageQueue {
        require(this.messageQueueListener == null)
        this.messageQueueListener = listener
        return this
    }

    override fun send(message: Message, messageMetaData: Protocol.MessageMetaData) {
    }

    inner class InnerEventSourceListener : EventSourceListener() {
        override fun onOpen(eventSource: EventSource, response: Response) =
            listener.onOpened(this@OkHttpEventSourceChannel,
                OkHttpEventSource.OpenResponse(eventSource, response)
            )

        override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
            messageQueueListener?.onMessageReceived(
                this@OkHttpEventSourceChannel,
                this@OkHttpEventSourceChannel,
                Message.Text(data),
                OkHttpEventSource.ReceivedMessageMetaData(id, type)
            )
        }

        override fun onClosed(eventSource: EventSource?) {
            listener.onClosed(this@OkHttpEventSourceChannel)
        }

        override fun onFailure(eventSource: EventSource?, t: Throwable?, response: Response?) {
            listener.onFailed(this@OkHttpEventSourceChannel, t)
        }
    }

    class Factory(
        private val okHttpClient: OkHttpClient
    ) : Channel.Factory, MessageQueue.Factory {

        override fun create(topic: Topic, listener: Channel.Listener): Channel? {
            if (topic != Topic.Main) {
                return null
            }
            return OkHttpEventSourceChannel(
                okHttpClient,
                listener
            )
        }
    }
}

