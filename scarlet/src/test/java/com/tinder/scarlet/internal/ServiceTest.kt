/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.internal

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.then
import com.tinder.scarlet.internal.connection.Connection
import com.tinder.scarlet.internal.servicemethod.ServiceMethodExecutor
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import java.lang.reflect.Method

@RunWith(MockitoJUnitRunner::class)
internal class ServiceTest {

    private val connection = mock<Connection>()
    private val serviceMethodExecutor = mock<ServiceMethodExecutor>()
    private val service = Service(connection, serviceMethodExecutor)

    @Test
    fun startForever_shouldDelegateToConnection() {
        // When
        service.startForever()

        // Then
        then(connection).should().startForever()
    }

    @Test
    fun execute_shouldDelegateToServiceMethodExecutor() {
        // Given
        val method = mock<Method>()
        val args = arrayOf<Any>()

        // When
        service.execute(method, args)

        // Then
        then(serviceMethodExecutor).should().execute(method, args)
    }
}
