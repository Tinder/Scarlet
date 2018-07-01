/*
 * © 2018 Match Group, LLC.
 */

@file:JvmName("OkHttpClientUtils")

package com.tinder.scarlet.websocket.okhttp

import com.tinder.scarlet.WebSocket
import com.tinder.scarlet.websocket.okhttp.request.RequestFactory
import com.tinder.scarlet.websocket.okhttp.request.StaticUrlRequestFactory
import okhttp3.OkHttpClient

fun OkHttpClient.newWebSocketFactory(requestFactory: RequestFactory): WebSocket.Factory {
    return OkHttpWebSocket.Factory(OkHttpClientWebSocketConnectionEstablisher(this, requestFactory))
}

fun OkHttpClient.newWebSocketFactory(url: String): WebSocket.Factory {
    return newWebSocketFactory(StaticUrlRequestFactory(url))
}
