package com.tinder.scarlet.internal.stub

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.anyArray
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.given
import com.nhaarman.mockito_kotlin.mock
import com.tinder.scarlet.MessageAdapter
import com.tinder.scarlet.Stream
import com.tinder.scarlet.StreamAdapter
import com.tinder.scarlet.internal.statetransition.StateTransitionAdapter
import com.tinder.scarlet.internal.statetransition.StateTransitionAdapterResolver
import com.tinder.scarlet.internal.utils.MessageAdapterResolver
import com.tinder.scarlet.internal.utils.RuntimePlatform
import com.tinder.scarlet.internal.utils.StreamAdapterResolver
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
internal class StubInterfaceFactoryTest {

    @RunWith(Parameterized::class)
    class GivenInvalidServiceInterface(
        private val serviceInterface: Class<*>,
        private val partialErrorMessage: String
    ) {
        @Suppress("UNUSED")
        @get:Rule
        val mockitoRule: MockitoRule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS)

        private val platform = mock<RuntimePlatform>()
        private val callback = mock<StubInterface.Callback>()
        private val stubMethodFactory = mock<StubMethod.Factory>()
        private val stubInterfaceFactory = StubInterface.Factory(
            platform, callback, stubMethodFactory
        )

        @Test
        fun create_shouldThrowIllegalArgumentException() {
            // Then
            assertThatIllegalArgumentException()
                .isThrownBy {
                    stubInterfaceFactory.create(serviceInterface)
                }
                .withMessageContaining(partialErrorMessage)
        }

        @Suppress("UNUSED")
        companion object {

            class AClass {
                @Send
                fun send(@Suppress("UNUSED_PARAMETER") message: String) {
                }
            }

            interface ParentInterface

            interface ChildInterface : ParentInterface

            @Parameterized.Parameters
            @JvmStatic
            fun data() = listOf(
                param<AClass>(partialErrorMessage = "Service declarations must be interfaces"),
                param<ChildInterface>(partialErrorMessage = "Service interfaces must not extend other interfaces")
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
        private val platform = mock<RuntimePlatform>()
        private val callback = mock<StubInterface.Callback>()
        private val streamAdapterResolver = mock<StreamAdapterResolver>()
        private val messageAdapterResolver = mock<MessageAdapterResolver>()
        private val stateTransitionAdapterResolver = mock<StateTransitionAdapterResolver>()
        private val stubMethodFactory = StubMethod.Factory(
            streamAdapterResolver,
            messageAdapterResolver,
            stateTransitionAdapterResolver
        )
        private val stubInterfaceFactory = StubInterface.Factory(
            platform, callback, stubMethodFactory
        )

        @Test
        fun create_shouldCreateServiceMethodExecutor() {
            // Given
            val stateTransitionAdapter = mock<StateTransitionAdapter<Any>>()
            given(stateTransitionAdapterResolver.resolve(any(), anyArray())).willReturn(
                stateTransitionAdapter
            )
            val streamAdapter = mock<StreamAdapter<Any, Any>>()
            given(streamAdapterResolver.resolve(any())).willReturn(streamAdapter)
            val messageAdapter = mock<MessageAdapter<Any>>()
            given(
                messageAdapterResolver.resolve(
                    any(),
                    eq(emptyArray())
                )
            ).willReturn(messageAdapter)

            // When
            val stubInterface = stubInterfaceFactory.create(serviceInterface)

            // Then
            val serviceMethods = stubInterface.stubMethods
            val numberOfSendMethods = serviceMethods.filterValues { it is StubMethod.Send }.size
            val numberOfReceiveMethods =
                serviceMethods.filterValues { it is StubMethod.Receive }.size
            assertThat(numberOfSendMethods).isEqualTo(expectedNumberOfSendMethods)
            assertThat(numberOfReceiveMethods).isEqualTo(expectedNumberOfReceiveMethods)
            val expectedNumberOfMethods =
                expectedNumberOfSendMethods + expectedNumberOfReceiveMethods
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
                param<ServiceWithNonServiceMethodAnnotation>(
                    numberOfSendMethods = 1,
                    numberOfReceiveMethods = 1
                ),
                param<ServiceWithMultipleSendMethods>(
                    numberOfSendMethods = 4,
                    numberOfReceiveMethods = 0
                ),
                param<ServiceWithMultipleReceiveMethods>(
                    numberOfSendMethods = 0,
                    numberOfReceiveMethods = 4
                )
            )

            private inline fun <reified T> param(
                numberOfSendMethods: Int,
                numberOfReceiveMethods: Int
            ) =
                arrayOf(T::class.java, numberOfSendMethods, numberOfReceiveMethods)
        }
    }
}
