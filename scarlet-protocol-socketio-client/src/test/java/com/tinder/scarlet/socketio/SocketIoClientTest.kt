/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.socketio

import com.tinder.scarlet.ProtocolEvent
import com.tinder.scarlet.Stream
import com.tinder.scarlet.testutils.any
import com.tinder.scarlet.testutils.containingBytes2
import com.tinder.scarlet.testutils.containingText2
import com.tinder.scarlet.testutils.rule.SocketIoConnection
import com.tinder.scarlet.testutils.test
import com.tinder.scarlet.ws.Receive
import com.tinder.scarlet.ws.Send
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test

class SocketIoClientTest {

    @get:Rule
    internal val connection = SocketIoConnection.create<Service>(
        observeProtocolEvent = { observeProtocolEvent() },
        clientConfiguration = SocketIoConnection.Configuration(EVENT_NAME),
        serverConfiguration = SocketIoConnection.Configuration(EVENT_NAME)
    )

    @Test
    fun send_givenConnectionIsEstablished_shouldBeReceivedByTheServer() {
        // Given
        connection.open()
        val textMessage1 = "Hello"
        val textMessage2 = "Hi"
        val serverStringObserver = connection.server.observeText().test()

        // When
        connection.client.sendText(textMessage1)
        val isSendTextEnqueued = connection.client.sendTextAndConfirm(textMessage2)

        // Then
        assertThat(isSendTextEnqueued).isTrue()
        connection.serverProtocolEventObserver.awaitValues(
            any<ProtocolEvent.OnOpened>(),
            any<ProtocolEvent.OnMessageReceived>().containingText2(textMessage1),
            any<ProtocolEvent.OnMessageReceived>().containingText2(textMessage2)
        )
        serverStringObserver.awaitValues(
            any<String> { Assertions.assertThat(this).isEqualTo(textMessage1) },
            any<String> { Assertions.assertThat(this).isEqualTo(textMessage2) }
        )
    }

    @Test
    fun send_givenConnectionIsNotEstablished_shouldFail() {
        // Given
        val textMessage = "Hello"
        val serverStringObserver = connection.server.observeText().test()

        // When
        val isSendTextSuccessful = connection.client.sendTextAndConfirm(textMessage)

        // Then
        assertThat(isSendTextSuccessful).isFalse()
        serverStringObserver.awaitValues()
    }

    @Test
    fun givenConnectionIsEstablished_andServerCloses_shouldClose() {
        // Given
        connection.open()

        // When
        connection.serverClosure()

        // Then
        connection.clientProtocolEventObserver.awaitValues(
            any<ProtocolEvent.OnOpened>(),
            any<ProtocolEvent.OnClosed>()
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
        connection.clientProtocolEventObserver.awaitValues(
            any<ProtocolEvent.OnOpened>(),
            any<ProtocolEvent.OnMessageReceived>().containingText2(textMessage),
            any<ProtocolEvent.OnMessageReceived>().containingBytes2(bytesMessage)
        )
        assertThat(testTextStreamObserver.values).containsExactly(textMessage)
        assertThat(testBytesStreamObserver.values).containsExactly(bytesMessage)
    }

    companion object {

        private const val EVENT_NAME = "AnEvent"

        internal interface Service {
            @Receive
            fun observeProtocolEvent(): Stream<ProtocolEvent>

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
}