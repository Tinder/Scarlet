/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.websocket.mockwebserver

import com.tinder.scarlet.v2.Channel
import com.tinder.scarlet.v2.Protocol
import com.tinder.scarlet.v2.ProtocolEventAdapter
import com.tinder.scarlet.websocket.ShutdownReason
import com.tinder.scarlet.websocket.WebSocketEvent
import com.tinder.scarlet.websocket.okhttp.OkHttpWebSocket
import com.tinder.scarlet.websocket.okhttp.OkHttpWebSocketChannel
import com.tinder.scarlet.websocket.okhttp.WebSocketFactory
import okhttp3.Request
import okhttp3.WebSocketListener
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer

class MockWebServerWebSocket(
    private val mockWebServer: MockWebServer
) : Protocol {
    override fun createChannelFactory(): Channel.Factory {
        return OkHttpWebSocketChannel.Factory(
            object : WebSocketFactory {
                override fun createWebSocket(request: Request, listener: WebSocketListener) {
                    mockWebServer.enqueue(MockResponse().withWebSocketUpgrade(listener))
                }
            }
        )
    }

    override fun createOpenRequestFactory(channel: Channel): Protocol.OpenRequest.Factory {
        return object : Protocol.OpenRequest.Factory {
            override fun create(channel: Channel): Protocol.OpenRequest {
                return OkHttpWebSocket.OpenRequest(Request.Builder().url("localhost.com").build())
            }
        }
    }

    override fun createCloseRequestFactory(channel: Channel): Protocol.CloseRequest.Factory {
        return object : Protocol.CloseRequest.Factory {
            override fun create(channel: Channel): Protocol.CloseRequest {
                return OkHttpWebSocket.CloseRequest(ShutdownReason.GRACEFUL)
            }
        }
    }

    override fun createEventAdapterFactory(): ProtocolEventAdapter.Factory {
        return WebSocketEvent.Adapter.Factory()
    }
}
