/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.sse

import com.tinder.scarlet.Channel
import com.tinder.scarlet.Message
import com.tinder.scarlet.MessageQueue
import com.tinder.scarlet.Protocol
import com.tinder.scarlet.ProtocolSpecificEventAdapter
import com.tinder.scarlet.utils.SimpleProtocolOpenRequestFactory
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
        return SimpleProtocolOpenRequestFactory {
            requestFactory.createOpenRequest()
        }
    }

    override fun createEventAdapterFactory(): ProtocolSpecificEventAdapter.Factory {
        return EventSourceEvent.Adapter.Factory()
    }

    data class OpenRequest(val okHttpRequest: Request) : Protocol.OpenRequest

    data class OpenResponse(val eventSource: EventSource, val okHttpResponse: Response) :
        Protocol.OpenResponse

    data class ReceivedMessageMetaData(val id: String?, val type: String?) :
        Protocol.MessageMetaData

    interface RequestFactory {
        fun createOpenRequest(): OpenRequest
    }

    open class SimpleRequestFactory(
        private val createOpenRequestCallable: () -> Request
    ) : RequestFactory {
        override fun createOpenRequest(): OpenRequest {
            return OpenRequest(createOpenRequestCallable())
        }
    }
}

class OkHttpEventSourceChannel(
    private val okHttpClient: OkHttpClient,
    private val listener: Channel.Listener
) : Channel, MessageQueue {
    private var eventSource: EventSource? = null
    private var messageQueueListener: MessageQueue.Listener? = null

    override fun open(openRequest: Protocol.OpenRequest) {
        val okHttpEventSourceOpenRequest = openRequest as OkHttpEventSource.OpenRequest
        eventSource = EventSources.createFactory(okHttpClient)
            .newEventSource(okHttpEventSourceOpenRequest.okHttpRequest, InnerEventSourceListener())
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

    override fun send(message: Message, messageMetaData: Protocol.MessageMetaData): Boolean {
        return false
    }

    inner class InnerEventSourceListener : EventSourceListener() {
        override fun onOpen(eventSource: EventSource, response: Response) =
            listener.onOpened(
                this@OkHttpEventSourceChannel,
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

        override fun onClosed(eventSource: EventSource) {
            listener.onClosed(this@OkHttpEventSourceChannel)
        }

        override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
            listener.onFailed(this@OkHttpEventSourceChannel, true, t)
        }
    }

    class Factory(
        private val okHttpClient: OkHttpClient
    ) : Channel.Factory, MessageQueue.Factory {

        override fun create(
            listener: Channel.Listener,
            parent: Channel?
        ): Channel? {
            return OkHttpEventSourceChannel(
                okHttpClient,
                listener
            )
        }
    }
}
