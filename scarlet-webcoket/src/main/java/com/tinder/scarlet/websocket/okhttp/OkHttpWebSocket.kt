/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.websocket.okhttp

import com.tinder.scarlet.Message
import com.tinder.scarlet.Stream
import com.tinder.scarlet.v2.Channel
import com.tinder.scarlet.v2.EventAdapter
import com.tinder.scarlet.v2.Connection
import com.tinder.scarlet.v2.Protocol
import com.tinder.scarlet.v2.Topic
import com.tinder.scarlet.websocket.ShutdownReason
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

class OkHttpWebSocket(
    private val okHttpClient: OkHttpClient,
    private val requestFactory: RequestFactory
) : Protocol {

    override fun createConnectionFactory(): Connection.Factory {
        return OkHttpWebSocketConnection.Factory(okHttpClient)
    }

    override fun createConnectionRequestFactory(): Connection.RequestFactory {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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

    data class OpenResponse(val okHttpWebSocket: WebSocket, val okHttpResponse: Response) : Channel.OpenResponse

    data class CloseRequest(val shutdownReason: ShutdownReason) : Channel.CloseRequest

    data class CloseResponse(val shutdownReason: ShutdownReason) : Channel.CloseResponse

    interface RequestFactory: Channel.RequestFactory {
        fun createOpenRequest(): OpenRequest
        fun createCloseRequest(): CloseRequest
    }
}

class OkHttpWebSocketConnection(
    val okHttpClient: OkHttpClient,
    private val listener: Connection.Listener
) : Connection {
    override val defaultTopic: Topic = DefaultTopic

    override fun open(openRequest: Connection.OpenRequest) {
    }

    override fun close(closeRequest: Connection.CloseRequest) {
    }

    override fun forceClose() {
    }
    
    class Factory(
        private val okHttpClient: OkHttpClient
    ) : Connection.Factory {
        override fun create(listener: Connection.Listener): Connection {
            return OkHttpWebSocketConnection(okHttpClient, listener)
        }
    }

}

class OkHttpWebSocketChannel(
    private val connection: OkHttpWebSocketConnection,
    private val listener: Channel.Listener
) : Channel {
    override val topic: Topic = DefaultTopic
    lateinit var webSocket: WebSocket

    override fun open(openRequest: Channel.OpenRequest) {
        val openRequest = openRequest as OkHttpWebSocket.OpenRequest
        webSocket = connection.okHttpClient.newWebSocket(openRequest.okHttpRequest, InnerWebSocketListener())
    }

    override fun close(closeRequest: Channel.CloseRequest) {
        val closeRequest = closeRequest as OkHttpWebSocket.CloseRequest
        val (code, reasonText) = closeRequest.shutdownReason
        webSocket.close(code, reasonText)
    }

    override fun forceClose() {
        webSocket.cancel()
    }

    override fun send(message: Message) {
        when (message) {
            is Message.Text -> webSocket.send(message.value)
            is Message.Bytes -> {
                val bytes = message.value
                val byteString = ByteString.of(bytes, 0, bytes.size)
                webSocket.send(byteString)
            }
        }
    }

    inner class InnerWebSocketListener : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) =
            listener.onOpened(this@OkHttpWebSocketChannel, OkHttpWebSocket.OpenResponse(webSocket, response))

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            listener.onMessageReceived(this@OkHttpWebSocketChannel, Message.Bytes(bytes.toByteArray()))
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            listener.onMessageReceived(this@OkHttpWebSocketChannel, Message.Text(text))
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            listener.onClosing(this@OkHttpWebSocketChannel)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            listener.onClosed(
                this@OkHttpWebSocketChannel,
                OkHttpWebSocket.CloseResponse(ShutdownReason(code, reason))
            )
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            listener.onCanceled(this@OkHttpWebSocketChannel, t)
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

object DefaultTopic : Topic

object EmptyRequest : Connection.OpenRequest, Channel.OpenRequest, Channel.CloseRequest

object EmptyResponse : Connection.OpenResponse, Channel.OpenRequest, Channel.CloseRequest
