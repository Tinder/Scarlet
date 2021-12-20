package com.tinder.scarlet.streamadapter.coroutines

import com.google.common.truth.Correspondence
import com.google.common.truth.Truth.assertThat
import com.tinder.scarlet.Stream
import com.tinder.scarlet.WebSocket
import com.tinder.scarlet.testutils.TestStreamObserver
import com.tinder.scarlet.testutils.any
import com.tinder.scarlet.testutils.containingBytes
import com.tinder.scarlet.testutils.containingText
import com.tinder.scarlet.testutils.test
import com.tinder.scarlet.ws.Receive
import com.tinder.scarlet.ws.Send
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import java.util.Arrays

class FlowStreamAdapterTest {

    @get:Rule
    val clientServerModel = object : ClientServerModel<Service>(Service::class.java) {
        override fun observeWebSocketEvents(service: Service): TestStreamObserver<WebSocket.Event> {
            return service.observeEvents().test()
        }
    }

    @Test
    fun `adapt - given a stream of strings, provides a Flow interface bound to the stream`() =
        runBlocking {
            // Given
            val (client, server) = clientServerModel.givenConnectionIsEstablished()
            val textMessage1 = "Hello"
            val textMessage2 = "Hi!"
            val bytesMessage1 = "Yo".toByteArray()
            val bytesMessage2 = "Sup".toByteArray()
            val testTextChannel = server.observeText()
            val testBytesChannel = server.observeBytes()

            // When
            client.sendText(textMessage1)
            val isSendTextSuccessful = client.sendTextAndConfirm(textMessage2)
            client.sendBytes(bytesMessage1)
            val isSendBytesSuccessful = client.sendBytesAndConfirm(bytesMessage2)

            // Then
            assertThat(isSendBytesSuccessful).isTrue()
            assertThat(isSendTextSuccessful).isTrue()

            clientServerModel.awaitValues(
                any<WebSocket.Event.OnConnectionOpened<*>>(),
                any<WebSocket.Event.OnMessageReceived>().containingText(textMessage1),
                any<WebSocket.Event.OnMessageReceived>().containingText(textMessage2),
                any<WebSocket.Event.OnMessageReceived>().containingBytes(bytesMessage1),
                any<WebSocket.Event.OnMessageReceived>().containingBytes(bytesMessage2)
            )

            assertThat(testTextChannel.take(2).toList()).containsExactly(
                textMessage1,
                textMessage2
            ).inOrder()

            val bytes = testBytesChannel.take(2).toList()
            assertThat(bytes).comparingElementsUsing(BYTE_ARRAY_CORRESPONDENCE).containsExactly(
                bytesMessage1,
                bytesMessage2
            ).inOrder()
        }

    val BYTE_ARRAY_CORRESPONDENCE = Correspondence.from<ByteArray, ByteArray>({ actual, expected ->
        Arrays.compare(actual, expected) == 0
    }, "Compare using Arrays.equals")

    interface Service {
        @Receive
        fun observeEvents(): Stream<WebSocket.Event>

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
