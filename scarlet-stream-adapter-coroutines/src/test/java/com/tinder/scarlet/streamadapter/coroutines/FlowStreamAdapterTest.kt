package com.tinder.scarlet.streamadapter.coroutines

import com.tinder.scarlet.Stream
import com.tinder.scarlet.testutils.any
import com.tinder.scarlet.testutils.containingBytes
import com.tinder.scarlet.testutils.containingText
import com.tinder.scarlet.testutils.rule.OkHttpWebSocketConnection
import com.tinder.scarlet.websocket.WebSocketEvent
import com.tinder.scarlet.ws.Receive
import com.tinder.scarlet.ws.Send
import com.tinder.streamadapter.coroutines.CoroutinesStreamAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test

class FlowStreamAdapterTest {

    @get:Rule
    internal val connection = OkHttpWebSocketConnection.create<Service>(
        observeWebSocketEvent = { observeEvents() },
        serverConfiguration = OkHttpWebSocketConnection.Configuration(
            streamAdapterFactories = listOf(CoroutinesStreamAdapterFactory())
        ),
        clientConfiguration = OkHttpWebSocketConnection.Configuration(
            streamAdapterFactories = listOf(CoroutinesStreamAdapterFactory())
        )
    )

    @Test
    fun send_givenConnectionIsEstablished_shouldBeReceivedByTheServer() = runBlockingTest {
        // Given
        connection.open()
        val textMessage1 = "Hello"
        val textMessage2 = "Hi!"
        val bytesMessage1 = "Yo".toByteArray()
        val bytesMessage2 = "Sup".toByteArray()
        val testTextFlow = connection.server.observeText()
        val testBytesFlow = connection.server.observeBytes()

        val receivedTextItems = mutableListOf<String>()
        val receivedByteItems = mutableListOf<ByteArray>()
        val job1 = launch {
            testTextFlow.collect {
                receivedTextItems.add(it)
            }

        }
        val job2 = launch {
            testBytesFlow.collect {
                receivedByteItems.add(it)
            }
        }

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

        job1.invokeOnCompletion {
            assertThat(receivedTextItems).isNotEmpty
            assertThat(receivedTextItems[0]).isEqualTo(textMessage1)
            assertThat(receivedTextItems[1]).isEqualTo(textMessage2)
        }

        job2.invokeOnCompletion {
            assertThat(receivedByteItems).isNotEmpty
            assertThat(receivedByteItems[0]).isEqualTo(bytesMessage1)
            assertThat(receivedByteItems[1]).isEqualTo(bytesMessage2)
        }

        job1.cancel()
        job2.cancel()
    }

    internal interface Service {
        @Receive
        fun observeEvents(): Stream<WebSocketEvent>

        @Receive
        fun observeText(): Flow<String>

        @Receive
        fun observeBytes(): Flow<ByteArray>

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