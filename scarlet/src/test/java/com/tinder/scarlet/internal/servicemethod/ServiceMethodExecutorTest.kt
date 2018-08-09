/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.internal.servicemethod

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.then
import org.assertj.core.api.Assertions.assertThatIllegalStateException
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import java.lang.reflect.Method

@RunWith(MockitoJUnitRunner::class)
internal class ServiceMethodExecutorTest {

    private val sendMethod = mock<Method>()
    private val sendServiceMethod = mock<ServiceMethod.Send>()
    private val receiveMethod = mock<Method>()
    private val receiveServiceMethod = mock<ServiceMethod.Receive>()
    private val serviceMethodExecutor = ServiceMethodExecutor(
        mapOf(sendMethod to sendServiceMethod, receiveMethod to receiveServiceMethod)
    )
    private val notRegisteredMethod = mock<Method>()

    @Test
    fun execute_givenSendMethod_shouldExecuteSendServiceMethod() {
        // Given
        val argument = "a message"
        val arguments = arrayOf<Any>(argument)

        // When
        serviceMethodExecutor.execute(sendMethod, arguments)

        // Then
        then(sendServiceMethod).should().execute(argument)
    }

    @Test
    fun execute_givenReceiveMethod_shouldExecuteReceiveServiceMethod() {
        // Given
        val arguments = arrayOf<Any>()

        // When
        serviceMethodExecutor.execute(receiveMethod, arguments)

        // Then
        then(receiveServiceMethod).should().execute()
    }

    @Test
    fun execute_givenNotRegisteredMethod_shouldThrowIllegalStateException() {
        assertThatIllegalStateException()
            .isThrownBy {
                serviceMethodExecutor.execute(notRegisteredMethod, arrayOf())
            }
    }
}
