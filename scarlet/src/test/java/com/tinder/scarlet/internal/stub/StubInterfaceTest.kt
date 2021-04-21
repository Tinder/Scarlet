/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.internal.stub

import org.mockito.kotlin.mock
import org.mockito.kotlin.then
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.assertj.core.api.Assertions.assertThatIllegalStateException
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import java.lang.reflect.Method

@RunWith(MockitoJUnitRunner::class)
class StubInterfaceTest {

    private val callback = mock<StubInterface.Callback>()

    @Test
    fun invoke_givenSendMethod_shouldInvokeStubMethod() {
        // Given
        val stubInterface = givenStubInterface()

        // When
        stubInterface.invoke(SEND_METHOD, arrayOf(MESSAGE))

        // Then
        then(callback).should().send(SEND_STUB_METHOD, MESSAGE)
    }

    @Test
    fun invoke_givenSendMethod_givenZeroArgs_shouldThrowIllegalStateException() {
        // Given
        val stubInterface = givenStubInterface()

        // When
        assertThatIllegalArgumentException()
            .isThrownBy {
                stubInterface.invoke(SEND_METHOD, arrayOf())
            }
    }

    @Test
    fun invoke_givenSendMethod_givenMoreThanOneArgs_shouldThrowIllegalStateException() {
        // Given
        val stubInterface = givenStubInterface()

        // When
        assertThatIllegalArgumentException()
            .isThrownBy {
                stubInterface.invoke(SEND_METHOD, arrayOf(MESSAGE, MESSAGE))
            }
    }

    @Test
    fun invoke_givenReceiveMethod_shouldInvokeStubMethod() {
        // Given
        val stubInterface = givenStubInterface()

        // When
        stubInterface.invoke(RECEIVE_METHOD, arrayOf())

        // Then
        then(callback).should().receive(RECEIVE_STUB_METHOD)
    }

    @Test
    fun invoke_givenSendMethod_givenMoreThanZeroArgs_shouldThrowIllegalStateException() {
        // Given
        val stubInterface = givenStubInterface()

        // When
        assertThatIllegalArgumentException()
            .isThrownBy {
                stubInterface.invoke(RECEIVE_METHOD, arrayOf(MESSAGE))
            }
    }

    @Test
    fun invoke_givenUnknownStubMethod_shouldThrowIllegalStateException() {
        // Given
        val stubInterface = givenStubInterface()

        // Then
        assertThatIllegalStateException()
            .isThrownBy {
                stubInterface.invoke(METHOD_WITHOUT_KNOWN_STUB_METHOD, arrayOf())
            }
    }

    private fun givenStubInterface(): StubInterface {
        return StubInterface(
            mapOf(
                SEND_METHOD to SEND_STUB_METHOD,
                RECEIVE_METHOD to RECEIVE_STUB_METHOD
            ),
            callback
        )
    }

    private companion object {
        private val SEND_METHOD = mock<Method>()
        private val SEND_STUB_METHOD = mock<StubMethod.Send>()
        private val RECEIVE_METHOD = mock<Method>()
        private val RECEIVE_STUB_METHOD = mock<StubMethod.Receive>()
        private val METHOD_WITHOUT_KNOWN_STUB_METHOD = mock<Method>()

        private val MESSAGE = "message"
    }
}