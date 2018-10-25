/*
 * © 2018 Match Group, LLC.
 */
package com.tinder.scarlet.websocket.okhttp

import com.tinder.scarlet.Message
import com.tinder.scarlet.Stream
import com.tinder.scarlet.testutils.TestStreamObserver
import com.tinder.scarlet.testutils.ValueAssert
import com.tinder.scarlet.testutils.any
import com.tinder.scarlet.testutils.test
import com.tinder.scarlet.v2.LifecycleState
import com.tinder.scarlet.v2.Scarlet
import com.tinder.scarlet.v2.lifecycle.LifecycleRegistry
import com.tinder.scarlet.websocket.ShutdownReason
import com.tinder.scarlet.websocket.WebSocketEvent
import com.tinder.scarlet.websocket.mockwebserver.MockWebServerWebSocket
import com.tinder.scarlet.ws.Receive
import com.tinder.scarlet.ws.Send
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.TimeUnit

internal class OkHttpWebSocketIntegrationTest {

    @get:Rule
    val mockWebServer = MockWebServer()
    private val serverUrlString by lazy { mockWebServer.url("/").toString() }

    private val serverLifecycleRegistry = LifecycleRegistry()
    private lateinit var server: Service
    private lateinit var serverWebSocketEventObserver: TestStreamObserver<WebSocketEvent>

    private val clientLifecycleRegistry = LifecycleRegistry()
    private lateinit var client: Service
    private lateinit var clientWebSocketEventObserver: TestStreamObserver<WebSocketEvent>

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
        val isSendTextEnqueued = client.sendTextAndConfirm(textMessage2)
        client.sendBytes(bytesMessage1)
        val isSendBytesEnqueued = client.sendBytesAndConfirm(bytesMessage2)

        // Then
        assertThat(isSendTextEnqueued).isTrue()
        assertThat(isSendBytesEnqueued).isTrue()
        serverWebSocketEventObserver.awaitValues(
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

        // When
        clientLifecycleRegistry.onNext(LifecycleState.Stopped)

        // Then
        serverWebSocketEventObserver.awaitValues(
                any<WebSocketEvent.OnConnectionOpened>(),
                any<WebSocketEvent.OnConnectionClosing>().withClosingReason(CLIENT_SHUTDOWN_REASON),
                any<WebSocketEvent.OnConnectionClosed>().withClosedReason(CLIENT_SHUTDOWN_REASON)
        )
    }

    @Test
    fun cancel_givenConnectionIsEstablished_shouldFailTheConnection() {
        // When
        givenConnectionIsEstablished()
        clientLifecycleRegistry.onNext(LifecycleState.Completed)

        // Then
        clientWebSocketEventObserver.awaitValues(
                any<WebSocketEvent.OnConnectionOpened>(),
                any<WebSocketEvent.OnConnectionFailed>()
        )
        serverWebSocketEventObserver.awaitValues(
                any<WebSocketEvent.OnConnectionOpened>(),
                any<WebSocketEvent.OnConnectionFailed>()
        )
    }

    @Test
    fun givenConnectionIsEstablished_andServerCloses_shouldClose() {
        // Given
        givenConnectionIsEstablished()

        // When
        serverLifecycleRegistry.onNext(LifecycleState.Stopped)

        // Then
        clientWebSocketEventObserver.awaitValues(
                any<WebSocketEvent.OnConnectionOpened>(),
                any<WebSocketEvent.OnConnectionClosing>().withClosingReason(SERVER_SHUTDOWN_REASON),
                any<WebSocketEvent.OnConnectionClosed>().withClosedReason(SERVER_SHUTDOWN_REASON)
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
                any<WebSocketEvent.OnConnectionOpened>(),
                any<WebSocketEvent.OnMessageReceived>().containingText(textMessage),
                any<WebSocketEvent.OnMessageReceived>().containingBytes(bytesMessage)
        )
        assertThat(testTextStreamObserver.values).containsExactly(textMessage)
        assertThat(testBytesStreamObserver.values).containsExactly(bytesMessage)
    }

    private fun givenConnectionIsEstablished() {
        createClientAndServer()
        serverLifecycleRegistry.onNext(LifecycleState.Started)
        clientLifecycleRegistry.onNext(LifecycleState.Started)
        blockUntilConnectionIsEstablish()
    }

    private fun createClientAndServer() {
        server = createServer()
        serverWebSocketEventObserver = server.observeWebSocketEvent().test()
        client = createClient()
        clientWebSocketEventObserver = client.observeWebSocketEvent().test()
    }

    private fun createServer(): Service {
        return Scarlet.Factory()
                .create(
                        Scarlet.Configuration(
                                protocol = MockWebServerWebSocket(
                                        mockWebServer,
                                        object : MockWebServerWebSocket.RequestFactory {
                                            override fun createCloseRequest(): OkHttpWebSocket.CloseRequest {
                                                return OkHttpWebSocket.CloseRequest(SERVER_SHUTDOWN_REASON)
                                            }
                                        }
                                ),
                                lifecycle = serverLifecycleRegistry
                        )
                )
                .create()
    }

    private fun createClient(): Service {
        val factory = Scarlet.Factory()
        val protocol = OkHttpWebSocket(
                createOkHttpClient(),
                object : OkHttpWebSocket.RequestFactory {
                    override fun createOpenRequest(): OkHttpWebSocket.OpenRequest {
                        return OkHttpWebSocket.OpenRequest(Request.Builder().url(serverUrlString).build())
                    }

                    override fun createCloseRequest(): OkHttpWebSocket.CloseRequest {
                        return OkHttpWebSocket.CloseRequest(CLIENT_SHUTDOWN_REASON)
                    }
                }
        )
        val configuration = Scarlet.Configuration(
                protocol = protocol,
                lifecycle = clientLifecycleRegistry
        )
        val scarlet = factory.create(configuration)
        return scarlet.create()
    }

    private fun createOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .writeTimeout(500, TimeUnit.MILLISECONDS)
            .readTimeout(500, TimeUnit.MILLISECONDS)
            .build()

    private fun blockUntilConnectionIsEstablish() {
        clientWebSocketEventObserver.awaitValues(
                any<WebSocketEvent.OnConnectionOpened>()
        )
        serverWebSocketEventObserver.awaitValues(
                any<WebSocketEvent.OnConnectionOpened>()
        )
    }

    private interface Service {
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

        fun ValueAssert<WebSocketEvent.OnMessageReceived>.containingText(expectedText: String) = assert {
            assertThat(message).isInstanceOf(Message.Text::class.java)
            val (text) = message as Message.Text
            assertThat(text).isEqualTo(expectedText)
        }

        fun ValueAssert<WebSocketEvent.OnMessageReceived>.containingBytes(expectedBytes: ByteArray) = assert {
            assertThat(message).isInstanceOf(Message.Bytes::class.java)
            val (bytes) = message as Message.Bytes
            assertThat(bytes).isEqualTo(expectedBytes)
        }

        fun ValueAssert<WebSocketEvent.OnConnectionClosing>.withClosingReason(
                expectedShutdownReason: ShutdownReason
        ) = assert {
            assertThat(shutdownReason).isEqualTo(expectedShutdownReason)
        }

        fun ValueAssert<WebSocketEvent.OnConnectionClosed>.withClosedReason(expectedShutdownReason: ShutdownReason) =
                assert {
                    assertThat(shutdownReason).isEqualTo(expectedShutdownReason)
                }
    }
}
