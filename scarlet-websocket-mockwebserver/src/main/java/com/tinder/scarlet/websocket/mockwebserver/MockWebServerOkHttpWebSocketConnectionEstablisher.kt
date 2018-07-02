/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.websocket.mockwebserver

import com.tinder.scarlet.websocket.okhttp.OkHttpWebSocket
import okhttp3.WebSocketListener
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer

class MockWebServerOkHttpWebSocketConnectionEstablisher(
    private val mockWebServer: MockWebServer
) : OkHttpWebSocket.ConnectionEstablisher {

    override fun establishConnection(webSocketListener: WebSocketListener) {
        mockWebServer.enqueue(MockResponse().withWebSocketUpgrade(webSocketListener))
    }
}
