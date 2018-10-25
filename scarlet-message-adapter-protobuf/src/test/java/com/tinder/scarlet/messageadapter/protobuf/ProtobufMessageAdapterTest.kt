/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.messageadapter.protobuf

import com.google.protobuf.ExtensionRegistry
import com.google.protobuf.InvalidProtocolBufferException
import com.tinder.scarlet.Deserialization
import com.tinder.scarlet.Stream
import com.tinder.scarlet.testutils.any
import com.tinder.scarlet.testutils.test
import com.tinder.scarlet.testutils.v2.OkHttpWebSocketConnection
import com.tinder.scarlet.testutils.v2.containingBytes
import com.tinder.scarlet.websocket.WebSocketEvent
import com.tinder.scarlet.ws.Receive
import com.tinder.scarlet.ws.Send
import okio.ByteString
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith

@RunWith(Enclosed::class)
internal class ProtobufMessageAdapterTest {

    class WithoutRegistry {

        @get:Rule
        internal val connection = OkHttpWebSocketConnection.create<Service>(
            observeWebSocketEvent = { observeEvents() },
            serverConfiguration = OkHttpWebSocketConnection.Configuration(
                messageAdapterFactories = listOf(
                    createProtobufMessageAdapterFactory(withRegistry = false)
                )
            ),
            clientConfiguration = OkHttpWebSocketConnection.Configuration(
                messageAdapterFactories = listOf(
                    createProtobufMessageAdapterFactory(withRegistry = false)
                )
            )
        )

        @Test
        fun serializeAndDeserialize() {
            // Given
            connection.open()
            val phone = PhoneProtos.Phone.newBuilder().setNumber("(519) 867-5309").build()
            val expectedSerializedPhone =
                ByteString.decodeBase64("Cg4oNTE5KSA4NjctNTMwOQ==")!!.toByteArray()
            val serverPhoneObserver = connection.server.receivePhone().test()

            // When
            val isSuccessful = connection.client.sendPhone(phone)

            // Then
            assertThat(isSuccessful).isTrue()
            connection.serverWebSocketEventObserver.awaitValues(
                any<WebSocketEvent.OnConnectionOpened>(),
                any<WebSocketEvent.OnMessageReceived>().containingBytes(expectedSerializedPhone)
            )
            serverPhoneObserver.awaitValues(
                any<PhoneProtos.Phone> { assertThat(this).isEqualTo(phone) }
            )
        }

        @Test
        fun deserializeEmpty() {
            // Given
            connection.open()
            val emptySerializedPhone = ByteArray(0)
            val serverPhoneObserver = connection.server.receivePhone().test()

            // When
            val isSuccessful = connection.client.sendBytes(emptySerializedPhone)

            // Then
            assertThat(isSuccessful).isTrue()
            connection.serverWebSocketEventObserver.awaitValues(
                any<WebSocketEvent.OnConnectionOpened>(),
                any<WebSocketEvent.OnMessageReceived>().containingBytes(emptySerializedPhone)
            )
            serverPhoneObserver.awaitValues(
                any<PhoneProtos.Phone> { assertThat(hasNumber()).isFalse() }
            )
        }

        @Test
        fun deserializeWrongClass() {
            // Given
            connection.open()
            val phone = PhoneProtos.Phone.newBuilder().setNumber("(519) 867-5309").build()
            val serializedPhone =
                ByteString.decodeBase64("Cg4oNTE5KSA4NjctNTMwOQ==")!!.toByteArray()
            val serverStringDeserializationObserver =
                connection.server.receiveWrongClassDeserialization().test()

            // When
            val isSuccessful = connection.client.sendPhone(phone)

            // Then
            assertThat(isSuccessful).isTrue()
            connection.serverWebSocketEventObserver.awaitValues(
                any<WebSocketEvent.OnConnectionOpened>(),
                any<WebSocketEvent.OnMessageReceived>().containingBytes(serializedPhone)
            )
            serverStringDeserializationObserver.awaitValues(
                any<Deserialization.Error<String>>()
            )
        }

        @Test
        fun deserializeWrongValue() {
            // Given
            connection.open()
            val data = ByteString.decodeBase64("////")!!.toByteArray()
            val serverPhoneObserver = connection.server.receivePhoneDeserialization().test()

            // When
            val isSuccessful = connection.client.sendBytes(data)

            // Then
            assertThat(isSuccessful).isTrue()
            connection.serverWebSocketEventObserver.awaitValues(
                any<WebSocketEvent.OnConnectionOpened>(),
                any<WebSocketEvent.OnMessageReceived>().containingBytes(data)
            )
            serverPhoneObserver.awaitValues(
                any<Deserialization.Error<PhoneProtos.Phone>> {
                    assertThat(throwable.cause)
                        .isInstanceOf(InvalidProtocolBufferException::class.java)
                        .hasMessageContaining("input ended unexpectedly")
                }
            )
        }
    }

    class WithRegistry {

        @get:Rule
        internal val connection = OkHttpWebSocketConnection.create<Service>(
            observeWebSocketEvent = { observeEvents() },
            serverConfiguration = OkHttpWebSocketConnection.Configuration(
                messageAdapterFactories = listOf(
                    createProtobufMessageAdapterFactory(withRegistry = true)
                )
            ),
            clientConfiguration = OkHttpWebSocketConnection.Configuration(
                messageAdapterFactories = listOf(
                    createProtobufMessageAdapterFactory(withRegistry = true)
                )
            )
        )

        @Test
        fun deserializeUsingRegistry() {
            // Given
            connection.open()
            val serializedPhone =
                ByteString.decodeBase64("Cg4oNTE5KSA4NjctNTMwORAB")!!.toByteArray()
            val serverPhoneObserver = connection.server.receivePhone().test()

            // When
            val isSuccessful = connection.client.sendBytes(serializedPhone)

            // Then
            assertThat(isSuccessful).isTrue()
            connection.serverWebSocketEventObserver.awaitValues(
                any<WebSocketEvent.OnConnectionOpened>(),
                any<WebSocketEvent.OnMessageReceived>().containingBytes(serializedPhone)
            )
            serverPhoneObserver.awaitValues(
                any<PhoneProtos.Phone> {
                    assertThat(number).isEqualTo("(519) 867-5309")
                    assertThat(getExtension(PhoneProtos.voicemail)).isEqualTo(true)
                }
            )
        }
    }
}

internal interface Service {
    @Receive
    fun observeEvents(): Stream<WebSocketEvent>

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

private fun createProtobufMessageAdapterFactory(
    withRegistry: Boolean
): ProtobufMessageAdapter.Factory {
    return if (withRegistry) {
        val registry = ExtensionRegistry.newInstance()
        PhoneProtos.registerAllExtensions(registry)
        ProtobufMessageAdapter.Factory(registry)
    } else {
        ProtobufMessageAdapter.Factory()
    }
}