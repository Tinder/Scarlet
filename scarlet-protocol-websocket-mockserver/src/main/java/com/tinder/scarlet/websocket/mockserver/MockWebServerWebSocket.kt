/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.websocket.mockserver

import com.tinder.scarlet.Channel
import com.tinder.scarlet.Protocol
import com.tinder.scarlet.ProtocolSpecificEventAdapter
import com.tinder.scarlet.utils.SimpleProtocolCloseRequestFactory
import com.tinder.scarlet.utils.SimpleProtocolOpenRequestFactory
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
        return SimpleProtocolOpenRequestFactory {
            OkHttpWebSocket.OpenRequest(Request.Builder().url("http://localhost.com").build())
        }
    }

    override fun createCloseRequestFactory(channel: Channel): Protocol.CloseRequest.Factory {
        return SimpleProtocolCloseRequestFactory {
            requestFactory.createCloseRequest()
        }
    }

    override fun createEventAdapterFactory(): ProtocolSpecificEventAdapter.Factory {
        return WebSocketEvent.Adapter.Factory()
    }

    interface RequestFactory {
        fun createCloseRequest(): OkHttpWebSocket.CloseRequest
    }

    open class SimpleRequestFactory(
        private val createCloseRequestCallable: () -> OkHttpWebSocket.CloseRequest
    ) : RequestFactory {
        override fun createCloseRequest(): OkHttpWebSocket.CloseRequest {
            return createCloseRequestCallable()
        }
    }
}
