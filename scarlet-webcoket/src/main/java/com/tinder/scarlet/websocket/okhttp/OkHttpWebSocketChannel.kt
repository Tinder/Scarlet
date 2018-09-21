/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.websocket.okhttp

import com.tinder.scarlet.Message
import com.tinder.scarlet.v2.Channel
import com.tinder.scarlet.v2.MessageQueue
import com.tinder.scarlet.v2.Protocol
import com.tinder.scarlet.v2.Topic
import com.tinder.scarlet.websocket.ShutdownReason
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

class OkHttpWebSocketChannel(
    private val webSocketFactory: WebSocketFactory,
    private val listener: Channel.Listener
) : Channel, MessageQueue {
    override val topic: Topic = Topic.Main
    private var webSocket: WebSocket? = null
    private var messageQueueListener: MessageQueue.Listener? = null

    override fun open(openRequest: Protocol.OpenRequest) {
        val openRequest = openRequest as OkHttpWebSocket.OpenRequest
        webSocketFactory.createWebSocket(openRequest.okHttpRequest, InnerWebSocketListener())
    }

    override fun close(closeRequest: Protocol.CloseRequest) {
        val closeRequest = closeRequest as OkHttpWebSocket.CloseRequest
        val (code, reasonText) = closeRequest.shutdownReason
        webSocket?.close(code, reasonText)
        webSocket = null
    }

    override fun forceClose() {
        webSocket?.cancel()
        webSocket = null
    }

    override fun createMessageQueue(listener: MessageQueue.Listener): MessageQueue {
        require(this.messageQueueListener == null)
        this.messageQueueListener = listener
        return this
    }

    override fun send(message: Message, messageMetaData: Protocol.MessageMetaData): Boolean {
        val webSocket = webSocket ?: return false
        when (message) {
            is Message.Text -> webSocket.send(message.value)
            is Message.Bytes -> {
                val bytes = message.value
                val byteString = ByteString.of(bytes, 0, bytes.size)
                webSocket.send(byteString)
            }
        }
        return true
    }

    inner class InnerWebSocketListener : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            this@OkHttpWebSocketChannel.webSocket = webSocket
            listener.onOpened(
                this@OkHttpWebSocketChannel,
                OkHttpWebSocket.OpenResponse(webSocket, response)
            )
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            messageQueueListener?.onMessageReceived(
                this@OkHttpWebSocketChannel,
                this@OkHttpWebSocketChannel,
                Message.Bytes(bytes.toByteArray())
            )
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            messageQueueListener?.onMessageReceived(
                this@OkHttpWebSocketChannel,
                this@OkHttpWebSocketChannel,
                Message.Text(text)
            )
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            listener.onClosing(
                this@OkHttpWebSocketChannel,
                OkHttpWebSocket.CloseResponse(
                    ShutdownReason(
                        code,
                        reason
                    )
                )
            )
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            listener.onClosed(
                this@OkHttpWebSocketChannel,
                OkHttpWebSocket.CloseResponse(
                    ShutdownReason(
                        code,
                        reason
                    )
                )
            )
            this@OkHttpWebSocketChannel.webSocket = null
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            listener.onFailed(this@OkHttpWebSocketChannel, t)
            this@OkHttpWebSocketChannel.webSocket = null
        }
    }

    class Factory(
        private val webSocketFactory: WebSocketFactory
    ) : Channel.Factory {

        override fun create(topic: Topic, listener: Channel.Listener): Channel? {
            if (topic != Topic.Main) {
                return null
            }
            return OkHttpWebSocketChannel(
                webSocketFactory,
                listener
            )
        }
    }
}
