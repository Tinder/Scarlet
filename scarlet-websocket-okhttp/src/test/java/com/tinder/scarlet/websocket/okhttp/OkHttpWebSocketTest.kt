/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.websocket.okhttp

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.then
import com.tinder.scarlet.WebSocket.Event
import com.tinder.scarlet.Message
import com.tinder.scarlet.ShutdownReason
import com.tinder.scarlet.testutils.test
import io.reactivex.processors.ReplayProcessor
import okhttp3.WebSocket
import okio.ByteString
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
internal class OkHttpWebSocketTest {

    private val webSocketHolder = mock<OkHttpWebSocketHolder> {
        on { send(any<String>()) } doReturn true
        on { send(any<ByteString>()) } doReturn true
        on { close(any(), any()) } doReturn true
    }
    private val eventProcessor = ReplayProcessor.create<Event>()
    private val webSocketEventObserver = mock<OkHttpWebSocketEventObserver> {
        on { observe() } doReturn eventProcessor
    }
    private val connectionEstablisher = mock<OkHttpWebSocket.ConnectionEstablisher>()
    private val okHttpWebSocket = OkHttpWebSocket(webSocketHolder, webSocketEventObserver, connectionEstablisher)

    @Test
    fun open_shouldSubscribeToOkHttpWebSocketEventObserver() {
        // When
        val testStreamObserver = okHttpWebSocket.open().test()

        // Then
        then(webSocketEventObserver).should().observe()
        assertThat(eventProcessor.hasSubscribers()).isTrue()
        assertThat(testStreamObserver.completions).isZero()
    }

    @Test
    fun open_shouldEstablishConnection() {
        // When
        okHttpWebSocket.open().test()

        // Then
        then(connectionEstablisher).should().establishConnection(webSocketEventObserver)
    }

    @Test
    fun open_onConnectionOpen_shouldInitiateWebSocketHolder() {
        // Given
        val aWebSocket = mock<WebSocket>()
        val events = arrayOf(
            mock<Event.OnConnectionOpened<WebSocket>> {
                on { webSocket } doReturn aWebSocket
            }
        )
        events.forEach { eventProcessor.onNext(it) }

        // When
        val testStreamObserver = okHttpWebSocket.open().test()

        // Then
        assertThat(testStreamObserver.values).containsExactly(*events)
        then(webSocketHolder).should().initiate(aWebSocket)
    }

    @Test
    fun open_onConnectionClosing_shouldCloseWebSocket() {
        // Given
        val events = arrayOf(
            mock<Event.OnConnectionOpened<WebSocket>> {
                on { webSocket } doReturn mock<WebSocket>()
            },
            mock<Event.OnConnectionClosing>()
        )
        events.forEach { eventProcessor.onNext(it) }
        val (expectedCode, expectedReason) = ShutdownReason.GRACEFUL

        // When
        val testStreamObserver = okHttpWebSocket.open().test()

        // Then
        assertThat(testStreamObserver.values).containsExactly(*events)
        then(webSocketHolder).should().close(expectedCode, expectedReason)
    }

    @Test
    fun open_onConnectionClosed_shouldTerminate() {
        // Given
        val events = arrayOf(
            mock<Event.OnConnectionOpened<WebSocket>> {
                on { webSocket } doReturn mock<WebSocket>()
            },
            mock<Event.OnConnectionClosed>()
        )
        events.forEach { eventProcessor.onNext(it) }

        // When
        val testStreamObserver = okHttpWebSocket.open().test()

        // Then
        assertThat(testStreamObserver.values).containsExactly(*events)
        then(webSocketEventObserver).should().terminate()
    }

    @Test
    fun open_onConnectionFailed_shouldTerminate() {
        // Given
        val events = arrayOf(
            mock<Event.OnConnectionOpened<WebSocket>> {
                on { webSocket } doReturn mock<WebSocket>()
            },
            mock<Event.OnConnectionFailed>()
        )
        events.forEach { eventProcessor.onNext(it) }

        // When
        val testStreamObserver = okHttpWebSocket.open().test()

        // Then
        assertThat(testStreamObserver.values).containsExactly(*events)
        then(webSocketEventObserver).should().terminate()
    }

    @Test
    fun send_givenTextMessage_shouldSendMessage() {
        // Given
        val text = "Hello"

        // When
        val isSuccessful = okHttpWebSocket.send(Message.Text(text))

        // Then
        assertThat(isSuccessful).isTrue()
        then(webSocketHolder).should().send(text)
    }

    @Test
    fun send_givenBytesMessage_shouldSendMessage() {
        // Given
        val bytes = "Hey".toByteArray()

        // When
        val isSuccessful = okHttpWebSocket.send(Message.Bytes(bytes))

        // Then
        assertThat(isSuccessful).isTrue()
        then(webSocketHolder).should().send(ByteString.of(*bytes))
    }

    @Test
    fun close_shouldCloseWebSocket() {
        // Given
        val code = 1001
        val reason = "Some reason"

        // When
        val isSuccessful = okHttpWebSocket.close(ShutdownReason(code, reason))

        // Then
        then(webSocketHolder).should().close(code, reason)
        assertThat(isSuccessful).isTrue()
    }

    @Test
    fun cancel_shouldCancelWebSocket() {
        // When
        okHttpWebSocket.cancel()

        // Then
        then(webSocketHolder).should().cancel()
    }
}
