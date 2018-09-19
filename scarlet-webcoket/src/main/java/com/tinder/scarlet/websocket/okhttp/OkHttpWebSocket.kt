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
import com.tinder.scarlet.websocket.ShutdownReason
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

// TODO seperate Protocol and implementation so that we can share the events and event adapters? between this and mock server
class OkHttpWebSocket(
    private val okHttpClient: OkHttpClient,
    private val requestFactory: RequestFactory
) : Protocol {

    private val factory = OkHttpWebSocketChannel.Factory(okHttpClient)

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
            override fun create(channel: Channel) = requestFactory.createCloseRequest()
        }
    }

    override fun createMessageMetaDataFactory(channel: Channel): Protocol.MessageMetaData.Factory {
        return object : Protocol.MessageMetaData.Factory {}
    }

    override fun createEventAdapterFactory(channel: Channel): Protocol.EventAdapter.Factory {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    data class OpenRequest(val okHttpRequest: Request) : Protocol.OpenRequest

    data class OpenResponse(val okHttpWebSocket: WebSocket, val okHttpResponse: Response) : Protocol.OpenResponse

    data class CloseRequest(val shutdownReason: ShutdownReason) : Protocol.CloseRequest

    data class CloseResponse(val shutdownReason: ShutdownReason) : Protocol.CloseResponse

    interface RequestFactory {
        fun createOpenRequest(): OpenRequest
        fun createCloseRequest(): CloseRequest
    }
}

class OkHttpWebSocketChannel(
    private val okHttpClient: OkHttpClient,
    private val listener: Channel.Listener
) : Channel {
    override val topic: Topic = DefaultTopic
    private var webSocket: WebSocket? = null
    private var messageQueueListener: MessageQueue.Listener? = null

    override fun open(openRequest: Protocol.OpenRequest) {
        val openRequest = openRequest as OkHttpWebSocket.OpenRequest
        webSocket = okHttpClient.newWebSocket(openRequest.okHttpRequest, InnerWebSocketListener())
    }

    override fun close(closeRequest: Protocol.CloseRequest) {
        val closeRequest = closeRequest as OkHttpWebSocket.CloseRequest
        val (code, reasonText) = closeRequest.shutdownReason
        webSocket?.close(code, reasonText)
    }

    override fun forceClose() {
        webSocket?.cancel()
    }

    fun addMessageQueue(messageQueueListener: MessageQueue.Listener): MessageQueue {
        require(this.messageQueueListener == null)
        this.messageQueueListener = messageQueueListener
        return InnerMessageQueue()
    }

    inner class InnerMessageQueue : MessageQueue {
        override fun send(message: Message, messageMetaData: Protocol.MessageMetaData) {
            when (message) {
                is Message.Text -> webSocket?.send(message.value)
                is Message.Bytes -> {
                    val bytes = message.value
                    val byteString = ByteString.of(bytes, 0, bytes.size)
                    webSocket?.send(byteString)
                }
            }
        }
    }

    inner class InnerWebSocketListener : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) =
            listener.onOpened(this@OkHttpWebSocketChannel, OkHttpWebSocket.OpenResponse(webSocket, response))

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            messageQueueListener?.onMessageReceived(this@OkHttpWebSocketChannel, Message.Bytes(bytes.toByteArray()))
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            messageQueueListener?.onMessageReceived(this@OkHttpWebSocketChannel, Message.Text(text))
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

    class Factory(
        private val okHttpClient: OkHttpClient
    ) : Channel.Factory, MessageQueue.Factory {

        override fun create(topic: Topic, listener: Channel.Listener): Channel? {
            if (topic != DefaultTopic) {
                return null
            }
            return OkHttpWebSocketChannel(
                okHttpClient,
                listener
            )
        }

        override fun create(channel: Channel, listener: MessageQueue.Listener): MessageQueue? {
            if (channel !is OkHttpWebSocketChannel) {
                return null
            }
            return channel.addMessageQueue(listener)
        }
    }
}

