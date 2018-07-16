/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.websocket.oksse

import com.here.oksse.ServerSentEvent
import okhttp3.Request
import java.util.concurrent.TimeUnit

internal class ServerSentEventHolder : ServerSentEvent {
    private var serverSentEvent: ServerSentEvent? = null

    fun initiate(serverSentEvent: ServerSentEvent) {
        this.serverSentEvent = serverSentEvent
    }

    fun shutdown() {
        serverSentEvent = null
    }

    override fun setTimeout(timeout: Long, unit: TimeUnit?) {
        val serverSentEvent = checkNotNull(serverSentEvent)
        serverSentEvent.setTimeout(timeout, unit)
    }

    override fun close() {
        serverSentEvent?.close()
    }

    override fun request(): Request {
        val serverSentEvent = checkNotNull(serverSentEvent)
        return serverSentEvent.request()
    }
}
