/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.websocket.oksse

import com.here.oksse.ServerSentEvent
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.KotlinJsonAdapterFactory
import com.squareup.moshi.Moshi
import com.tinder.scarlet.Message
import com.tinder.scarlet.WebSocket
import com.tinder.scarlet.websocket.oksse.model.ServerSentMessage
import io.reactivex.Flowable
import io.reactivex.processors.PublishProcessor
import okhttp3.Request
import okhttp3.Response

internal class OkSseWebSocketEventObserver(
    private val jsonAdapter: JsonAdapter<ServerSentMessage>
) : ServerSentEvent.Listener {
    private val processor = PublishProcessor.create<WebSocket.Event>().toSerialized()

    fun observe(): Flowable<WebSocket.Event> = processor

    fun terminate() = processor.onComplete()

    override fun onOpen(sse: ServerSentEvent, response: Response) {
        processor.onNext(WebSocket.Event.OnConnectionOpened(sse))
    }

    override fun onRetryTime(sse: ServerSentEvent, milliseconds: Long): Boolean {
        return true
    }

    override fun onComment(sse: ServerSentEvent, comment: String?) {
        // The comment line can be used to prevent connections from timing out;
        // a server can send a comment periodically to keep the connection alive.
    }

    override fun onRetryError(
        sse: ServerSentEvent,
        throwable: Throwable?,
        response: Response?
    ): Boolean {
        return true
    }

    override fun onPreRetry(sse: ServerSentEvent, originalRequest: Request): Request {
        return originalRequest
    }

    override fun onMessage(sse: ServerSentEvent, id: String, event: String, message: String) {
        val jsonString = jsonAdapter.toJson(
            ServerSentMessage(
                event,
                message
            )
        )
        processor.onNext(WebSocket.Event.OnMessageReceived(Message.Text(jsonString)))
    }

    override fun onClosed(sse: ServerSentEvent) {
        processor.onNext(WebSocket.Event.OnConnectionFailed(RuntimeException()))
    }
}
