/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.messageadapter.jackson

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.tinder.scarlet.Message
import com.tinder.scarlet.MessageAdapter
import com.tinder.scarlet.Stream
import com.tinder.scarlet.testutils.any
import com.tinder.scarlet.testutils.containingText
import com.tinder.scarlet.testutils.rule.OkHttpWebSocketConnection
import com.tinder.scarlet.testutils.test
import com.tinder.scarlet.websocket.WebSocketEvent
import com.tinder.scarlet.ws.Receive
import com.tinder.scarlet.ws.Send
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import java.lang.reflect.Type

internal class JacksonMessageAdapterTest {

    private val jacksonMessageAdapterFactory = JacksonMessageAdapter.Factory(createJackson())

    @get:Rule
    internal val connection = OkHttpWebSocketConnection.create<Service>(
        observeWebSocketEvent = { observeEvents() },
        serverConfiguration = OkHttpWebSocketConnection.Configuration(
            messageAdapterFactories = listOf(
                jacksonMessageAdapterFactory
            )
        ),
        clientConfiguration = OkHttpWebSocketConnection.Configuration(
            messageAdapterFactories = listOf(
                TextMessageAdapter.Factory(),
                jacksonMessageAdapterFactory
            )
        )
    )

    @Test
    fun sendAnInterface_shouldBeReceivedByTheServer() {
        // Given
        connection.open()
        val data = AnImplementation("value")
        val expectedString = """{"name":"value"}"""
        val serverAnImplementationObserver = connection.server.observeAnImplementation().test()

        // When
        val isSuccessful = connection.client.sendAnInterface(data)

        // Then
        assertThat(isSuccessful).isTrue()
        connection.serverWebSocketEventObserver.awaitValues(
            any<WebSocketEvent.OnConnectionOpened>(),
            any<WebSocketEvent.OnMessageReceived>().containingText(expectedString)
        )
        serverAnImplementationObserver.awaitCount(1)
        assertThat(serverAnImplementationObserver.values).containsExactly(data)
    }

    @Test
    fun sendAnImplementation_shouldBeReceivedByTheServer() {
        // Given
        connection.open()
        val data = AnImplementation("value")
        val expectedString = """{"name":"value"}"""
        val serverAnImplementationObserver = connection.server.observeAnImplementation().test()

        // When
        val isSuccessful = connection.client.sendAnImplementation(data)

        // Then
        assertThat(isSuccessful).isTrue()
        connection.serverWebSocketEventObserver.awaitValues(
            any<WebSocketEvent.OnConnectionOpened>(),
            any<WebSocketEvent.OnMessageReceived>().containingText(expectedString)
        )
        serverAnImplementationObserver.awaitCount(1)
        assertThat(serverAnImplementationObserver.values).containsExactly(data)
    }

    private fun createJackson(): ObjectMapper = ObjectMapper()
        .registerModule(KotlinModule())

    companion object {

        interface AnInterface {
            val name: String?
        }

        data class AnImplementation(override val name: String?) : AnInterface

        internal class TextMessageAdapter : MessageAdapter<String> {

            override fun fromMessage(message: Message): String = (message as Message.Text).value

            override fun toMessage(data: String): Message = Message.Text(data)

            class Factory : MessageAdapter.Factory {
                override fun create(type: Type, annotations: Array<Annotation>): MessageAdapter<*> =
                    when (type) {
                        String::class.java -> TextMessageAdapter()
                        else -> throw IllegalArgumentException("$type is not supported.")
                    }
            }
        }

        internal interface Service {
            @Receive
            fun observeEvents(): Stream<WebSocketEvent>

            @Send
            fun sendAnImplementation(impl: AnImplementation): Boolean

            @Receive
            fun observeAnImplementation(): Stream<AnImplementation>

            @Send
            fun sendAnInterface(impl: AnInterface): Boolean
        }
    }
}
