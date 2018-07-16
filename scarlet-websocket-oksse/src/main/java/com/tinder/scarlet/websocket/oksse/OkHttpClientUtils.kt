/*
 * © 2018 Match Group, LLC.
 */

@file:JvmName("OkHttpClientUtils")

package com.tinder.scarlet.websocket.oksse

import com.tinder.scarlet.WebSocket
import com.tinder.scarlet.websocket.okhttp.request.RequestFactory
import com.tinder.scarlet.websocket.oksse.request.StaticUrlRequestFactory
import okhttp3.OkHttpClient

fun OkHttpClient.newSseWebSocketFactory(requestFactory: RequestFactory): WebSocket.Factory {
    return OkSseWebSocket.Factory(OkHttpClientSSEConnectionEstablisher(this, requestFactory))
}

fun OkHttpClient.newSseWebSocketFactory(url: String): WebSocket.Factory {
    return newSseWebSocketFactory(StaticUrlRequestFactory(url))
}
