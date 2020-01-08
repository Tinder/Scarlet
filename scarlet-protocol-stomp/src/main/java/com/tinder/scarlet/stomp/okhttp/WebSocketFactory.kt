/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.stomp.okhttp

import okhttp3.Request
import okhttp3.WebSocketListener

interface WebSocketFactory {

    fun createWebSocket(request: Request, listener: WebSocketListener)
}
