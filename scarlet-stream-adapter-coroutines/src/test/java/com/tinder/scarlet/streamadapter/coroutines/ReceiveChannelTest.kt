/*
 * © 2013 - 2018 Tinder, Inc., ALL RIGHTS RESERVED
 */

package com.tinder.scarlet.streamadapter.coroutines

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
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

class ReceiveChannelTest {

    @get:Rule val clientServerModel = object : ClientServerModel<Service>(Service::class.java) {
        override fun observeWebSocketEvents(service: Service): TestStreamObserver<WebSocket.Event> {
            return service.observeEvents().test()
        }
    }

    @Test
    fun send_givenConnectionIsEstablished_shouldBeReceivedByTheServer() {
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
        assertThat(isSendTextSuccessful).isTrue()
        assertThat(isSendBytesSuccessful).isTrue()

        clientServerModel.awaitValues(
            any<WebSocket.Event.OnConnectionOpened<*>>(),
            any<WebSocket.Event.OnMessageReceived>().containingText(textMessage1),
            any<WebSocket.Event.OnMessageReceived>().containingText(textMessage2),
            any<WebSocket.Event.OnMessageReceived>().containingBytes(bytesMessage1),
            any<WebSocket.Event.OnMessageReceived>().containingBytes(bytesMessage2)
        )

        runBlocking {
            assertThat(testTextChannel.receive()).isEqualTo(textMessage1)
            assertThat(testTextChannel.receive()).isEqualTo(textMessage2)

            assertThat(testBytesChannel.receive()).isEqualTo(bytesMessage1)
            assertThat(testBytesChannel.receive()).isEqualTo(bytesMessage2)
        }
    }

    interface Service {
        @Receive
        fun observeEvents(): Stream<WebSocket.Event>

        @Receive
        fun observeText(): ReceiveChannel<String>

        @Receive
        fun observeBytes(): ReceiveChannel<ByteArray>

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
