/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.streamadapter.rxjava2

import com.tinder.scarlet.Lifecycle
import com.tinder.scarlet.Scarlet
import com.tinder.scarlet.Stream
import com.tinder.scarlet.WebSocket
import com.tinder.scarlet.lifecycle.LifecycleRegistry
import com.tinder.scarlet.testutils.TestStreamObserver
import com.tinder.scarlet.testutils.any
import com.tinder.scarlet.testutils.containingBytes
import com.tinder.scarlet.testutils.containingText
import com.tinder.scarlet.testutils.test
import com.tinder.scarlet.websocket.mockwebserver.newWebSocketFactory
import com.tinder.scarlet.websocket.okhttp.newWebSocketFactory
import com.tinder.scarlet.ws.Receive
import com.tinder.scarlet.ws.Send
import io.reactivex.Flowable
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.TimeUnit

internal class FlowableTest {

    @get:Rule
    private val mockWebServer = MockWebServer()
    private val serverUrlString by lazy { mockWebServer.url("/").toString() }

    private val serverLifecycleRegistry = LifecycleRegistry()
    private lateinit var server: Service
    private lateinit var serverEventObserver: TestStreamObserver<WebSocket.Event>

    private val clientLifecycleRegistry = LifecycleRegistry()
    private lateinit var client: Service
    private lateinit var clientEventObserver: TestStreamObserver<WebSocket.Event>

    @Test
    fun send_givenConnectionIsEstablished_shouldBeReceivedByTheServer() {
        // Given
        givenConnectionIsEstablished()
        val textMessage1 = "Hello"
        val textMessage2 = "Hi"
        val bytesMessage1 = "Yo".toByteArray()
        val bytesMessage2 = "Sup".toByteArray()
        val testTextSubscriber = server.observeText().test()
        val testBytesSubscriber = server.observeBytes().test()

        // When
        client.sendText(textMessage1)
        val isSendTextSuccessful = client.sendTextAndConfirm(textMessage2)
        client.sendBytes(bytesMessage1)
        val isSendBytesSuccessful = client.sendBytesAndConfirm(bytesMessage2)

        // Then
        assertThat(isSendTextSuccessful).isTrue()
        assertThat(isSendBytesSuccessful).isTrue()
        serverEventObserver.awaitValues(
            any<WebSocket.Event.OnConnectionOpened<*>>(),
            any<WebSocket.Event.OnMessageReceived>().containingText(textMessage1),
            any<WebSocket.Event.OnMessageReceived>().containingText(textMessage2),
            any<WebSocket.Event.OnMessageReceived>().containingBytes(bytesMessage1),
            any<WebSocket.Event.OnMessageReceived>().containingBytes(bytesMessage2)
        )
        assertThat(testTextSubscriber.values()).containsExactly(textMessage1, textMessage2)
        assertThat(testBytesSubscriber.values()).containsExactly(bytesMessage1, bytesMessage2)
    }

    private fun givenConnectionIsEstablished() {
        createClientAndServer()
        serverLifecycleRegistry.onNext(Lifecycle.State.Started)
        clientLifecycleRegistry.onNext(Lifecycle.State.Started)
        blockUntilConnectionIsEstablish()
    }

    private fun createClientAndServer() {
        server = createServer()
        serverEventObserver = server.observeEvents().test()
        client = createClient()
        clientEventObserver = client.observeEvents().test()
    }

    private fun createServer(): Service {
        val webSocketFactory = mockWebServer.newWebSocketFactory()
        val scarlet = Scarlet.Builder()
            .webSocketFactory(webSocketFactory)
            .lifecycle(serverLifecycleRegistry)
            .addStreamAdapterFactory(RxJava2StreamAdapterFactory())
            .build()
        return scarlet.create()
    }

    private fun createClient(): Service {
        val okHttpClient = OkHttpClient.Builder()
            .writeTimeout(500, TimeUnit.MILLISECONDS)
            .readTimeout(500, TimeUnit.MILLISECONDS)
            .build()
        val webSocketFactory = okHttpClient.newWebSocketFactory(serverUrlString)
        val scarlet = Scarlet.Builder()
            .webSocketFactory(webSocketFactory)
            .lifecycle(clientLifecycleRegistry)
            .addStreamAdapterFactory(RxJava2StreamAdapterFactory())
            .build()
        return scarlet.create()
    }

    private fun blockUntilConnectionIsEstablish() {
        clientEventObserver.awaitValues(
            any<WebSocket.Event.OnConnectionOpened<*>>()
        )
        serverEventObserver.awaitValues(
            any<WebSocket.Event.OnConnectionOpened<*>>()
        )
    }

    private interface Service {
        @Receive
        fun observeEvents(): Stream<WebSocket.Event>

        @Receive
        fun observeText(): Flowable<String>

        @Receive
        fun observeBytes(): Flowable<ByteArray>

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
