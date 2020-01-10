package com.tinder.scarlet.stomp.okhttp

import okhttp3.WebSocket
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


class WebSocketConnection(
    private val webSocket: WebSocket
) : Connection {

    @Volatile
    private var lastReadTime: Long = -1

    @Volatile
    private var lastWriteTime: Long = -1

    private val executor = Executors.newSingleThreadScheduledExecutor()

    companion object {

        private const val NORMAL_CLOSURE_STATUS_CODE = 1000
        private const val NORMAL_CLOSURE_REASON = "Normal closure"

    }

    override fun send(message: String): Boolean {
        updateLastWriteTime()
        return webSocket.send(message)
    }

    override fun onReadInactivity(duration: Long, runnable: () -> Unit) {
        lastReadTime = System.currentTimeMillis()
        executor.scheduleWithFixedDelay({
            if (System.currentTimeMillis() - lastReadTime > duration) {
                runnable.invoke()
            }
        }, 0, duration / 2, TimeUnit.MILLISECONDS)
    }

    override fun onWriteInactivity(duration: Long, runnable: () -> Unit) {
        lastWriteTime = System.currentTimeMillis()
        executor.scheduleWithFixedDelay({
            if (System.currentTimeMillis() - lastWriteTime > duration) {
                runnable.invoke()
            }
        }, 0, duration / 2, TimeUnit.MILLISECONDS)
    }

    override fun forceClose() {
        webSocket.cancel()
    }

    override fun close() {
        webSocket.close(NORMAL_CLOSURE_STATUS_CODE, NORMAL_CLOSURE_REASON)
    }

    private fun updateLastWriteTime() {
        val lastWriteTime = lastWriteTime
        if (lastWriteTime != -1L) {
            this.lastWriteTime = System.currentTimeMillis()
        }
    }

}