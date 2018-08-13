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
import com.tinder.scarlet.Lifecycle
import com.tinder.scarlet.Message
import com.tinder.scarlet.MessageAdapter
import com.tinder.scarlet.Scarlet
import com.tinder.scarlet.Stream
import com.tinder.scarlet.WebSocket.Event
import com.tinder.scarlet.lifecycle.LifecycleRegistry
import com.tinder.scarlet.testutils.TestStreamObserver
import com.tinder.scarlet.testutils.any
import com.tinder.scarlet.testutils.containingText
import com.tinder.scarlet.testutils.test
import com.tinder.scarlet.websocket.mockwebserver.newWebSocketFactory
import com.tinder.scarlet.websocket.okhttp.newWebSocketFactory
import com.tinder.scarlet.ws.Receive
import com.tinder.scarlet.ws.Send
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import java.lang.reflect.Type
import java.util.concurrent.TimeUnit

internal class GsonMessageAdapterTest {

    @get:Rule
    private val mockWebServer = MockWebServer()
    private val serverUrlString by lazy { mockWebServer.url("/").toString() }

    private val serverLifecycleRegistry = LifecycleRegistry()
    private lateinit var server: Service
    private lateinit var serverEventObserver: TestStreamObserver<Event>

    private val clientLifecycleRegistry = LifecycleRegistry()
    private lateinit var client: Service
    private lateinit var clientEventObserver: TestStreamObserver<Event>

    @Test
    fun sendAnInterface_shouldBeReceivedByTheServer() {
        // Given
        givenConnectionIsEstablished()
        val data = AnImplementation("value")
        val expectedString = """{"name":"value"}"""
        val serverAnImplementationObserver = server.observeAnImplementation().test()

        // When
        val isSuccessful = client.sendAnInterface(data)

        // Then
        assertThat(isSuccessful).isTrue()
        serverEventObserver.awaitValues(
            any<Event.OnConnectionOpened<*>>(),
            any<Event.OnMessageReceived>().containingText(expectedString)
        )
        assertThat(serverAnImplementationObserver.values).containsExactly(data)
    }

    @Test
    fun sendAnImplementation_shouldBeReceivedByTheServer() {
        // Given
        givenConnectionIsEstablished()
        val data = AnImplementation("value")
        val expectedString = """{"name":"value"}"""
        val serverAnImplementationObserver = server.observeAnImplementation().test()

        // When
        val isSuccessful = client.sendAnImplementation(data)

        // Then
        assertThat(isSuccessful).isTrue()
        serverEventObserver.awaitValues(
            any<Event.OnConnectionOpened<*>>(),
            any<Event.OnMessageReceived>().containingText(expectedString)
        )
        assertThat(serverAnImplementationObserver.values).containsExactly(data)
    }

    @Test
    fun serializeUsesConfiguration() {
        // Given
        givenConnectionIsEstablished()
        val data = AnImplementation(null)
        val expectedString = "{}"
        val serverAnImplementationObserver = server.observeAnImplementation().test()

        // When
        val isSuccessful = client.sendAnImplementation(data)

        // Then
        assertThat(isSuccessful).isTrue()
        serverEventObserver.awaitValues(
            any<Event.OnConnectionOpened<*>>(),
            any<Event.OnMessageReceived>().containingText(expectedString)
        )
        assertThat(serverAnImplementationObserver.values).containsExactly(data)
    }

    @Test
    fun deserializeUsesConfiguration() {
        // Given
        givenConnectionIsEstablished()
        val data = "{/* a comment! */}"
        val expectedString = "{/* a comment! */}"
        val serverAnImplementationObserver = server.observeAnImplementation().test()

        // When
        val isSuccessful = client.sendString(data)

        // Then
        assertThat(isSuccessful).isTrue()
        serverEventObserver.awaitValues(
            any<Event.OnConnectionOpened<*>>(),
            any<Event.OnMessageReceived>().containingText(expectedString)
        )
        serverAnImplementationObserver.awaitValues(
            any<AnImplementation> { assertThat(this).isEqualTo(AnImplementation(null)) }
        )
    }

    private fun givenConnectionIsEstablished() {
        createClientAndServer()
        serverLifecycleRegistry.onNext(Lifecycle.State.Started)
        clientLifecycleRegistry.onNext(Lifecycle.State.Started)
        blockUntilConnectionIsEstablish()
    }

    private fun createClientAndServer() {
        val factory = GsonMessageAdapter.Factory(createGson())
        server = createServer(factory)
        serverEventObserver = server.observeEvents().test()
        client = createClient(factory)
        clientEventObserver = client.observeEvents().test()
    }

    private fun createGson(): Gson = GsonBuilder()
        .registerTypeAdapter(AnInterface::class.java, AnInterfaceAdapter())
        .setLenient()
        .create()

    private fun createServer(factory: GsonMessageAdapter.Factory): Service {
        val webSocketFactory = mockWebServer.newWebSocketFactory()
        val scarlet = Scarlet.Builder()
            .webSocketFactory(webSocketFactory)
            .addMessageAdapterFactory(factory)
            .lifecycle(serverLifecycleRegistry)
            .build()
        return scarlet.create()
    }

    private fun createClient(factory: GsonMessageAdapter.Factory): Service {
        val okHttpClient = OkHttpClient.Builder()
            .writeTimeout(500, TimeUnit.MILLISECONDS)
            .readTimeout(500, TimeUnit.MILLISECONDS)
            .build()
        val webSocketFactory = okHttpClient.newWebSocketFactory(serverUrlString)
        val scarlet = Scarlet.Builder()
            .webSocketFactory(webSocketFactory)
            .addMessageAdapterFactory(TextMessageAdapter.Factory())
            .addMessageAdapterFactory(factory)
            .lifecycle(clientLifecycleRegistry)
            .build()
        return scarlet.create()
    }

    private fun blockUntilConnectionIsEstablish() {
        serverEventObserver.awaitValues(
            any<Event.OnConnectionOpened<*>>()
        )
        clientEventObserver.awaitValues(
            any<Event.OnConnectionOpened<*>>()
        )
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
                override fun create(type: Type, annotations: Array<Annotation>): MessageAdapter<*> = when (type) {
                    String::class.java -> TextMessageAdapter()
                    else -> throw IllegalArgumentException("$type is not supported.")
                }
            }
        }

        internal interface Service {
            @Receive
            fun observeEvents(): Stream<Event>

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
