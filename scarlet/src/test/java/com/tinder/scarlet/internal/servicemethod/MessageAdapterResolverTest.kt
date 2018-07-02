/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.internal.servicemethod

import com.nhaarman.mockito_kotlin.given
import com.nhaarman.mockito_kotlin.mock
import com.tinder.scarlet.MessageAdapter
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalStateException
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
internal class MessageAdapterResolverTest {

    private val messageAdapterFactory1 = mock<MessageAdapter.Factory>()
    private val messageAdapterFactory2 = mock<MessageAdapter.Factory>()
    private val messageAdapterResolver = MessageAdapterResolver(listOf(messageAdapterFactory1, messageAdapterFactory2))

    @Test
    fun resolve_givenTypeIsSupportedByTheFirstMessageAdapterFactory_shouldReturnMessageAdapter() {
        // Given
        val type = Array<Int>::class.java
        val messageAdapter = mock<MessageAdapter<Any>>()
        given(messageAdapterFactory1.create(type, emptyArray())).willReturn(messageAdapter)

        // When
        val resolvedMessageAdapter = messageAdapterResolver.resolve(type, emptyArray())

        // Then
        assertThat(resolvedMessageAdapter).isEqualTo(messageAdapter)
    }

    @Test
    fun resolve_givenTypeIsSupportedByTheSecondMessageAdapterFactory_shouldReturnMessageAdapter() {
        // Given
        val type = Array<Int>::class.java
        val messageAdapter = mock<MessageAdapter<Any>>()
        given(messageAdapterFactory1.create(type, emptyArray())).willThrow(IllegalArgumentException("Unsupported type"))
        given(messageAdapterFactory2.create(type, emptyArray())).willReturn(messageAdapter)

        // When
        val resolvedMessageAdapter = messageAdapterResolver.resolve(type, emptyArray())

        // Then
        assertThat(resolvedMessageAdapter).isEqualTo(messageAdapter)
    }

    @Test
    fun resolve_givenTypeHasBeenResolved_shouldReturnTheMemoizedMessageAdapter() {
        // Given
        val type = Array<Int>::class.java
        given(messageAdapterFactory1.create(type, emptyArray())).willAnswer { mock<MessageAdapter<Any>>() }
        val firstResolvedMessageAdapter = messageAdapterResolver.resolve(type, emptyArray())

        // When
        val secondResolvedMessageAdapter = messageAdapterResolver.resolve(type, emptyArray())

        // Then
        assertThat(secondResolvedMessageAdapter).isSameAs(firstResolvedMessageAdapter)
    }

    @Test
    fun resolve_givenTypeIsNotSupportedByAnyMessageAdapterFactory_shouldThrowIllegalStateException() {
        // Given
        val type = Array<Int>::class.java
        given(messageAdapterFactory1.create(type, emptyArray())).willThrow(IllegalArgumentException("Unsupported type"))
        given(messageAdapterFactory2.create(type, emptyArray())).willThrow(IllegalArgumentException("Unsupported type"))

        // Then
        assertThatIllegalStateException()
            .isThrownBy {
                messageAdapterResolver.resolve(type, emptyArray())
            }
            .withMessageContaining("Cannot resolve message adapter")
    }
}
