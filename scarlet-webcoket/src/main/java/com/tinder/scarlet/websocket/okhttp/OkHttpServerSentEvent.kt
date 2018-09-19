/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.websocket.okhttp

import com.tinder.scarlet.Message
import com.tinder.scarlet.v2.Channel
import com.tinder.scarlet.v2.Connection
import com.tinder.scarlet.v2.EventAdapter
import com.tinder.scarlet.v2.Protocol
import com.tinder.scarlet.v2.Topic
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import okio.ByteString

class OkHttpEventSource(
    private val okHttpClient: OkHttpClient,
    private val requestFactory: RequestFactory
) : Protocol {

    override fun createConnectionFactory(): Connection.Factory {
        return OkHttpWebSocketConnection.Factory(okHttpClient)
    }

    override fun createConnectionRequestFactory(): Connection.RequestFactory {
        return object : Connection.RequestFactory {
            override fun createOpenRequest(connection: Connection): Connection.OpenRequest {
                return EmptyRequest
            }

            override fun createCloseRequest(connection: Connection): Connection.CloseRequest {
                return EmptyRequest
            }
        }
    }

    override fun createChannelFactory(): Channel.Factory {
        return OkHttpWebSocketChannel.Factory()
    }

    override fun createChannelRequestFactory(): Channel.RequestFactory {
        return requestFactory
    }

    override fun createEventAdapterFactory(): EventAdapter.Factory {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    data class OpenRequest(val okHttpRequest: Request) : Channel.OpenRequest

    data class OpenResponse(val okHttpResponse: Response) : Channel.OpenResponse

    data class CloseRequest(val shutdownReason: OkHttpEventSource.ShutdownReason) : Channel.CloseRequest

    data class CloseResponse(val shutdownReason: OkHttpEventSource.ShutdownReason) : Channel.CloseResponse

    object DefaultTopic : Topic

    object EmptyRequest : Connection.OpenRequest, Connection.CloseRequest

    object EmptyResponse : Connection.OpenResponse, Connection.CloseResponse

    interface RequestFactory : Channel.RequestFactory {
        fun createOpenRequest(): OpenRequest
        fun createCloseRequest(): CloseRequest
    }
}

class OkHttpEventSourceConnection(
    val okHttpClient: OkHttpClient,
    private val listener: Connection.Listener
) : Connection {
    override val defaultTopic: Topic = OkHttpEventSource.DefaultTopic

    override fun open(openRequest: Connection.OpenRequest) {
        listener.onOpened(this, OkHttpEventSource.EmptyResponse)
    }

    override fun close(closeRequest: Connection.CloseRequest) {
        listener.onClosed(this, OkHttpEventSource.EmptyResponse)
    }

    override fun forceClose() {
        listener.onClosed(this, OkHttpEventSource.EmptyResponse)
    }

    class Factory(
        private val okHttpClient: OkHttpClient
    ) : Connection.Factory {
        override fun create(listener: Connection.Listener): Connection {
            return OkHttpWebSocketConnection(okHttpClient, listener)
        }
    }

}

class OkHttpEventSourceChannel(
    private val connection: OkHttpWebSocketConnection,
    private val listener: Channel.Listener
) : Channel {
    override val topic: Topic = DefaultTopic
    lateinit var eventSource: EventSource

    override fun open(openRequest: Channel.OpenRequest) {
        val openRequest = openRequest as OkHttpWebSocket.OpenRequest
        eventSource = EventSources.createFactory(connection.okHttpClient).newEventSource(openRequest.okHttpRequest, InnerEventSourceListener())
    }

    override fun close(closeRequest: Channel.CloseRequest) {
        eventSource.cancel()
    }

    override fun forceClose() {
        eventSource.cancel()
    }

    override fun send(message: Message) {
    }

    inner class InnerEventSourceListener : EventSourceListener() {
        override fun onOpen(webSocket: EventSource, response: Response) =
            listener.onOpened(this@OkHttpEventSourceChannel, OkHttpWebSocket.OpenResponse(webSocket, response))

        override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
            super.onEvent(eventSource, id, type, Message.Text(data))
        }

        override fun onClosed(webSocket: EventSource?) {
            listener.onClosed(
                this@OkHttpEventSourceChannel,
                )
            )
        }

        override fun onFailure(webSocket: EventSource?, t: Throwable?, response: Response?) {
            listener.onCanceled(this@OkHttpEventSourceChannel, t)
        }
    }

    class Factory : Channel.Factory {

        override fun create(configuration: Channel.Configuration): Channel {
            return OkHttpWebSocketChannel(
                configuration.connection as OkHttpWebSocketConnection,
                configuration.listener
            )
        }
    }
}

