/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.websocket.okhttp

import com.tinder.scarlet.v2.Channel
import com.tinder.scarlet.v2.Protocol
import com.tinder.scarlet.v2.ProtocolEventAdapter
import com.tinder.scarlet.websocket.ShutdownReason
import com.tinder.scarlet.websocket.WebSocketEvent
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class OkHttpWebSocket(
    private val okHttpClient: OkHttpClient,
    private val requestFactory: RequestFactory
) : Protocol {

    override fun createChannelFactory(): Channel.Factory {
        return OkHttpWebSocketChannel.Factory(
            object : WebSocketFactory {
                override fun createWebSocket(request: Request, listener: WebSocketListener) {
                    okHttpClient.newWebSocket(request, listener)
                }
            }
        )
    }

    override fun createOpenRequestFactory(channel: Channel): Protocol.OpenRequest.Factory {
        return object : Protocol.OpenRequest.Factory {
            override fun create(channel: Channel) = requestFactory.createOpenRequest()
        }
    }

    override fun createCloseRequestFactory(channel: Channel): Protocol.CloseRequest.Factory {
        return object : Protocol.CloseRequest.Factory {
            override fun create(channel: Channel) = requestFactory.createCloseRequest()
        }
    }

    override fun createEventAdapterFactory(): ProtocolEventAdapter.Factory {
        return WebSocketEvent.Adapter.Factory()
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
