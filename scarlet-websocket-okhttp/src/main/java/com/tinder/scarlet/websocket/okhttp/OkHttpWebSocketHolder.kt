/*
 * © 2013 - 2018 Tinder, Inc., ALL RIGHTS RESERVED
 */

package com.tinder.scarlet.websocket.okhttp

import okhttp3.WebSocket
import okio.ByteString

internal class OkHttpWebSocketHolder : WebSocket {
    private var webSocket: WebSocket? = null

    fun initiate(webSocket: WebSocket) {
        this.webSocket = webSocket
    }

    fun shutdown() {
        webSocket = null
    }

    override fun queueSize() = throw UnsupportedOperationException()

    override fun request() = throw UnsupportedOperationException()

    override fun send(text: String) = webSocket?.send(text) ?: false

    override fun send(bytes: ByteString) = webSocket?.send(bytes) ?: false

    override fun close(code: Int, reason: String?) = webSocket?.close(code, reason) ?: false

    override fun cancel() = webSocket?.cancel() ?: Unit
}
