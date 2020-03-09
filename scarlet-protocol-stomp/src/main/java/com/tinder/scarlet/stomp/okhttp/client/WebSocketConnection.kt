/*
 * © 2018 Match Group, LLC.
 */
package com.tinder.scarlet.stomp.okhttp.client

import com.tinder.scarlet.stomp.okhttp.core.Connection
import com.tinder.scarlet.stomp.okhttp.core.MessageHandler
import com.tinder.scarlet.stomp.okhttp.models.StompMessage
import com.tinder.scarlet.stomp.okhttp.support.StompMessageDecoder
import com.tinder.scarlet.stomp.okhttp.support.StompMessageEncoder
import okhttp3.WebSocket
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Okhttp websocket based implementation of {@link Connection}.
 */
class WebSocketConnection(
    private val webSocket: WebSocket
) : Connection, MessageHandler {

    @Volatile
    private var lastReadTime: Long = -1

    @Volatile
    private var lastWriteTime: Long = -1

    private val executor = Executors.newSingleThreadScheduledExecutor()

    private val messageEncoder = StompMessageEncoder()
    private val messageDecoder = StompMessageDecoder()

    companion object {

        private const val NORMAL_CLOSURE_STATUS_CODE = 1000
        private const val NORMAL_CLOSURE_REASON = "Normal closure"
    }

    /**
     * {@inheritDoc}
     */
    override fun sendMessage(message: StompMessage): Boolean {
        val lastWriteTime = lastWriteTime
        if (lastWriteTime != -1L) {
            this.lastWriteTime = System.currentTimeMillis()
        }
        val encodedMessage = messageEncoder.encode(message)
        return webSocket.send(String(encodedMessage))
    }

    /**
     * {@inheritDoc}
     */
    override fun onReceiveInactivity(duration: Long, runnable: () -> Unit) {
        lastReadTime = System.currentTimeMillis()
        executor.scheduleWithFixedDelay({
            if (System.currentTimeMillis() - lastReadTime > duration) {
                runnable.invoke()
            }
        }, 0, duration / 2, TimeUnit.MILLISECONDS)
    }

    /**
     * {@inheritDoc}
     */
    override fun onWriteInactivity(duration: Long, runnable: () -> Unit) {
        lastWriteTime = System.currentTimeMillis()
        executor.scheduleWithFixedDelay({
            if (System.currentTimeMillis() - lastWriteTime > duration) {
                runnable.invoke()
            }
        }, 0, duration / 2, TimeUnit.MILLISECONDS)
    }

    /**
     * {@inheritDoc}
     */
    override fun forceClose() {
        webSocket.cancel()
        executor.shutdown()
    }

    /**
     * {@inheritDoc}
     */
    override fun close() {
        webSocket.close(NORMAL_CLOSURE_STATUS_CODE, NORMAL_CLOSURE_REASON)
        executor.shutdown()
    }

    /**
     * {@inheritDoc}
     */
    override fun handle(data: ByteArray): StompMessage? {
        val lastReadTime = lastReadTime
        if (lastReadTime != -1L) {
            this.lastReadTime = System.currentTimeMillis()
        }
        return messageDecoder.decode(data)
    }
}