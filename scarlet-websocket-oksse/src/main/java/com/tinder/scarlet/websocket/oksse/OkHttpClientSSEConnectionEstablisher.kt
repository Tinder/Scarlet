/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.websocket.oksse

import com.here.oksse.OkSse
import com.here.oksse.ServerSentEvent
import com.tinder.scarlet.websocket.okhttp.request.RequestFactory
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class OkHttpClientSseConnectionEstablisher(
    okHttpClient: OkHttpClient,
    private val requestFactory: RequestFactory
) : OkSseWebSocket.ConnectionEstablisher {

    private val okHttpClient = okHttpClient.newBuilder()
        .readTimeout(0, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    override fun establishConnection(sseListener: ServerSentEvent.Listener) {
        val request = requestFactory.createRequest()
        val okSse = OkSse(okHttpClient)
        okSse.newServerSentEvent(request, sseListener)
    }
}
