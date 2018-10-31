/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.messageadapter.gson

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import com.tinder.scarlet.Message
import com.tinder.scarlet.MessageAdapter
import com.tinder.scarlet.Stream
import com.tinder.scarlet.testutils.any
import com.tinder.scarlet.testutils.test
import com.tinder.scarlet.testutils.rule.OkHttpWebSocketConnection
import com.tinder.scarlet.testutils.containingText
import com.tinder.scarlet.StateTransition
import com.tinder.scarlet.websocket.WebSocketEvent
import com.tinder.scarlet.ws.Receive
import com.tinder.scarlet.ws.Send
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import java.lang.reflect.Type

internal class GsonMessageAdapterTest {

    private val gsonMessageAdapterFactory = GsonMessageAdapter.Factory(createGson())

    @get:Rule
    internal val connection = OkHttpWebSocketConnection.create<Service>(
        observeWebSocketEvent = { observeEvents() },
        serverConfiguration = OkHttpWebSocketConnection.Configuration(
            messageAdapterFactories = listOf(
                gsonMessageAdapterFactory
            )
        ),
        clientConfiguration = OkHttpWebSocketConnection.Configuration(
            messageAdapterFactories = listOf(
                TextMessageAdapter.Factory(),
                gsonMessageAdapterFactory
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
        assertThat(serverAnImplementationObserver.values).containsExactly(data)
    }

    @Test
    fun serializeUsesConfiguration() {
        // Given
        connection.open()
        val data = AnImplementation(null)
        val expectedString = "{}"
        val serverAnImplementationObserver = connection.server.observeAnImplementation().test()

        // When
        val isSuccessful = connection.client.sendAnImplementation(data)

        // Then
        assertThat(isSuccessful).isTrue()
        connection.serverWebSocketEventObserver.awaitValues(
            any<WebSocketEvent.OnConnectionOpened>(),
            any<WebSocketEvent.OnMessageReceived>().containingText(expectedString)
        )
        assertThat(serverAnImplementationObserver.values).containsExactly(data)
    }

    @Test
    fun deserializeUsesConfiguration() {
        // Given
        connection.open()
        val data = "{/* a comment! */}"
        val expectedString = "{/* a comment! */}"
        val serverAnImplementationObserver = connection.server.observeAnImplementation().test()

        // When
        val isSuccessful = connection.client.sendString(data)

        // Then
        assertThat(isSuccessful).isTrue()
        connection.serverWebSocketEventObserver.awaitValues(
            any<WebSocketEvent.OnConnectionOpened>(),
            any<WebSocketEvent.OnMessageReceived>().containingText(expectedString)
        )
        serverAnImplementationObserver.awaitValues(
            any<AnImplementation> { assertThat(this).isEqualTo(AnImplementation(null)) }
        )
    }

    private fun createGson(): Gson {
        return GsonBuilder()
            .registerTypeAdapter(AnInterface::class.java, AnInterfaceAdapter())
            .setLenient()
            .create()
    }

    companion object {

        interface AnInterface {
            val name: String?
        }

        data class AnImplementation(override val name: String?) : AnInterface

        internal class AnInterfaceAdapter : TypeAdapter<AnInterface>() {
            override fun write(jsonWriter: JsonWriter, anInterface: AnInterface) {
                jsonWriter.beginObject()
                jsonWriter.name("name").value(anInterface.name)
                jsonWriter.endObject()
            }

            override fun read(jsonReader: JsonReader): AnInterface {
                jsonReader.beginObject()

                var name: String? = null
                while (jsonReader.peek() !== JsonToken.END_OBJECT) {
                    when (jsonReader.nextName()) {
                        "name" -> name = jsonReader.nextString()
                    }
                }

                jsonReader.endObject()
                return AnImplementation(name)
            }
        }

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

            @Receive
            fun observeStateTransition(): Stream<StateTransition>

            @Send
            fun sendString(message: String): Boolean

            @Send
            fun sendAnImplementation(impl: AnImplementation): Boolean

            @Receive
            fun observeAnImplementation(): Stream<AnImplementation>

            @Send
            fun sendAnInterface(impl: AnInterface): Boolean
        }
    }
}
