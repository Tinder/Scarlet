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
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * Okhttp websocket based implementation of {@link Connection}.
 */
class WebSocketConnection(
    private val webSocket: WebSocket,
    private val executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
) : Connection, MessageHandler {

    @Volatile
    private var lastReadTime: Long = -1

    @Volatile
    private var lastWriteTime: Long = -1

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
            this.lastWriteTime = System.nanoTime()
        }
        val encodedMessage = messageEncoder.encode(message).toString(Charsets.UTF_8)
        return webSocket.send(encodedMessage)
    }

    /**
     * {@inheritDoc}
     */
    override fun onReceiveInactivity(duration: Long, runnable: () -> Unit) {
        check(duration > 0) { "Duration must be more than 0" }
        lastReadTime = System.nanoTime()
        executor.scheduleWithFixedDelay({
            if ((System.nanoTime() - lastReadTime) > TimeUnit.MILLISECONDS.toNanos(duration)) {
                runnable.invoke()
            }
        }, 0, duration / 2L, TimeUnit.MILLISECONDS)
    }

    /**
     * {@inheritDoc}
     */
    override fun onWriteInactivity(duration: Long, runnable: () -> Unit) {
        check(duration > 0) { "Duration must be more than 0" }
        lastWriteTime = System.nanoTime()
        executor.scheduleWithFixedDelay({
            if ((System.nanoTime() - lastWriteTime) > TimeUnit.MILLISECONDS.toNanos(duration)) {
                runnable.invoke()
            }
        }, 0, duration / 2L, TimeUnit.MILLISECONDS)
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
            this.lastReadTime = System.nanoTime()
        }
        return messageDecoder.decode(data)
    }
}