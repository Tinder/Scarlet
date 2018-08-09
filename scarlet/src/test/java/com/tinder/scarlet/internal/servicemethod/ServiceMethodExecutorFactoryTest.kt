/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.internal.servicemethod

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.given
import com.nhaarman.mockito_kotlin.mock
import com.tinder.scarlet.Stream
import com.tinder.scarlet.internal.connection.Connection
import com.tinder.scarlet.internal.utils.RuntimePlatform
import com.tinder.scarlet.ws.Receive
import com.tinder.scarlet.ws.Send
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.mockito.quality.Strictness

@RunWith(Enclosed::class)
internal class ServiceMethodExecutorFactoryTest {

    @RunWith(Parameterized::class)
    class GivenInvalidServiceInterface(
        private val serviceInterface: Class<*>,
        private val partialErrorMessage: String
    ) {
        @Suppress("UNUSED")
        @get:Rule
        val mockitoRule: MockitoRule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS)

        private val platform = mock<RuntimePlatform>()
        private val sendServiceMethodFactory = mock<ServiceMethod.Send.Factory>()
        private val receiveServiceMethodFactory = mock<ServiceMethod.Receive.Factory>()
        private val serviceMethodExecutorFactory = ServiceMethodExecutor.Factory(
            platform, sendServiceMethodFactory, receiveServiceMethodFactory
        )

        @Test
        fun create_shouldThrowIllegalArgumentException() {
            // Given
            val connection = mock<Connection>()

            // Then
            assertThatIllegalArgumentException()
                .isThrownBy {
                    serviceMethodExecutorFactory.create(serviceInterface, connection)
                }
                .withMessageContaining(partialErrorMessage)
        }

        @Suppress("UNUSED")
        companion object {

            private interface NoServiceMethodAnnotation {
                fun call(param1: Int)
            }

            private interface MultipleServiceMethodAnnotations {
                @Receive
                @Send
                fun call(param1: Int)
            }

            private interface MultipleServiceMethodAnnotations2 {
                @Send
                @Receive
                fun call(param1: Int)
            }

            @Parameterized.Parameters
            @JvmStatic
            fun data() = listOf(
                param<NoServiceMethodAnnotation>(partialErrorMessage = "one service method annotation"),
                param<MultipleServiceMethodAnnotations>(partialErrorMessage = "one service method annotation"),
                param<MultipleServiceMethodAnnotations2>(partialErrorMessage = "one service method annotation")
            )

            private inline fun <reified T> param(partialErrorMessage: String) =
                arrayOf(T::class.java, partialErrorMessage)
        }
    }

    @RunWith(Parameterized::class)
    class GivenValidServiceInterface(
        private val serviceInterface: Class<*>,
        private val expectedNumberOfSendMethods: Int,
        private val expectedNumberOfReceiveMethods: Int
    ) {
        @Suppress("UNUSED")
        @get:Rule
        val mockitoRule: MockitoRule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS)

        private val platform = mock<RuntimePlatform>()
        private val sendServiceMethodFactory = mock<ServiceMethod.Send.Factory>()
        private val receiveServiceMethodFactory = mock<ServiceMethod.Receive.Factory>()
        private val serviceMethodExecutorFactory = ServiceMethodExecutor.Factory(
            platform, sendServiceMethodFactory, receiveServiceMethodFactory
        )

        @Test
        fun create_shouldCreateServiceMethodExecutor() {
            // Given
            val connection = mock<Connection>()
            given(sendServiceMethodFactory.create(eq(connection), any())).willReturn(mock())
            given(receiveServiceMethodFactory.create(eq(connection), any())).willReturn(mock())

            // When
            val serviceMethodExecutor = serviceMethodExecutorFactory.create(serviceInterface, connection)

            // Then
            val serviceMethods = serviceMethodExecutor.serviceMethods
            val numberOfSendMethods = serviceMethods.filterValues { it is ServiceMethod.Send }.size
            val numberOfReceiveMethods = serviceMethods.filterValues { it is ServiceMethod.Receive }.size
            assertThat(numberOfSendMethods).isEqualTo(expectedNumberOfSendMethods)
            assertThat(numberOfReceiveMethods).isEqualTo(expectedNumberOfReceiveMethods)
            val expectedNumberOfMethods = expectedNumberOfSendMethods + expectedNumberOfReceiveMethods
            assertThat(serviceMethods.size).isEqualTo(expectedNumberOfMethods)
        }

        @Suppress("UNUSED")
        companion object {
            private interface ServiceExample {
                @Send
                fun send(param1: Int)

                @Receive
                fun receive(): Stream<Int>
            }

            private interface ServiceWithNonServiceMethodAnnotation {
                @Send
                @Deprecated("This method is deprecated")
                fun send(param1: Int)

                @Receive
                fun receive(): Stream<Int>
            }

            private interface ServiceWithMultipleSendMethods {
                @Send
                fun sendInt(param1: Int)

                @Send
                fun sendString(param1: String)

                @Send
                fun sendBoolean(param1: Boolean)

                @Send
                fun sendArray(param1: Array<Any>)
            }

            private interface ServiceWithMultipleReceiveMethods {
                @Receive
                fun receiveInt(): Stream<Int>

                @Receive
                fun receiveString(): Stream<String>

                @Receive
                fun receiveBoolean(): Stream<Boolean>

                @Receive
                fun receiveArray(): Stream<Array<Any>>
            }

            @Parameterized.Parameters
            @JvmStatic
            fun data() = listOf(
                param<ServiceExample>(numberOfSendMethods = 1, numberOfReceiveMethods = 1),
                param<ServiceWithNonServiceMethodAnnotation>(numberOfSendMethods = 1, numberOfReceiveMethods = 1),
                param<ServiceWithMultipleSendMethods>(numberOfSendMethods = 4, numberOfReceiveMethods = 0),
                param<ServiceWithMultipleReceiveMethods>(numberOfSendMethods = 0, numberOfReceiveMethods = 4)
            )

            private inline fun <reified T> param(numberOfSendMethods: Int, numberOfReceiveMethods: Int) =
                arrayOf(T::class.java, numberOfSendMethods, numberOfReceiveMethods)
        }
    }
}
