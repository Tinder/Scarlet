package com.tinder.scarlet.internal.stub

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.anyArray
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.given
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.then
import com.tinder.scarlet.Deserialization
import com.tinder.scarlet.MessageAdapter
import com.tinder.scarlet.ProtocolEvent
import com.tinder.scarlet.Stream
import com.tinder.scarlet.StreamAdapter
import com.tinder.scarlet.internal.statetransition.DeserializationStateTransitionAdapter
import com.tinder.scarlet.internal.statetransition.DeserializedValueStateTransitionAdapter
import com.tinder.scarlet.internal.statetransition.NoOpStateTransitionAdapter
import com.tinder.scarlet.internal.statetransition.StateTransitionAdapter
import com.tinder.scarlet.internal.statetransition.StateTransitionAdapterResolver
import com.tinder.scarlet.internal.utils.MessageAdapterResolver
import com.tinder.scarlet.internal.utils.StreamAdapterResolver
import com.tinder.scarlet.utils.onlyMethod
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.mockito.quality.Strictness
import java.lang.reflect.Method

@RunWith(Enclosed::class)
internal class StubMethodFactoryTest {

    @RunWith(Enclosed::class)
    class GivenSend {
        @RunWith(Parameterized::class)
        class GivenInvalidMethod(
            private val method: Method,
            private val partialErrorMessage: String
        ) {
            @Suppress("UNUSED")
            @get:Rule
            val mockitoRule: MockitoRule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS)

            private val streamAdapterResolver = mock<StreamAdapterResolver>()
            private val messageAdapterResolver = mock<MessageAdapterResolver>()
            private val stateTransitionAdapterResolver = mock<StateTransitionAdapterResolver>()
            private val stubMethodFactory = StubMethod.Factory(
                streamAdapterResolver,
                messageAdapterResolver,
                stateTransitionAdapterResolver
            )

            @Test
            fun create_shouldThrowIllegalArgumentException() {
                // Given
                given(messageAdapterResolver.resolve(any(), eq(emptyArray()))).willReturn(mock())

                // Then
                Assertions.assertThatIllegalArgumentException()
                    .isThrownBy {
                        stubMethodFactory.create(method)
                    }
                    .withMessageContaining(partialErrorMessage)
            }

            @Suppress("UNUSED")
            companion object {
                interface EmptyParameters {
                    fun call()
                }

                interface MultipleParameters {
                    fun call(param1: String, param2: Boolean)
                }

                interface NonBooleanReturnType {
                    fun call(param1: String): String
                }

                interface NonBooleanReturnType2 {
                    fun call(param1: String): Int
                }

                @Parameterized.Parameters
                @JvmStatic
                fun data() = listOf(
                    param<EmptyParameters>(partialErrorMessage = "one parameter"),
                    param<MultipleParameters>(partialErrorMessage = "one parameter"),
                    param<NonBooleanReturnType>(partialErrorMessage = "Boolean or Void"),
                    param<NonBooleanReturnType2>(partialErrorMessage = "Boolean or Void")
                )

                private inline fun <reified T> param(partialErrorMessage: String) =
                    arrayOf(T::class.java.onlyMethod(), partialErrorMessage)
            }
        }

        @RunWith(Parameterized::class)
        class GivenValidMethod(
            private val method: Method
        ) {
            @Suppress("UNUSED")
            @get:Rule
            val mockitoRule: MockitoRule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS)

            private val streamAdapterResolver = mock<StreamAdapterResolver>()
            private val messageAdapterResolver = mock<MessageAdapterResolver>()
            private val stateTransitionAdapterResolver = mock<StateTransitionAdapterResolver>()
            private val stubMethodFactory = StubMethod.Factory(
                streamAdapterResolver,
                messageAdapterResolver,
                stateTransitionAdapterResolver
            )
            private val messageAdapter = mock<MessageAdapter<Any>>()

            @Test
            fun create_shouldCreateServiceMethod() {
                // Given
                given(messageAdapterResolver.resolve(any(), eq(emptyArray()))).willReturn(
                    messageAdapter
                )

                // When
                stubMethodFactory.create(method)

                // Then
                then(messageAdapterResolver).should().resolve(any(), eq(emptyArray()))
            }

            @Suppress("UNUSED")
            companion object {
                interface SendTextExample {
                    fun call(text: String): Boolean
                }

                interface SendByteArrayExample {
                    fun call(bytes: ByteArray): Boolean
                }

                interface SendTextReturnVoidExample {
                    fun call(text: String)
                }

                interface SendByteArrayReturnVoidExample {
                    fun call(bytes: ByteArray)
                }

                @Parameterized.Parameters
                @JvmStatic
                fun data() = listOf(
                    param<SendTextExample>(),
                    param<SendByteArrayExample>(),
                    param<SendTextReturnVoidExample>(),
                    param<SendByteArrayReturnVoidExample>()
                )

                private inline fun <reified T> param() = arrayOf(T::class.java.onlyMethod())
            }
        }
    }


    @RunWith(Enclosed::class)
    class GivenReceive {

        @RunWith(Parameterized::class)
        class GivenInvalidMethod(
            private val method: Method,
            private val partialErrorMessage: String
        ) {
            @Suppress("UNUSED")
            @get:Rule
            val mockitoRule: MockitoRule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS)

            private val streamAdapterResolver = mock<StreamAdapterResolver>()
            private val messageAdapterResolver = mock<MessageAdapterResolver>()
            private val stateTransitionAdapterResolver = mock<StateTransitionAdapterResolver>()
            private val stubMethodFactory = StubMethod.Factory(
                streamAdapterResolver,
                messageAdapterResolver,
                stateTransitionAdapterResolver
            )

            @Test
            fun create_shouldThrowIllegalArgumentException() {
                // Given
                val stateTransitionAdapter = mock<StateTransitionAdapter<Any>>()
                given(stateTransitionAdapterResolver.resolve(any(), anyArray())).willReturn(stateTransitionAdapter)
                val streamAdapter = mock<StreamAdapter<Any, Any>>()
                given(streamAdapterResolver.resolve(any())).willReturn(streamAdapter)

                // Then
                Assertions.assertThatIllegalArgumentException()
                    .isThrownBy {
                        stubMethodFactory.create(method)
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
            private val stateTransitionAdapterClass: Class<*>
        ) {
            @Suppress("UNUSED")
            @get:Rule
            val mockitoRule: MockitoRule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS)

            private val streamAdapterResolver = mock<StreamAdapterResolver>()
            private val messageAdapterResolver = mock<MessageAdapterResolver>()
            private val stateTransitionAdapterResolver = mock<StateTransitionAdapterResolver>()
            private val stubMethodFactory = StubMethod.Factory(
                streamAdapterResolver,
                messageAdapterResolver,
                stateTransitionAdapterResolver
            )

            @Test
            fun create_shouldCreateServiceMethod() {
                // Given
                val stateTransitionAdapter = mock<StateTransitionAdapter<Any>>()
                given(stateTransitionAdapterResolver.resolve(any(), anyArray())).willReturn(stateTransitionAdapter)
                val streamAdapter = mock<StreamAdapter<Any, Any>>()
                given(streamAdapterResolver.resolve(any())).willReturn(streamAdapter)

                // When
                val stubMethod = stubMethodFactory.create(method)

                // Then
                assertThat(stubMethod).isInstanceOf(StubMethod.Receive::class.java)
                val receiveStubMethod = stubMethod as StubMethod.Receive
                assertThat(receiveStubMethod.stateTransitionAdatper).isInstanceOf(stateTransitionAdapterClass)
                then(stateTransitionAdapterResolver).should().resolve(any(), anyArray())
                then(streamAdapterResolver).should().resolve(any())
            }

            @Suppress("UNUSED")
            companion object {
                interface EventExample {
                    fun call(): Stream<ProtocolEvent>
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
                    param<EventExample, NoOpStateTransitionAdapter>(),
                    param<StringDeserializationStreamExample, DeserializationStateTransitionAdapter>(),
                    param<ByteArrayDeserializationStreamExample, DeserializationStateTransitionAdapter>(),
                    param<StringStreamExample, DeserializedValueStateTransitionAdapter>(),
                    param<ByteArrayStreamExample, DeserializedValueStateTransitionAdapter>()
                )

                private inline fun <reified T, reified R> param() =
                    arrayOf(T::class.java.onlyMethod(), R::class.java)
            }
        }
    }

}
