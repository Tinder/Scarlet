/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.internal.stub

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.then
import com.tinder.scarlet.Stream
import com.tinder.scarlet.internal.utils.RuntimePlatform
import com.tinder.scarlet.ws.Receive
import com.tinder.scarlet.ws.Send
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ProxyFactoryTest {

    private val platform = mock<RuntimePlatform>()
    private val proxyFactory = ProxyFactory(platform)

    @Test
    fun create_invoke_shouldInvokeStubInterface() {
        // Given
        val stubInterface = mock<StubInterface>()
        val service = proxyFactory.create(ExampleService::class.java, stubInterface)

        // When
        service.send(MESSAGE)

        // Then
        then(stubInterface).should().invoke(any(), eq(arrayOf<Any>(MESSAGE)))
    }

    @Test
    fun create_hashCode_shouldEqualServiceInstanceHashCode() {
        // Given
        val stubInterface = mock<StubInterface>()
        val service = proxyFactory.create(ExampleService::class.java, stubInterface)

        // When
        val hashCode = service.hashCode()

        // Then
        assertThat(hashCode).isEqualTo(service.hashCode())
    }

    @Test
    fun create_toString_shouldProduceCorrectValue() {
        // Given
        val stubInterface = mock<StubInterface>()
        val service = proxyFactory.create(ExampleService::class.java, stubInterface)

        // When
        val toString = service.toString()

        // Then
        assertThat(toString)
            .isEqualTo("Scarlet service implementation for com.tinder.scarlet.internal.stub.ProxyFactoryTest\$Companion\$ExampleService")
    }

    @Test
    fun create_equals_shouldEqualSelf() {
        // Given
        val stubInterface = mock<StubInterface>()
        val service = proxyFactory.create(ExampleService::class.java, stubInterface)

        // When
        val equalsSelf = service.equals(service)

        // Then
        assertThat(equalsSelf).describedAs("equals must be reflexive").isTrue()
    }

    @Test
    fun create_equals_shouldNotEqualOther() {
        // Given
        val stubInterface = mock<StubInterface>()
        val service = proxyFactory.create(ExampleService::class.java, stubInterface)
        val otherService = proxyFactory.create(ExampleService::class.java, stubInterface)

        // When
        val equalsOther = service.equals(otherService)

        // Then
        assertThat(equalsOther).describedAs("should not equal other instance").isFalse()
    }

    @Suppress("UNUSED")
    companion object {
        private interface ExampleService {
            @Send
            fun send(param1: Int)

            @Receive
            fun receive(): Stream<Int>
        }

        private const val MESSAGE = 132
    }
}