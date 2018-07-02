/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.websocket.okhttp

import com.tinder.scarlet.Scarlet
import com.tinder.scarlet.ShutdownReason
import com.tinder.scarlet.Stream
import com.tinder.scarlet.Lifecycle
import com.tinder.scarlet.WebSocket
import com.tinder.scarlet.lifecycle.LifecycleRegistry
import com.tinder.scarlet.testutils.TestStreamObserver
import com.tinder.scarlet.testutils.containingBytes
import com.tinder.scarlet.testutils.containingText
import com.tinder.scarlet.testutils.test
import com.tinder.scarlet.testutils.any
import com.tinder.scarlet.testutils.withClosedReason
import com.tinder.scarlet.testutils.withClosingReason
import com.tinder.scarlet.websocket.mockwebserver.newWebSocketFactory
import com.tinder.scarlet.ws.Receive
import com.tinder.scarlet.ws.Send
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.TimeUnit

internal class OkHttpWebSocketIntegrationTest {

    @get:Rule
    private val mockWebServer = MockWebServer()
    private val serverUrlString by lazy { mockWebServer.url("/").toString() }

    private val serverLifecycleRegistry = LifecycleRegistry()
    private lateinit var server: Service
    private lateinit var serverWebSocketEventObserver: TestStreamObserver<WebSocket.Event>

    private val clientLifecycleRegistry = LifecycleRegistry()
    private lateinit var client: Service
    private lateinit var clientWebSocketEventObserver: TestStreamObserver<WebSocket.Event>

    @Test
    fun send_givenConnectionIsEstablished_shouldBeReceivedByTheServer() {
        // Given
        givenConnectionIsEstablished()
        val textMessage1 = "Hello"
        val textMessage2 = "Hi"
        val bytesMessage1 = "Yo".toByteArray()
        val bytesMessage2 = "Sup".toByteArray()
        val serverStringObserver = server.observeText().test()
        val serverBytesObserver = server.observeBytes().test()

        // When
        client.sendText(textMessage1)
        val isSendTextSuccessful = client.sendTextAndConfirm(textMessage2)
        client.sendBytes(bytesMessage1)
        val isSendBytesSuccessful = client.sendBytesAndConfirm(bytesMessage2)

        // Then
        assertThat(isSendTextSuccessful).isTrue()
        assertThat(isSendBytesSuccessful).isTrue()
        serverWebSocketEventObserver.awaitValues(
            any<WebSocket.Event.OnConnectionOpened<*>>(),
            any<WebSocket.Event.OnMessageReceived>().containingText(textMessage1),
            any<WebSocket.Event.OnMessageReceived>().containingText(textMessage2),
            any<WebSocket.Event.OnMessageReceived>().containingBytes(bytesMessage1),
            any<WebSocket.Event.OnMessageReceived>().containingBytes(bytesMessage2)
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
        createClientAndServer()
        val textMessage = "Hello"
        val bytesMessage = "Yo".toByteArray()
        val serverStringObserver = server.observeText().test()
        val serverBytesObserver = server.observeText().test()

        // When
        val isSendTextSuccessful = client.sendTextAndConfirm(textMessage)
        val isSendBytesSuccessful = client.sendBytesAndConfirm(bytesMessage)

        // Then
        assertThat(isSendTextSuccessful).isFalse()
        assertThat(isSendBytesSuccessful).isFalse()
        serverStringObserver.awaitValues()
        serverBytesObserver.awaitValues()
    }

    @Test
    fun close_givenConnectionIsEstablished_shouldCloseServer() {
        // Given
        givenConnectionIsEstablished()
        val clientCloseReason = ShutdownReason(1001, "client away")

        // When
        clientLifecycleRegistry.onNext(Lifecycle.State.Stopped.WithReason(clientCloseReason))

        // Then
        serverWebSocketEventObserver.awaitValues(
            any<WebSocket.Event.OnConnectionOpened<*>>(),
            any<WebSocket.Event.OnConnectionClosing>().withClosingReason(clientCloseReason),
            any<WebSocket.Event.OnConnectionClosed>().withClosedReason(clientCloseReason)
        )
    }

    @Test
    fun cancel_givenConnectionIsEstablished_shouldFailTheConnection() {
        // When
        givenConnectionIsEstablished()
        clientLifecycleRegistry.onNext(Lifecycle.State.Stopped.AndAborted)

        // Then
        clientWebSocketEventObserver.awaitValues(
            any<WebSocket.Event.OnConnectionOpened<*>>(),
            any<WebSocket.Event.OnConnectionFailed>()
        )
        serverWebSocketEventObserver.awaitValues(
            any<WebSocket.Event.OnConnectionOpened<*>>(),
            any<WebSocket.Event.OnConnectionFailed>()
        )
    }

    @Test
    fun givenConnectionIsEstablished_andServerCloses_shouldClose() {
        // Given
        givenConnectionIsEstablished()
        val serverCloseReason = ShutdownReason(1002, "mockWebServer shutdown")

        // When
        serverLifecycleRegistry.onNext(Lifecycle.State.Stopped.WithReason(serverCloseReason))

        // Then
        clientWebSocketEventObserver.awaitValues(
            any<WebSocket.Event.OnConnectionOpened<*>>(),
            any<WebSocket.Event.OnConnectionClosing>().withClosingReason(serverCloseReason),
            any<WebSocket.Event.OnConnectionClosed>().withClosedReason(serverCloseReason)
        )
    }

    @Test
    fun givenConnectionIsEstablished_andServerSendsMessages_shouldReceiveMessages() {
        // Given
        givenConnectionIsEstablished()
        val textMessage = "Hello"
        val bytesMessage = "Hi".toByteArray()
        val testTextStreamObserver = client.observeText().test()
        val testBytesStreamObserver = client.observeBytes().test()

        // When
        server.sendText(textMessage)
        server.sendBytes(bytesMessage)

        // Then
        clientWebSocketEventObserver.awaitValues(
            any<WebSocket.Event.OnConnectionOpened<*>>(),
            any<WebSocket.Event.OnMessageReceived>().containingText(textMessage),
            any<WebSocket.Event.OnMessageReceived>().containingBytes(bytesMessage)
        )
        assertThat(testTextStreamObserver.values).containsExactly(textMessage)
        assertThat(testBytesStreamObserver.values).containsExactly(bytesMessage)
    }

    private fun givenConnectionIsEstablished() {
        createClientAndServer()
        serverLifecycleRegistry.onNext(Lifecycle.State.Started)
        clientLifecycleRegistry.onNext(Lifecycle.State.Started)
        blockUntilConnectionIsEstablish()
    }

    private fun createClientAndServer() {
        server = createServer()
        serverWebSocketEventObserver = server.observeWebSocketEvent().test()
        client = createClient()
        clientWebSocketEventObserver = client.observeWebSocketEvent().test()
    }

    private fun createServer(): Service = Scarlet.Builder()
        .webSocketFactory(mockWebServer.newWebSocketFactory())
        .lifecycle(serverLifecycleRegistry)
        .build()
        .create()

    private fun createClient(): Service = Scarlet.Builder()
        .webSocketFactory(createOkHttpClient().newWebSocketFactory(serverUrlString))
        .lifecycle(clientLifecycleRegistry)
        .build().create()

    private fun createOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .writeTimeout(500, TimeUnit.MILLISECONDS)
        .readTimeout(500, TimeUnit.MILLISECONDS)
        .build()

    private fun blockUntilConnectionIsEstablish() {
        clientWebSocketEventObserver.awaitValues(
            any<WebSocket.Event.OnConnectionOpened<*>>()
        )
        serverWebSocketEventObserver.awaitValues(
            any<WebSocket.Event.OnConnectionOpened<*>>()
        )
    }

    private interface Service {
        @Receive
        fun observeWebSocketEvent(): Stream<WebSocket.Event>

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
}
