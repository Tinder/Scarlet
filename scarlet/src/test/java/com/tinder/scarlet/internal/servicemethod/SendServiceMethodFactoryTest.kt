/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.internal.servicemethod

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.given
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.then
import com.tinder.scarlet.MessageAdapter
import com.tinder.scarlet.internal.connection.Connection
import com.tinder.scarlet.utils.onlyMethod
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
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
internal class SendServiceMethodFactoryTest {

    @RunWith(Parameterized::class)
    class GivenInvalidMethod(
        private val method: Method,
        private val partialErrorMessage: String
    ) {
        @Suppress("UNUSED")
        @get:Rule
        val mockitoRule: MockitoRule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS)

        private val webSocketClient = mock<Connection>()
        private val messageAdapterResolver = mock<MessageAdapterResolver>()
        private val sendServiceMethodFactory = ServiceMethod.Send.Factory(messageAdapterResolver)

        @Test
        fun create_shouldThrowIllegalArgumentException() {
            // Given
            given(messageAdapterResolver.resolve(any(), eq(emptyArray()))).willReturn(mock())

            // Then
            assertThatIllegalArgumentException()
                .isThrownBy {
                    sendServiceMethodFactory.create(webSocketClient, method)
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

        private val webSocketClient = mock<Connection>()
        private val messageAdapterResolver = mock<MessageAdapterResolver>()
        private val sendServiceMethodFactory = ServiceMethod.Send.Factory(messageAdapterResolver)

        private val messageAdapter = mock<MessageAdapter<Any>>()

        @Test
        fun create_shouldCreateServiceMethod() {
            // Given
            given(messageAdapterResolver.resolve(any(), eq(emptyArray()))).willReturn(messageAdapter)

            // When
            sendServiceMethodFactory.create(webSocketClient, method)

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
