/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.internal.servicemethod

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.anyArray
import com.nhaarman.mockito_kotlin.given
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.then
import com.tinder.scarlet.Deserialization
import com.tinder.scarlet.Stream
import com.tinder.scarlet.StreamAdapter
import com.tinder.scarlet.WebSocket.Event
import com.tinder.scarlet.internal.connection.Connection
import com.tinder.scarlet.utils.onlyMethod
import io.reactivex.Scheduler
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.mockito.quality.Strictness
import java.lang.reflect.Method

@RunWith(Enclosed::class)
internal class ReceiveServiceMethodFactoryTest {

    @RunWith(Parameterized::class)
    class GivenInvalidMethod(
        private val method: Method,
        private val partialErrorMessage: String
    ) {
        @Suppress("UNUSED")
        @get:Rule
        val mockitoRule: MockitoRule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS)

        private val scheduler = mock<Scheduler>()
        private val eventMapperFactory = mock<EventMapper.Factory>()
        private val streamAdapterResolver = mock<StreamAdapterResolver>()
        private val receiveServiceMethodFactory = ServiceMethod.Receive.Factory(
            scheduler, eventMapperFactory, streamAdapterResolver
        )

        @Test
        fun create_shouldThrowIllegalArgumentException() {
            // Given
            val webSocketClient = mock<Connection>()
            val eventMapper = mock<EventMapper<Any>>()
            val streamAdapter = mock<StreamAdapter<Any, Any>>()
            given(eventMapperFactory.create(any(), anyArray())).willReturn(eventMapper)
            given(streamAdapterResolver.resolve(any())).willReturn(streamAdapter)

            // Then
            assertThatIllegalArgumentException()
                .isThrownBy {
                    receiveServiceMethodFactory.create(webSocketClient, method)
                }
                .withMessageContaining(partialErrorMessage)
        }

        @Suppress("UNUSED")
        companion object {
            interface NonEmptyParameters {
                fun call(param1: Int)
            }

            interface NonEmptyParameters2 {
                fun call(param1: String)
            }

            interface NonEmptyParameters3 {
                fun call(param1: String, param2: Boolean)
            }

            interface NonEmptyParameters4 {
                fun call(vararg param1: String)
            }

            interface InvalidReturnType {
                fun call(): Int
            }

            interface InvalidReturnType2 {
                fun call(): String
            }

            interface InvalidReturnType3 {
                fun call(): Array<String>
            }

            interface VoidReturnType {
                fun call()
            }

            interface UnresolvableReturnType {
                fun call(): Stream<*>
            }

            interface UnresolvableReturnType2 {
                fun <T> call(): Stream<T>
            }

            @Parameterized.Parameters
            @JvmStatic
            fun data() = listOf(
                param<NonEmptyParameters>(partialErrorMessage = "zero"),
                param<NonEmptyParameters2>(partialErrorMessage = "zero"),
                param<NonEmptyParameters3>(partialErrorMessage = "zero"),
                param<NonEmptyParameters4>(partialErrorMessage = "zero"),
                param<InvalidReturnType>(partialErrorMessage = "ParameterizedType"),
                param<InvalidReturnType2>(partialErrorMessage = "ParameterizedType"),
                param<InvalidReturnType3>(partialErrorMessage = "ParameterizedType"),
                param<VoidReturnType>(partialErrorMessage = "ParameterizedType"),
                param<UnresolvableReturnType>(partialErrorMessage = "wildcard"),
                param<UnresolvableReturnType2>(partialErrorMessage = "wildcard")
            )

            private inline fun <reified T> param(partialErrorMessage: String) =
                arrayOf(T::class.java.onlyMethod(), partialErrorMessage)
        }
    }

    @RunWith(Parameterized::class)
    class GivenValidMethod(
        private val method: Method,
        private val eventMapperClass: Class<*>
    ) {
        @Suppress("UNUSED")
        @get:Rule
        val mockitoRule: MockitoRule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS)

        private val scheduler = mock<Scheduler>()
        private val eventMapperFactory = mock<EventMapper.Factory>()
        private val streamAdapterResolver = mock<StreamAdapterResolver>()
        private val receiveServiceMethodFactory = ServiceMethod.Receive.Factory(
            scheduler, eventMapperFactory, streamAdapterResolver
        )

        @Test
        fun create_shouldCreateServiceMethod() {
            // Given
            val webSocketClient = mock<Connection>()
            val eventMapper = Mockito.mock(eventMapperClass) as EventMapper<*>
            val streamAdapter = mock<StreamAdapter<Any, Any>>()
            given(eventMapperFactory.create(any(), anyArray())).willReturn(eventMapper)
            given(streamAdapterResolver.resolve(any())).willReturn(streamAdapter)

            // When
            val receiveServiceMethod = receiveServiceMethodFactory.create(webSocketClient, method)

            // Then
            assertThat(receiveServiceMethod.eventMapper).isInstanceOf(eventMapperClass)
            then(eventMapperFactory).should().create(any(), anyArray())
            then(streamAdapterResolver).should().resolve(any())
        }

        @Suppress("UNUSED")
        companion object {
            interface EventExample {
                fun call(): Stream<Event>
            }

            interface StringDeserializationStreamExample {
                fun call(): Stream<Deserialization<String>>
            }

            interface ByteArrayDeserializationStreamExample {
                fun call(): Stream<Deserialization<ByteArray>>
            }

            interface StringStreamExample {
                fun call(): Stream<String>
            }

            interface ByteArrayStreamExample {
                fun call(): Stream<ByteArray>
            }

            @Parameterized.Parameters
            @JvmStatic
            fun data() = listOf(
                param<EventExample, EventMapper.NoOp>(),
                param<StringDeserializationStreamExample, EventMapper.ToDeserialization<*>>(),
                param<ByteArrayDeserializationStreamExample, EventMapper.ToDeserialization<*>>(),
                param<StringStreamExample, EventMapper.ToDeserializedValue<*>>(),
                param<ByteArrayStreamExample, EventMapper.ToDeserializedValue<*>>()
            )

            private inline fun <reified T, reified R> param() = arrayOf(T::class.java.onlyMethod(), R::class.java)
        }
    }
}
