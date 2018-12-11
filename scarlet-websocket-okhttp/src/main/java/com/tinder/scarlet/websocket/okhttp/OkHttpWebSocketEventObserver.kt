/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.websocket.okhttp

import com.tinder.scarlet.Message
import com.tinder.scarlet.ShutdownReason
import com.tinder.scarlet.WebSocket
import io.reactivex.Flowable
import io.reactivex.processors.PublishProcessor
import okhttp3.Response
import okhttp3.WebSocketListener
import okio.ByteString

internal class OkHttpWebSocketEventObserver : WebSocketListener() {
    private val processor = PublishProcessor.create<WebSocket.Event>().toSerialized()

    fun observe(): Flowable<WebSocket.Event> = processor.onBackpressureBuffer()

    fun terminate() = processor.onComplete()

    override fun onOpen(webSocket: okhttp3.WebSocket, response: Response) =
        processor.onNext(WebSocket.Event.OnConnectionOpened(webSocket))

    override fun onMessage(webSocket: okhttp3.WebSocket, bytes: ByteString) =
        processor.onNext(WebSocket.Event.OnMessageReceived(Message.Bytes(bytes.toByteArray())))

    override fun onMessage(webSocket: okhttp3.WebSocket, text: String) =
        processor.onNext(WebSocket.Event.OnMessageReceived(Message.Text(text)))

    override fun onClosing(webSocket: okhttp3.WebSocket, code: Int, reason: String) =
        processor.onNext(WebSocket.Event.OnConnectionClosing(ShutdownReason(code, reason)))

    override fun onClosed(webSocket: okhttp3.WebSocket, code: Int, reason: String) =
        processor.onNext(WebSocket.Event.OnConnectionClosed(ShutdownReason(code, reason)))

    override fun onFailure(webSocket: okhttp3.WebSocket, t: Throwable, response: Response?) =
        processor.onNext(WebSocket.Event.OnConnectionFailed(t))
}
