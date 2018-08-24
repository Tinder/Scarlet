/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.websocket.okhttp

import com.nhaarman.mockito_kotlin.mock
import com.tinder.scarlet.Message
import com.tinder.scarlet.ShutdownReason
import com.tinder.scarlet.WebSocket
import okhttp3.Response
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
internal class OkHttpWebSocketEventObserverTest {

    private val okHttpWebSocketEventObserver = OkHttpWebSocketEventObserver()

    @Test
    fun observe_shouldReturnAHotObservable() {
        // When
        val testSubscriber = okHttpWebSocketEventObserver.observe().test()

        // Then
        testSubscriber.assertNotTerminated()
    }

    @Test
    fun observe_shouldObserveWebSocketEvents() {
        // Given
        val webSocket = mock<okhttp3.WebSocket>()
        val response = mock<Response>()
        val throwable = mock<Throwable>()

        // When
        val testSubscriber = okHttpWebSocketEventObserver.observe().test()
        okHttpWebSocketEventObserver.onOpen(webSocket, response)
        okHttpWebSocketEventObserver.onMessage(webSocket, "Hello")
        okHttpWebSocketEventObserver.onClosing(webSocket, 1001, "")
        okHttpWebSocketEventObserver.onClosed(webSocket, 1001, "")
        okHttpWebSocketEventObserver.onFailure(webSocket, throwable, null)

        // Then
        testSubscriber.assertValues(
            WebSocket.Event.OnConnectionOpened(webSocket),
            WebSocket.Event.OnMessageReceived(Message.Text("Hello")),
            WebSocket.Event.OnConnectionClosing(ShutdownReason(1001, "")),
            WebSocket.Event.OnConnectionClosed(ShutdownReason(1001, "")),
            WebSocket.Event.OnConnectionFailed(throwable)
        )
    }
}
