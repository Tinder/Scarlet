/*
 * © 2018 Match Group, LLC.
 */

@file:JvmName("MockWebServerUtils")

package com.tinder.scarlet.websocket.mockwebserver

import com.tinder.scarlet.WebSocket
import com.tinder.scarlet.websocket.okhttp.OkHttpWebSocket
import okhttp3.mockwebserver.MockWebServer

fun MockWebServer.newWebSocketFactory(): WebSocket.Factory =
    OkHttpWebSocket.Factory(MockWebServerOkHttpWebSocketConnectionEstablisher(this))
