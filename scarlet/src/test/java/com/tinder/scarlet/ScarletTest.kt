/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet

import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.given
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.then
import com.tinder.scarlet.internal.Service
import com.tinder.scarlet.internal.utils.RuntimePlatform
import com.tinder.scarlet.ws.Receive
import com.tinder.scarlet.ws.Send
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import java.util.stream.Stream

@RunWith(MockitoJUnitRunner::class)
internal class ScarletTest {

    private val platform = mock<RuntimePlatform>()
    private val serviceFactory = mock<Service.Factory>()
    private val scarlet = Scarlet(platform, serviceFactory)

    @Test
    fun create_givenRequestFactory_shouldCreatesService_andStartsService() {
        // Given
        val service = mock<Service>()
        given(serviceFactory.create(ExampleService::class.java)).willReturn(service)

        // When
        scarlet.create(ExampleService::class.java)

        // Then
        then(serviceFactory).should().create(ExampleService::class.java)
        then(service).should().startForever()
    }

    @Test
    fun reifiedCreate_givenRequestFactory_shouldCreatesService_andStartsService() {
        // Given
        val service = mock<Service>()
        given(serviceFactory.create(ExampleService::class.java)).willReturn(service)

        // When
        scarlet.create<ExampleService>()

        // Then
        then(serviceFactory).should().create(ExampleService::class.java)
        then(service).should().startForever()
    }

    @Test
    fun create_givenInvocation_shouldExecuteMethodOnService() {
        // Given
        val service = mock<Service>()
        given(serviceFactory.create(eq(ExampleService::class.java))).willReturn(service)
        val exampleService = scarlet.create<ExampleService>()

        // When
        exampleService.send(1)

        // Then
        then(service).should()
            .execute(ExampleService::class.java.getDeclaredMethod("send", Int::class.java), arrayOf(1))

        // When
        exampleService.receive()

        // Then
        then(service).should().execute(ExampleService::class.java.getDeclaredMethod("receive"), emptyArray())
    }

    @Test
    fun create_hashCode_shouldEqualServiceInstanceHashCode() {
        // Given
        val service = mock<Service>()
        given(serviceFactory.create(ExampleService::class.java)).willReturn(service)
        val exampleService = scarlet.create<ExampleService>()

        // When
        val hashCode = exampleService.hashCode()

        // Then
        assertThat(hashCode).isEqualTo(service.hashCode())
    }

    @Test
    fun create_toString_shouldProduceCorrectValue() {
        // Given
        val service = mock<Service>()
        given(serviceFactory.create(ExampleService::class.java)).willReturn(service)
        val exampleService = scarlet.create<ExampleService>()

        // When
        val toString = exampleService.toString()

        // Then
        assertThat(toString)
            .isEqualTo("Scarlet service implementation for com.tinder.scarlet.ScarletTest\$Companion\$ExampleService")
    }

    @Test
    fun create_equals_shouldEqualSelf() {
        // Given
        val service = mock<Service>()
        given(serviceFactory.create(ExampleService::class.java)).willReturn(service)
        val exampleService = scarlet.create<ExampleService>()

        // When
        val equalsSelf = exampleService.equals(exampleService)

        // Then
        assertThat(equalsSelf).describedAs("equals must be reflexive").isTrue()
    }

    @Test
    fun create_equals_shouldNotEqualOther() {
        // Given
        val service = mock<Service>()
        given(serviceFactory.create(ExampleService::class.java)).willReturn(service)
        val exampleService = scarlet.create<ExampleService>()
        val otherExampleService = scarlet.create<ExampleService>()

        // When
        val equalsOther = exampleService.equals(otherExampleService)

        // Then
        assertThat(equalsOther).describedAs("should not equal other instance").isFalse()
    }

    @Suppress("UNUSED")
    companion object {
        interface ExampleService {
            @Send
            fun send(param1: Int)

            @Receive
            fun receive(): Stream<Int>
        }
    }
}
