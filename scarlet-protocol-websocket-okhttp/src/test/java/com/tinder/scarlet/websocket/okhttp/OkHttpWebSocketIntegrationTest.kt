/*
 * © 2018 Match Group, LLC.
 */
package com.tinder.scarlet.websocket.okhttp

import com.tinder.scarlet.Stream
import com.tinder.scarlet.testutils.any
import com.tinder.scarlet.testutils.test
import com.tinder.scarlet.testutils.v2.OkHttpWebSocketConnection
import com.tinder.scarlet.testutils.v2.containingBytes
import com.tinder.scarlet.testutils.v2.containingText
import com.tinder.scarlet.testutils.v2.withClosedReason
import com.tinder.scarlet.testutils.v2.withClosingReason
import com.tinder.scarlet.websocket.ShutdownReason
import com.tinder.scarlet.websocket.WebSocketEvent
import com.tinder.scarlet.ws.Receive
import com.tinder.scarlet.ws.Send
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test

internal class OkHttpWebSocketIntegrationTest {

    @get:Rule
    internal val connection = OkHttpWebSocketConnection.create<Service>(
        observeWebSocketEvent = { observeWebSocketEvent() },
        serverConfiguration = OkHttpWebSocketConnection.Configuration(
            shutdownReason = SERVER_SHUTDOWN_REASON
        ),
        clientConfiguration = OkHttpWebSocketConnection.Configuration(
            shutdownReason = CLIENT_SHUTDOWN_REASON
        )
    )

    @Test
    fun send_givenConnectionIsEstablished_shouldBeReceivedByTheServer() {
        // Given
        connection.open()
        val textMessage1 = "Hello"
        val textMessage2 = "Hi"
        val bytesMessage1 = "Yo".toByteArray()
        val bytesMessage2 = "Sup".toByteArray()
        val serverStringObserver = connection.server.observeText().test()
        val serverBytesObserver = connection.server.observeBytes().test()

        // When
        connection.client.sendText(textMessage1)
        val isSendTextEnqueued = connection.client.sendTextAndConfirm(textMessage2)
        connection.client.sendBytes(bytesMessage1)
        val isSendBytesEnqueued = connection.client.sendBytesAndConfirm(bytesMessage2)

        // Then
        assertThat(isSendTextEnqueued).isTrue()
        assertThat(isSendBytesEnqueued).isTrue()
        connection.serverWebSocketEventObserver.awaitValues(
            any<WebSocketEvent.OnConnectionOpened>(),
            any<WebSocketEvent.OnMessageReceived>().containingText(textMessage1),
            any<WebSocketEvent.OnMessageReceived>().containingText(textMessage2),
            any<WebSocketEvent.OnMessageReceived>().containingBytes(bytesMessage1),
            any<WebSocketEvent.OnMessageReceived>().containingBytes(bytesMessage2)
        )
        serverStringObserver.awaitValues(
            any<String> { assertThat(this).isEqualTo(textMessage1) },
            any<String> { assertThat(this).isEqualTo(textMessage2) }
        )
        serverBytesObserver.awaitValues(
            any<ByteArray> { assertThat(this).isEqualTo(bytesMessage1) },
            any<ByteArray> { assertThat(this).isEqualTo(bytesMessage2) }
        )
    }

    @Test
    fun send_givenConnectionIsNotEstablished_shouldFail() {
        // Given
        val textMessage = "Hello"
        val bytesMessage = "Yo".toByteArray()
        val serverStringObserver = connection.server.observeText().test()
        val serverBytesObserver = connection.server.observeText().test()

        // When
        val isSendTextSuccessful = connection.client.sendTextAndConfirm(textMessage)
        val isSendBytesSuccessful = connection.client.sendBytesAndConfirm(bytesMessage)

        // Then
        assertThat(isSendTextSuccessful).isFalse()
        assertThat(isSendBytesSuccessful).isFalse()
        serverStringObserver.awaitValues()
        serverBytesObserver.awaitValues()
    }

    @Test
    fun close_givenConnectionIsEstablished_shouldCloseServer() {
        // Given
        connection.open()

        // When
        connection.clientClosure()

        // Then
        connection.serverWebSocketEventObserver.awaitValues(
            any<WebSocketEvent.OnConnectionOpened>(),
            any<WebSocketEvent.OnConnectionClosing>().withClosingReason(CLIENT_SHUTDOWN_REASON),
            any<WebSocketEvent.OnConnectionClosed>().withClosedReason(CLIENT_SHUTDOWN_REASON)
        )
    }

    @Test
    fun cancel_givenConnectionIsEstablished_shouldFailTheConnection() {
        // When
        connection.open()
        connection.clientTerminate()

        // Then
        connection.clientWebSocketEventObserver.awaitValues(
            any<WebSocketEvent.OnConnectionOpened>(),
            any<WebSocketEvent.OnConnectionFailed>()
        )
        connection.serverWebSocketEventObserver.awaitValues(
            any<WebSocketEvent.OnConnectionOpened>(),
            any<WebSocketEvent.OnConnectionFailed>()
        )
    }

    @Test
    fun givenConnectionIsEstablished_andServerCloses_shouldClose() {
        // Given
        connection.open()

        // When
        connection.serverClosure()

        // Then
        connection.clientWebSocketEventObserver.awaitValues(
            any<WebSocketEvent.OnConnectionOpened>(),
            any<WebSocketEvent.OnConnectionClosing>().withClosingReason(SERVER_SHUTDOWN_REASON),
            any<WebSocketEvent.OnConnectionClosed>().withClosedReason(SERVER_SHUTDOWN_REASON)
        )
    }

    @Test
    fun givenConnectionIsEstablished_andServerSendsMessages_shouldReceiveMessages() {
        // Given
        connection.open()
        val textMessage = "Hello"
        val bytesMessage = "Hi".toByteArray()
        val testTextStreamObserver = connection.client.observeText().test()
        val testBytesStreamObserver = connection.client.observeBytes().test()

        // When
        connection.server.sendText(textMessage)
        connection.server.sendBytes(bytesMessage)

        // Then
        connection.clientWebSocketEventObserver.awaitValues(
            any<WebSocketEvent.OnConnectionOpened>(),
            any<WebSocketEvent.OnMessageReceived>().containingText(textMessage),
            any<WebSocketEvent.OnMessageReceived>().containingBytes(bytesMessage)
        )
        assertThat(testTextStreamObserver.values).containsExactly(textMessage)
        assertThat(testBytesStreamObserver.values).containsExactly(bytesMessage)
    }

    internal interface Service {
        @Receive
        fun observeWebSocketEvent(): Stream<WebSocketEvent>

        @Receive
        fun observeText(): Stream<String>

        @Receive
        fun observeBytes(): Stream<ByteArray>

        @Send
        fun sendText(message: String)

        @Send
        fun sendTextAndConfirm(message: String): Boolean

        @Send
        fun sendBytes(message: ByteArray)

        @Send
        fun sendBytesAndConfirm(message: ByteArray): Boolean
    }

    private companion object {

        private val CLIENT_SHUTDOWN_REASON = ShutdownReason(1001, "client away")
        private val SERVER_SHUTDOWN_REASON = ShutdownReason(1002, "mockWebServer shutdown")

    }
}
