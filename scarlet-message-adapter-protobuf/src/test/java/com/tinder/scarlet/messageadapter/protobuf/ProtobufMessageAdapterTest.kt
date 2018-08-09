/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.messageadapter.protobuf

import com.google.protobuf.ExtensionRegistry
import com.google.protobuf.InvalidProtocolBufferException
import com.tinder.scarlet.Deserialization
import com.tinder.scarlet.Scarlet
import com.tinder.scarlet.Stream
import com.tinder.scarlet.WebSocket.Event
import com.tinder.scarlet.testutils.TestStreamObserver
import com.tinder.scarlet.testutils.any
import com.tinder.scarlet.testutils.containingBytes
import com.tinder.scarlet.testutils.test
import com.tinder.scarlet.websocket.mockwebserver.newWebSocketFactory
import com.tinder.scarlet.websocket.okhttp.newWebSocketFactory
import com.tinder.scarlet.ws.Receive
import com.tinder.scarlet.ws.Send
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockWebServer
import okio.ByteString
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.TimeUnit

internal class ProtobufMessageAdapterTest {

    @get:Rule
    private val mockWebServer = MockWebServer()
    private val serverUrlString by lazy { mockWebServer.url("/").toString() }

    private lateinit var server: Service
    private lateinit var serverEventObserver: TestStreamObserver<Event>

    private lateinit var client: Service
    private lateinit var clientEventObserver: TestStreamObserver<Event>

    @Test
    fun serializeAndDeserialize() {
        // Given
        givenConnectionIsEstablished()
        val phone = PhoneProtos.Phone.newBuilder().setNumber("(519) 867-5309").build()
        val expectedSerializedPhone = ByteString.decodeBase64("Cg4oNTE5KSA4NjctNTMwOQ==")!!.toByteArray()
        val serverPhoneObserver = server.receivePhone().test()

        // When
        val isSuccessful = client.sendPhone(phone)

        // Then
        assertThat(isSuccessful).isTrue()
        serverEventObserver.awaitValues(
            any<Event.OnConnectionOpened<*>>(),
            any<Event.OnMessageReceived>().containingBytes(expectedSerializedPhone)
        )
        serverPhoneObserver.awaitValues(
            any<PhoneProtos.Phone> { assertThat(this).isEqualTo(phone) }
        )
    }

    @Test
    fun deserializeEmpty() {
        // Given
        givenConnectionIsEstablished()
        val emptySerializedPhone = ByteArray(0)
        val serverPhoneObserver = server.receivePhone().test()

        // When
        val isSuccessful = client.sendBytes(emptySerializedPhone)

        // Then
        assertThat(isSuccessful).isTrue()
        serverEventObserver.awaitValues(
            any<Event.OnConnectionOpened<*>>(),
            any<Event.OnMessageReceived>().containingBytes(emptySerializedPhone)
        )
        serverPhoneObserver.awaitValues(
            any<PhoneProtos.Phone> { assertThat(hasNumber()).isFalse() }
        )
    }

    @Test
    fun deserializeUsingRegistry() {
        // Given
        givenConnectionIsEstablished(withRegistry = true)
        val serializedPhone = ByteString.decodeBase64("Cg4oNTE5KSA4NjctNTMwORAB")!!.toByteArray()
        val serverPhoneObserver = server.receivePhone().test()

        // When
        val isSuccessful = client.sendBytes(serializedPhone)

        // Then
        assertThat(isSuccessful).isTrue()
        serverEventObserver.awaitValues(
            any<Event.OnConnectionOpened<*>>(),
            any<Event.OnMessageReceived>().containingBytes(serializedPhone)
        )
        serverPhoneObserver.awaitValues(
            any<PhoneProtos.Phone> {
                assertThat(number).isEqualTo("(519) 867-5309")
                assertThat(getExtension(PhoneProtos.voicemail)).isEqualTo(true)
            }
        )
    }

    @Test
    fun deserializeWrongClass() {
        // Given
        givenConnectionIsEstablished()
        val phone = PhoneProtos.Phone.newBuilder().setNumber("(519) 867-5309").build()
        val serializedPhone = ByteString.decodeBase64("Cg4oNTE5KSA4NjctNTMwOQ==")!!.toByteArray()
        val serverStringDeserializationObserver = server.receiveWrongClassDeserialization().test()

        // When
        val isSuccessful = client.sendPhone(phone)

        // Then
        assertThat(isSuccessful).isTrue()
        serverEventObserver.awaitValues(
            any<Event.OnConnectionOpened<*>>(),
            any<Event.OnMessageReceived>().containingBytes(serializedPhone)
        )
        serverStringDeserializationObserver.awaitValues(
            any<Deserialization.Error<String>>()
        )
    }

    @Test
    fun deserializeWrongValue() {
        // Given
        givenConnectionIsEstablished()
        val data = ByteString.decodeBase64("////")!!.toByteArray()
        val serverPhoneObserver = server.receivePhoneDeserialization().test()

        // When
        val isSuccessful = client.sendBytes(data)

        // Then
        assertThat(isSuccessful).isTrue()
        serverEventObserver.awaitValues(
            any<Event.OnConnectionOpened<*>>(),
            any<Event.OnMessageReceived>().containingBytes(data)
        )
        serverPhoneObserver.awaitValues(
            any<Deserialization.Error<PhoneProtos.Phone>> {
                assertThat(throwable.cause)
                    .isInstanceOf(InvalidProtocolBufferException::class.java)
                    .hasMessageContaining("input ended unexpectedly")
            }
        )
    }

    private fun givenConnectionIsEstablished(withRegistry: Boolean = false) {
        createClientAndServer(withRegistry)
        blockUntilConnectionIsEstablish()
    }

    private fun createClientAndServer(withRegistry: Boolean) {
        val factory = if (withRegistry) {
            val registry = ExtensionRegistry.newInstance()
            PhoneProtos.registerAllExtensions(registry)
            ProtobufMessageAdapter.Factory(registry)
        } else {
            ProtobufMessageAdapter.Factory()
        }
        server = createServer(factory)
        serverEventObserver = server.observeEvents().test()
        client = createClient(factory)
        clientEventObserver = client.observeEvents().test()
    }

    private fun createServer(factory: ProtobufMessageAdapter.Factory): Service = Scarlet.Builder()
        .webSocketFactory(mockWebServer.newWebSocketFactory())
        .addMessageAdapterFactory(factory)
        .build()
        .create()

    private fun createClient(factory: ProtobufMessageAdapter.Factory): Service = Scarlet.Builder()
        .webSocketFactory(createOkHttpClient().newWebSocketFactory(serverUrlString))
        .addMessageAdapterFactory(factory)
        .build()
        .create()

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

        internal interface Service {
            @Receive
            fun observeEvents(): Stream<Event>

            @Send
            fun sendBytes(byteArray: ByteArray): Boolean

            @Send
            fun sendPhone(phone: PhoneProtos.Phone): Boolean

            @Receive
            fun receivePhone(): Stream<PhoneProtos.Phone>

            @Receive
            fun receivePhoneDeserialization(): Stream<Deserialization<PhoneProtos.Phone>>

            @Receive
            fun receiveWrongClassDeserialization(): Stream<Deserialization<String>>
        }
    }
}
