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
import com.tinder.scarlet.WebSocket.Event
import com.tinder.scarlet.Scarlet
import com.tinder.scarlet.Stream
import com.tinder.scarlet.messageadapter.moshi.MoshiMessageAdapter.Factory
import com.tinder.scarlet.testutils.TestStreamObserver
import com.tinder.scarlet.testutils.containingBytes
import com.tinder.scarlet.testutils.containingText
import com.tinder.scarlet.testutils.test
import com.tinder.scarlet.testutils.any
import com.tinder.scarlet.websocket.mockwebserver.newWebSocketFactory
import com.tinder.scarlet.websocket.okhttp.newWebSocketFactory
import com.tinder.scarlet.ws.Receive
import com.tinder.scarlet.ws.Send
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import okio.ByteString
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import java.lang.reflect.Type
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

internal class MoshiMessageAdapterTest {

    @get:Rule
    private val mockWebServer = MockWebServer()
    private val serverUrlString by lazy { mockWebServer.url("/").toString() }

    private lateinit var server: Service
    private lateinit var serverEventObserver: TestStreamObserver<Event>

    private lateinit var client: Service
    private lateinit var clientEventObserver: TestStreamObserver<Event>

    @Test
    fun sendAnInterface_shouldBeReceivedByTheServer() {
        // Given
        givenConnectionIsEstablished()
        val data = AnImplementation("value")
        val expectedSerializedData = """{"name":"value"}"""
        val serverAnImplementationObserver = server.observeAnImplementation().test()

        // When
        val isSuccessful = client.sendAnInterface(data)

        // Then
        assertThat(isSuccessful).isTrue()
        serverEventObserver.awaitValues(
            any<Event.OnConnectionOpened<*>>(),
            any<Event.OnMessageReceived>().containingText(expectedSerializedData)
        )
        serverAnImplementationObserver.awaitValues(
            any<AnImplementation> { assertThat(this).isEqualTo(AnImplementation("value")) }
        )
    }

    @Test
    fun sendAnImplementation_shouldBeReceivedByTheServer() {
        // Given
        givenConnectionIsEstablished()
        val data = AnImplementation("value")
        val expectedSerializedData = """{"name":"value"}"""
        val serverAnImplementationObserver = server.observeAnImplementation().test()

        // When
        val isSuccessful = client.sendAnImplementation(data)

        // Then
        assertThat(isSuccessful).isTrue()
        serverEventObserver.awaitValues(
            any<Event.OnConnectionOpened<*>>(),
            any<Event.OnMessageReceived>().containingText(expectedSerializedData)
        )
        serverAnImplementationObserver.awaitValues(
            any<AnImplementation> { assertThat(this).isEqualTo(data) }
        )
    }

    @Test
    fun sendAnnotatedString_shouldBeReceivedByTheServer() {
        // Given
        givenConnectionIsEstablished()
        val data = "value"
        val expectedSerializedData = """"qualified!""""
        val expectedDeserializedSerializedData = "it worked!"
        val serverAnnotatedStringObserver = server.observeAnnotatedString().test()

        // When
        val isSuccessful = client.sendAnnotatedString(data)

        // Then
        assertThat(isSuccessful).isTrue()
        serverEventObserver.awaitValues(
            any<Event.OnConnectionOpened<*>>(),
            any<Event.OnMessageReceived>().containingText(expectedSerializedData)
        )
        serverAnnotatedStringObserver.awaitValues(
            any<String> { assertThat(this).isEqualTo(expectedDeserializedSerializedData) }
        )
    }

    @Test
    fun sendRawString_givenJsonIsMalformed_andFactoryIsLenient_shouldBeReceivedByTheServer() {
        // Given
        givenConnectionIsEstablished(Factory.Config(lenient = true))
        val malformedJson = """{"name":value}"""
        val serverAnImplementationObserver = server.observeAnImplementation().test()

        // When
        client.sendRawString(malformedJson)

        // Then
        serverEventObserver.awaitValues(
            any<Event.OnConnectionOpened<*>>(),
            any<Event.OnMessageReceived>().containingText(malformedJson)
        )
        serverAnImplementationObserver.awaitValues(
            any<AnImplementation> { assertThat(this).isEqualTo(AnImplementation("value")) }
        )
    }

    @Test
    fun sendRawString_givenJsonIsMalformed_andFactoryIsNotLenient_shouldNotBeReceivedByTheServer() {
        // Given
        givenConnectionIsEstablished()
        val malformedJson = """{"name":value}"""
        val serverAnImplementationObserver = server.observeAnImplementation().test()

        // When
        client.sendRawString(malformedJson)

        // Then
        serverEventObserver.awaitValues(
            any<Event.OnConnectionOpened<*>>(),
            any<Event.OnMessageReceived>().containingText(malformedJson)
        )
        serverAnImplementationObserver.awaitValues()
    }

    @Test
    fun sendRawString_givenJsonHasNullValues_andFactorySerializesNull_shouldBeReceivedByTheServer() {
        // Given
        givenConnectionIsEstablished(Factory.Config(lenient = true, serializeNull = true))
        val jsonWithNullValues = "{}"
        val serverAnImplementationObserver = server.observeAnImplementation().test()

        // When
        client.sendRawString(jsonWithNullValues)

        // Then
        serverEventObserver.awaitValues(
            any<Event.OnConnectionOpened<*>>(),
            any<Event.OnMessageReceived>().containingText(jsonWithNullValues)
        )
        serverAnImplementationObserver.awaitValues(
            any { assertThat(this).isEqualTo(AnImplementation(null)) }
        )
    }

    @Test
    fun sendRawString_givenJsonHasUnknownKeys_andFactoryFailsOnUnknown_shouldNotBeReceivedByTheServer() {
        // Given
        givenConnectionIsEstablished(Factory.Config(lenient = true, serializeNull = true, failOnUnknown = true))
        val jsonWithUnknownKeys = """{"taco":"delicious"}"""
        val serverAnImplementationObserver = server.observeAnImplementation().test()

        // When
        client.sendRawString(jsonWithUnknownKeys)

        // Then
        serverEventObserver.awaitValues(
            any<Event.OnConnectionOpened<*>>(),
            any<Event.OnMessageReceived>().containingText(jsonWithUnknownKeys)
        )
        serverAnImplementationObserver.awaitValues()
    }

    @Test
    fun sendRawBytes_givenUtf8EncodedJsonWithUtf8Bom_shouldSkipUtf8Bom() {
        // Given
        givenConnectionIsEstablished(Factory.Config())
        val jsonWithUtf8Bom = Buffer()
            .write(ByteString.decodeHex("EFBBBF"))
            .writeUtf8("""{"name":"value"}""")
            .readByteString()
            .toByteArray()
        val serverAnImplementationObserver = server.observeAnImplementation().test()

        // When
        client.sendRawBytes(jsonWithUtf8Bom)

        // Then
        serverEventObserver.awaitValues(
            any<Event.OnConnectionOpened<*>>(),
            any<Event.OnMessageReceived>().containingBytes(jsonWithUtf8Bom)
        )
        serverAnImplementationObserver.awaitValues(
            any<AnImplementation> { assertThat(this).isEqualTo(AnImplementation("value")) }
        )
    }

    @Test
    fun sendRawBytes_givenUtf16EncodedJsonWithUtf16Bom_shouldNotSkipUtf16Bom() {
        // Given
        givenConnectionIsEstablished(Factory.Config())
        val jsonWithUtf16Bom = Buffer()
            .write(ByteString.decodeHex("FEFF"))
            .writeString("""{"name":"value"}""", Charset.forName("UTF-16"))
            .readByteString()
            .toByteArray()
        val serverAnImplementationObserver = server.observeAnImplementation().test()

        // When
        client.sendRawBytes(jsonWithUtf16Bom)

        // Then
        serverEventObserver.awaitValues(
            any<Event.OnConnectionOpened<*>>(),
            any<Event.OnMessageReceived>().containingBytes(jsonWithUtf16Bom)
        )
        serverAnImplementationObserver.awaitValues()
    }

    private fun givenConnectionIsEstablished(config: Factory.Config = Factory.Config()) {
        createClientAndServer(config)
        blockUntilConnectionIsEstablish()
    }

    private fun createClientAndServer(config: Factory.Config) {
        val moshi = createMoshi()
        val factory = Factory(moshi, config)
        server = createServer(factory)
        serverEventObserver = server.observeEvents().test()
        client = createClient(factory)
        clientEventObserver = client.observeEvents().test()
    }

    private fun createMoshi(): Moshi = Moshi.Builder()
        .add(VerifyJsonQualifierJsonAdapterFactory())
        .add(Adapters())
        .add(KotlinJsonAdapterFactory())
        .build()

    private fun createServer(factory: Factory): Service = Scarlet.Builder()
        .webSocketFactory(mockWebServer.newWebSocketFactory())
        .addMessageAdapterFactory(factory)
        .build()
        .create()

    private fun createClient(factory: Factory): Service = Scarlet.Builder()
        .webSocketFactory(createOkHttpClient().newWebSocketFactory(serverUrlString))
        .addMessageAdapterFactory(factory)
        .build().create()

    private fun createOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .writeTimeout(500, TimeUnit.MILLISECONDS)
        .readTimeout(500, TimeUnit.MILLISECONDS)
        .build()

    private fun blockUntilConnectionIsEstablish() {
        serverEventObserver.awaitValues(
            any<Event.OnConnectionOpened<*>>()
        )
        clientEventObserver.awaitValues(
            any<Event.OnConnectionOpened<*>>()
        )
    }

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
            fun observeEvents(): Stream<Event>

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
