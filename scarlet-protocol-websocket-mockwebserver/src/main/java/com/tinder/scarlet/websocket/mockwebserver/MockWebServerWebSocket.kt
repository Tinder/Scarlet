/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.websocket.mockwebserver

import com.tinder.scarlet.Channel
import com.tinder.scarlet.Protocol
import com.tinder.scarlet.ProtocolEventAdapter
import com.tinder.scarlet.websocket.WebSocketEvent
import com.tinder.scarlet.websocket.okhttp.OkHttpWebSocket
import com.tinder.scarlet.websocket.okhttp.OkHttpWebSocketChannel
import com.tinder.scarlet.websocket.okhttp.WebSocketFactory
import okhttp3.Request
import okhttp3.WebSocketListener
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer

class MockWebServerWebSocket(
    private val mockWebServer: MockWebServer,
    private val requestFactory: RequestFactory
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
                return OkHttpWebSocket.OpenRequest(Request.Builder().url("http://localhost.com").build())
            }
        }
    }

    override fun createCloseRequestFactory(channel: Channel): Protocol.CloseRequest.Factory {
        return object : Protocol.CloseRequest.Factory {
            override fun create(channel: Channel): Protocol.CloseRequest {
                return requestFactory.createCloseRequest()
            }
        }
    }

    override fun createEventAdapterFactory(): ProtocolEventAdapter.Factory {
        return WebSocketEvent.Adapter.Factory()
    }

    interface RequestFactory {
        fun createCloseRequest(): OkHttpWebSocket.CloseRequest
    }

    open class SimpleRequestFactory(
        private val createCloseRequestCallable: () -> OkHttpWebSocket.CloseRequest
    ) : RequestFactory{
        override fun createCloseRequest(): OkHttpWebSocket.CloseRequest {
            return createCloseRequestCallable()
        }
    }
}
