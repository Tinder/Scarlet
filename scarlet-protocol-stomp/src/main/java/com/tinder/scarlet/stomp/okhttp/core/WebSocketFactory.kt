/*
 * © 2018 Match Group, LLC.
 */
package com.tinder.scarlet.stomp.okhttp.core

import okhttp3.Request
import okhttp3.WebSocketListener

/**
 * An factory for create STOMP WebSocket connection.
 */
interface WebSocketFactory {

    fun createWebSocket(request: Request, listener: WebSocketListener)
}
