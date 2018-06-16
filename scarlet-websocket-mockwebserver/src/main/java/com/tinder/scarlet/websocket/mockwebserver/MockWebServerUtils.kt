/*
 * © 2013 - 2018 Tinder, Inc., ALL RIGHTS RESERVED
 */

@file:JvmName("MockWebServerUtils")

package com.tinder.scarlet.websocket.mockwebserver

import com.tinder.scarlet.WebSocket
import com.tinder.scarlet.websocket.okhttp.OkHttpWebSocket
import okhttp3.mockwebserver.MockWebServer

fun MockWebServer.newWebSocketFactory(): WebSocket.Factory =
    OkHttpWebSocket.Factory(MockWebServerOkHttpWebSocketConnectionEstablisher(this))
