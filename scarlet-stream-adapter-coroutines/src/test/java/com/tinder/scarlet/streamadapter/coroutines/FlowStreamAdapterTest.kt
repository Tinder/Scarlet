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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
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

    private val testCoroutineDispatcher = TestCoroutineDispatcher()

    @Before
    fun before() {
        Dispatchers.setMain(testCoroutineDispatcher)
    }

    @After
    fun after() {
        testCoroutineDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun send_givenConnectionIsEstablished_shouldBeReceivedByTheServer() {
        // Given
        connection.open()
        val textMessage1 = "Hello"
        val textMessage2 = "Hi!"
        val bytesMessage1 = "Yo".toByteArray()
        val bytesMessage2 = "Sup".toByteArray()
        val testTextFlow = connection.server.observeText()
        val testBytesFlow = connection.server.observeBytes()

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

        testCoroutineDispatcher.runBlockingTest {
            val receivedTestTextList = testTextFlow.take(2).toList()
            assertThat(receivedTestTextList[0]).isEqualTo(textMessage1)
            assertThat(receivedTestTextList[1]).isEqualTo(textMessage2)
//
//            val receivedTestBytesList = testBytesFlow.take(2).toList()
//            assertThat(receivedTestBytesList[0]).isEqualTo(bytesMessage1)
//            assertThat(receivedTestBytesList[1]).isEqualTo(bytesMessage2)
        }
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