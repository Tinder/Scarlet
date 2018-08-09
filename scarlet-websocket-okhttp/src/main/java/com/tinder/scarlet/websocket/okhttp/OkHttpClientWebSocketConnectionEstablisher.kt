/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.websocket.okhttp

import com.tinder.scarlet.websocket.okhttp.request.RequestFactory
import okhttp3.OkHttpClient
import okhttp3.WebSocketListener

class OkHttpClientWebSocketConnectionEstablisher(
    private val okHttpClient: OkHttpClient,
    private val requestFactory: RequestFactory
) : OkHttpWebSocket.ConnectionEstablisher {

    override fun establishConnection(webSocketListener: WebSocketListener) {
        val request = requestFactory.createRequest()
        okHttpClient.newWebSocket(request, webSocketListener)
    }
}
