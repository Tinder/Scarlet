/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.internal

import com.nhaarman.mockito_kotlin.mock
import com.tinder.scarlet.internal.connection.Connection
import com.tinder.scarlet.internal.servicemethod.ServiceMethodExecutor
import com.tinder.scarlet.ws.Send
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
internal class ServiceFactoryTest {
    private val connectionFactory = mock<Connection.Factory>()
    private val serviceMethodExecutorFactory = mock<ServiceMethodExecutor.Factory>()
    private val serviceFactory = Service.Factory(
        connectionFactory,
        serviceMethodExecutorFactory
    )

    @Test
    fun create_givenAClass_shouldThrowIllegalArgumentException() {
        // Then
        assertThatIllegalArgumentException()
            .isThrownBy {
                serviceFactory.create(AClass::class.java)
            }
    }

    @Test
    fun create_givenAnInheritedInterface_shouldThrowIllegalArgumentException() {
        // Then
        assertThatIllegalArgumentException()
            .isThrownBy {
                serviceFactory.create(ChildInterface::class.java)
            }
    }

    companion object {
        class AClass {
            @Send
            fun send(@Suppress("UNUSED_PARAMETER") message: String) {
            }
        }

        interface ParentInterface

        interface ChildInterface : ParentInterface
    }
}
