package com.tinder.scarlet.stomp.okhttp

import com.tinder.scarlet.stomp.core.Connection
import com.tinder.scarlet.stomp.core.models.StompMessage
import com.tinder.scarlet.stomp.support.StompMessageDecoder
import com.tinder.scarlet.stomp.support.StompMessageEncoder
import okhttp3.WebSocket
import okio.ByteString
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

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

    override fun sendMessage(message: StompMessage): Boolean {
        val lastWriteTime = lastWriteTime
        if (lastWriteTime != -1L) {
            this.lastWriteTime = System.currentTimeMillis()
        }
        val encodedMessage = messageEncoder.encode(message)
        val byteString = ByteString.of(encodedMessage, 0, encodedMessage.size)
        return webSocket.send(byteString)
    }

    override fun onReceiveInactivity(duration: Long, runnable: () -> Unit) {
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
        executor.shutdown()
    }

    override fun close() {
        webSocket.close(NORMAL_CLOSURE_STATUS_CODE, NORMAL_CLOSURE_REASON)
        executor.shutdown()
    }

    override fun handle(data: ByteArray): StompMessage {
        val lastReadTime = lastReadTime
        if (lastReadTime != -1L) {
            this.lastReadTime = System.currentTimeMillis()
        }
        return messageDecoder.decode(data)
    }
}