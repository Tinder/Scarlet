/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.internal.servicemethod

import com.nhaarman.mockito_kotlin.given
import com.nhaarman.mockito_kotlin.mock
import com.tinder.scarlet.StreamAdapter
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalStateException
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
internal class StreamAdapterResolverTest {
    private val streamAdapterFactory1 = mock<StreamAdapter.Factory>()
    private val streamAdapterFactory2 = mock<StreamAdapter.Factory>()
    private val streamAdapterResolver = StreamAdapterResolver(listOf(streamAdapterFactory1, streamAdapterFactory2))

    @Test
    fun resolve_givenTypeIsSupportedByTheFirstStreamAdapterFactory_shouldReturnStreamAdapter() {
        // Given
        val type = Array<Int>::class.java
        val streamAdapter = mock<StreamAdapter<Any, Any>>()
        given(streamAdapterFactory1.create(type)).willReturn(streamAdapter)

        // When
        val resolvedStreamAdapter = streamAdapterResolver.resolve(type)

        // Then
        assertThat(resolvedStreamAdapter).isEqualTo(streamAdapter)
    }

    @Test
    fun resolve_givenTypeIsSupportedByTheSecondStreamAdapterFactory_shouldReturnStreamAdapter() {
        // Given
        val type = Array<Int>::class.java
        val streamAdapter = mock<StreamAdapter<Any, Any>>()
        given(streamAdapterFactory1.create(type)).willThrow(IllegalArgumentException("Unsupported type"))
        given(streamAdapterFactory2.create(type)).willReturn(streamAdapter)

        // When
        val resolvedStreamAdapter = streamAdapterResolver.resolve(type)

        // Then
        assertThat(resolvedStreamAdapter).isEqualTo(streamAdapter)
    }

    @Test
    fun resolve_givenTypeIsNotSupportedByAnyStreamAdapterFactory_shouldThrowIllegalStateException() {
        // Given
        val type = Array<Int>::class.java
        given(streamAdapterFactory1.create(type)).willThrow(IllegalArgumentException("Unsupported type"))
        given(streamAdapterFactory2.create(type)).willThrow(IllegalArgumentException("Unsupported type"))

        // Then
        assertThatIllegalStateException()
            .isThrownBy {
                streamAdapterResolver.resolve(type)
            }
    }
}
