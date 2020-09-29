/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.streamadapter.rxjava2

import com.tinder.scarlet.Stream
import com.tinder.scarlet.testutils.any
import com.tinder.scarlet.testutils.containingBytes
import com.tinder.scarlet.testutils.containingText
import com.tinder.scarlet.testutils.rule.OkHttpWebSocketConnection
import com.tinder.scarlet.websocket.WebSocketEvent
import com.tinder.scarlet.ws.Receive
import com.tinder.scarlet.ws.Send
import io.reactivex.Observable
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test

class ObservableTest {

    @get:Rule
    internal val connection = OkHttpWebSocketConnection.create<FlowableTest.Service>(
        observeWebSocketEvent = { observeEvents() },
        serverConfiguration = OkHttpWebSocketConnection.Configuration(
            streamAdapterFactories = listOf(
                RxJava2StreamAdapterFactory()
            )
        ),
        clientConfiguration = OkHttpWebSocketConnection.Configuration(
            streamAdapterFactories = listOf(
                RxJava2StreamAdapterFactory()
            )
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
        val testTextSubscriber = connection.server.observeText().test()
        val testBytesSubscriber = connection.server.observeBytes().test()

        // When
        connection.client.sendText(textMessage1)
        val isSendTextSuccessful = connection.client.sendTextAndConfirm(textMessage2)
        connection.client.sendBytes(bytesMessage1)
        val isSendBytesSuccessful = connection.client.sendBytesAndConfirm(bytesMessage2)

        // Then
        assertThat(isSendTextSuccessful).isTrue()
        assertThat(isSendBytesSuccessful).isTrue()
        connection.serverWebSocketEventObserver.awaitValues(
            any<WebSocketEvent.OnConnectionOpened>(),
            any<WebSocketEvent.OnMessageReceived>().containingText(textMessage1),
            any<WebSocketEvent.OnMessageReceived>().containingText(textMessage2),
            any<WebSocketEvent.OnMessageReceived>().containingBytes(bytesMessage1),
            any<WebSocketEvent.OnMessageReceived>().containingBytes(bytesMessage2)
        )
        assertThat(testTextSubscriber.values()).containsExactly(textMessage1, textMessage2)
        assertThat(testBytesSubscriber.values()).containsExactly(bytesMessage1, bytesMessage2)
    }

    internal interface Service {
        @Receive
        fun observeEvents(): Stream<WebSocketEvent>

        @Receive
        fun observeText(): Observable<String>

        @Receive
        fun observeBytes(): Observable<ByteArray>

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
