/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.messageadapter.moshi

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonQualifier
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.KotlinJsonAdapterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.tinder.scarlet.Stream
import com.tinder.scarlet.testutils.any
import com.tinder.scarlet.testutils.test
import com.tinder.scarlet.testutils.v2.OkHttpWebSocketConnection
import com.tinder.scarlet.testutils.v2.containingBytes
import com.tinder.scarlet.testutils.v2.containingText
import com.tinder.scarlet.websocket.WebSocketEvent
import com.tinder.scarlet.ws.Receive
import com.tinder.scarlet.ws.Send
import okio.Buffer
import okio.ByteString
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import java.lang.reflect.Type
import java.nio.charset.Charset

internal class MoshiMessageAdapterTest {

    private val moshi = createMoshi()
    private var config = MoshiMessageAdapter.Factory.Config()
    private val factory = MoshiMessageAdapter.Factory(moshi, config)

    @get:Rule
    internal val connection = OkHttpWebSocketConnection.create<Service>(
        observeWebSocketEvent = { observeEvents() },
        serverConfiguration = OkHttpWebSocketConnection.Configuration(
            messageAdapterFactories = listOf(
                factory
            )
        ),
        clientConfiguration = OkHttpWebSocketConnection.Configuration(
            messageAdapterFactories = listOf(
                factory
            )
        )
    )

    @Test
    fun sendAnInterface_shouldBeReceivedByTheServer() {
        // Given
        connection.open()
        val data = AnImplementation("value")
        val expectedSerializedData = """{"name":"value"}"""
        val serverAnImplementationObserver = connection.server.observeAnImplementation().test()

        // When
        val isSuccessful = connection.client.sendAnInterface(data)

        // Then
        assertThat(isSuccessful).isTrue()
        connection.serverWebSocketEventObserver.awaitValues(
            any<WebSocketEvent.OnConnectionOpened>(),
            any<WebSocketEvent.OnMessageReceived>().containingText(expectedSerializedData)
        )
        serverAnImplementationObserver.awaitValues(
            any<AnImplementation> { assertThat(this).isEqualTo(AnImplementation("value")) }
        )
    }

    @Test
    fun sendAnImplementation_shouldBeReceivedByTheServer() {
        // Given
        connection.open()
        val data = AnImplementation("value")
        val expectedSerializedData = """{"name":"value"}"""
        val serverAnImplementationObserver = connection.server.observeAnImplementation().test()

        // When
        val isSuccessful = connection.client.sendAnImplementation(data)

        // Then
        assertThat(isSuccessful).isTrue()
        connection.serverWebSocketEventObserver.awaitValues(
            any<WebSocketEvent.OnConnectionOpened>(),
            any<WebSocketEvent.OnMessageReceived>().containingText(expectedSerializedData)
        )
        serverAnImplementationObserver.awaitValues(
            any<AnImplementation> { assertThat(this).isEqualTo(data) }
        )
    }

    @Test
    fun sendAnnotatedString_shouldBeReceivedByTheServer() {
        // Given
        connection.open()
        val data = "value"
        val expectedSerializedData = """"qualified!""""
        val expectedDeserializedSerializedData = "it worked!"
        val serverAnnotatedStringObserver = connection.server.observeAnnotatedString().test()

        // When
        val isSuccessful = connection.client.sendAnnotatedString(data)

        // Then
        assertThat(isSuccessful).isTrue()
        connection.serverWebSocketEventObserver.awaitValues(
            any<WebSocketEvent.OnConnectionOpened>(),
            any<WebSocketEvent.OnMessageReceived>().containingText(expectedSerializedData)
        )
        serverAnnotatedStringObserver.awaitValues(
            any<String> { assertThat(this).isEqualTo(expectedDeserializedSerializedData) }
        )
    }

    @Test
    fun sendRawString_givenJsonIsMalformed_andFactoryIsLenient_shouldBeReceivedByTheServer() {
        // Given
//        Factory.Config(lenient = true)
        connection.open()
        val malformedJson = """{"name":value}"""
        val serverAnImplementationObserver = connection.server.observeAnImplementation().test()

        // When
        connection.client.sendRawString(malformedJson)

        // Then
        connection.serverWebSocketEventObserver.awaitValues(
            any<WebSocketEvent.OnConnectionOpened>(),
            any<WebSocketEvent.OnMessageReceived>().containingText(malformedJson)
        )
        serverAnImplementationObserver.awaitValues(
            any<AnImplementation> { assertThat(this).isEqualTo(AnImplementation("value")) }
        )
    }

    @Test
    fun sendRawString_givenJsonIsMalformed_andFactoryIsNotLenient_shouldNotBeReceivedByTheServer() {
        // Given
        connection.open()
        val malformedJson = """{"name":value}"""
        val serverAnImplementationObserver = connection.server.observeAnImplementation().test()

        // When
        connection.client.sendRawString(malformedJson)

        // Then
        connection.serverWebSocketEventObserver.awaitValues(
            any<WebSocketEvent.OnConnectionOpened>(),
            any<WebSocketEvent.OnMessageReceived>().containingText(malformedJson)
        )
        serverAnImplementationObserver.awaitValues()
    }

    @Test
    fun sendRawString_givenJsonHasNullValues_andFactorySerializesNull_shouldBeReceivedByTheServer() {
        // Given
//        givenConnectionIsEstablished(Factory.Config(lenient = true, serializeNull = true))
        connection.open()
        val jsonWithNullValues = "{}"
        val serverAnImplementationObserver = connection.server.observeAnImplementation().test()

        // When
        connection.client.sendRawString(jsonWithNullValues)

        // Then
        connection.serverWebSocketEventObserver.awaitValues(
            any<WebSocketEvent.OnConnectionOpened>(),
            any<WebSocketEvent.OnMessageReceived>().containingText(jsonWithNullValues)
        )
        serverAnImplementationObserver.awaitValues(
            any { assertThat(this).isEqualTo(AnImplementation(null)) }
        )
    }

    @Test
    fun sendRawString_givenJsonHasUnknownKeys_andFactoryFailsOnUnknown_shouldNotBeReceivedByTheServer() {
        // Given
//        Factory.Config(
//            lenient = true,
//            serializeNull = true,
//            failOnUnknown = true
//        )

        connection.open()
        val jsonWithUnknownKeys = """{"taco":"delicious"}"""
        val serverAnImplementationObserver = connection.server.observeAnImplementation().test()

        // When
        connection.client.sendRawString(jsonWithUnknownKeys)

        // Then
        connection.serverWebSocketEventObserver.awaitValues(
            any<WebSocketEvent.OnConnectionOpened>(),
            any<WebSocketEvent.OnMessageReceived>().containingText(jsonWithUnknownKeys)
        )
        serverAnImplementationObserver.awaitValues()
    }

    @Test
    fun sendRawBytes_givenUtf8EncodedJsonWithUtf8Bom_shouldSkipUtf8Bom() {
        // Given
        connection.open()
        val jsonWithUtf8Bom = Buffer()
            .write(ByteString.decodeHex("EFBBBF"))
            .writeUtf8("""{"name":"value"}""")
            .readByteString()
            .toByteArray()
        val serverAnImplementationObserver = connection.server.observeAnImplementation().test()

        // When
        connection.client.sendRawBytes(jsonWithUtf8Bom)

        // Then
        connection.serverWebSocketEventObserver.awaitValues(
            any<WebSocketEvent.OnConnectionOpened>(),
            any<WebSocketEvent.OnMessageReceived>().containingBytes(jsonWithUtf8Bom)
        )
        serverAnImplementationObserver.awaitValues(
            any<AnImplementation> { assertThat(this).isEqualTo(AnImplementation("value")) }
        )
    }

    @Test
    fun sendRawBytes_givenUtf16EncodedJsonWithUtf16Bom_shouldNotSkipUtf16Bom() {
        // Given
        connection.open()
        val jsonWithUtf16Bom = Buffer()
            .write(ByteString.decodeHex("FEFF"))
            .writeString("""{"name":"value"}""", Charset.forName("UTF-16"))
            .readByteString()
            .toByteArray()
        val serverAnImplementationObserver = connection.server.observeAnImplementation().test()

        // When
        connection.client.sendRawBytes(jsonWithUtf16Bom)

        // Then
        connection.serverWebSocketEventObserver.awaitValues(
            any<WebSocketEvent.OnConnectionOpened>(),
            any<WebSocketEvent.OnMessageReceived>().containingBytes(jsonWithUtf16Bom)
        )
        serverAnImplementationObserver.awaitValues()
    }

    private fun createMoshi(): Moshi = Moshi.Builder()
        .add(VerifyJsonQualifierJsonAdapterFactory())
        .add(Adapters())
        .add(KotlinJsonAdapterFactory())
        .build()

    companion object {
        @Retention(AnnotationRetention.RUNTIME)
        @JsonQualifier
        annotation class SkipMoshi

        @Retention(AnnotationRetention.RUNTIME)
        @JsonQualifier
        annotation class Qualifier

        @Retention(AnnotationRetention.RUNTIME)
        annotation class NonQualifer

        interface AnInterface {
            val name: String?
        }

        data class AnImplementation(override val name: String?) : AnInterface

        class VerifyJsonQualifierJsonAdapterFactory : JsonAdapter.Factory {
            override fun create(
                type: Type,
                annotations: Set<Annotation>,
                moshi: Moshi
            ): JsonAdapter<*>? {
                for (annotation in annotations) {
                    assert(annotation.annotationClass.java.isAnnotationPresent(JsonQualifier::class.java)) {
                        "Non-@JsonQualifier annotation: $annotation"
                    }
                }
                return null
            }
        }

        @Suppress("UNUSED")
        class Adapters {
            @ToJson
            fun write(jsonWriter: JsonWriter, anInterface: AnInterface) {
                jsonWriter.beginObject()
                jsonWriter.name("name").value(anInterface.name)
                jsonWriter.endObject()
            }

            @FromJson
            fun read(jsonReader: JsonReader): AnInterface {
                jsonReader.beginObject()

                var name: String? = null
                while (jsonReader.hasNext()) {
                    when (jsonReader.nextName()) {
                        "name" -> name = jsonReader.nextString()
                    }
                }

                jsonReader.endObject()
                return AnImplementation(name!!)
            }

            @ToJson
            fun write(writer: JsonWriter, @Suppress("UNUSED_PARAMETER") @Qualifier value: String) {
                writer.value("qualified!")
            }

            @FromJson
            @Qualifier
            fun readQualified(reader: JsonReader): String {
                val string = reader.nextString()
                if (string == "qualified!") {
                    return "it worked!"
                }
                throw AssertionError("Found: $string")
            }
        }

        internal interface Service {
            @Receive
            fun observeEvents(): Stream<WebSocketEvent>

            @Send
            fun sendRawString(@SkipMoshi message: String): Boolean

            @Send
            fun sendRawBytes(@SkipMoshi message: ByteArray): Boolean

            @Send
            fun sendAnImplementation(impl: AnImplementation): Boolean

            @Receive
            fun observeAnImplementation(): Stream<AnImplementation>

            @Send
            fun sendAnInterface(impl: AnInterface): Boolean

            @Send
            fun sendAnnotatedString(@Qualifier @NonQualifer message: String): Boolean

            @Receive
            @Qualifier
            @NonQualifer
            fun observeAnnotatedString(): Stream<String>
        }
    }
}
