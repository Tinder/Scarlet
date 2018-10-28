/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.internal.statetransition

import com.nhaarman.mockito_kotlin.given
import com.nhaarman.mockito_kotlin.mock
import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class StateTransitionAdapterResolverTest {

    private val stateTransitionAdapterFactory1 = mock<StateTransitionAdapter.Factory>()
    private val stateTransitionAdapterFactory2 = mock<StateTransitionAdapter.Factory>()
    private val stateTransitionAdapterResolver = StateTransitionAdapterResolver(listOf(stateTransitionAdapterFactory1, stateTransitionAdapterFactory2))

    @Test
    fun resolve_givenTypeIsSupportedByTheFirstStateTransitionAdapterFactory_shouldReturnStateTransitionAdapter() {
        // Given
        val type = Array<Int>::class.java
        val stateTransitionAdapter = mock<StateTransitionAdapter<Any>>()
        given(stateTransitionAdapterFactory1.create(type, emptyArray())).willReturn(stateTransitionAdapter)

        // When
        val resolvedStateTransitionAdapter = stateTransitionAdapterResolver.resolve(type, emptyArray())

        // Then
        Assertions.assertThat(resolvedStateTransitionAdapter).isEqualTo(stateTransitionAdapter)
    }

    @Test
    fun resolve_givenTypeIsSupportedByTheSecondStateTransitionAdapterFactory_shouldReturnStateTransitionAdapter() {
        // Given
        val type = Array<Int>::class.java
        val stateTransitionAdapter = mock<StateTransitionAdapter<Any>>()
        given(stateTransitionAdapterFactory1.create(type, emptyArray())).willThrow(IllegalArgumentException("Unsupported type"))
        given(stateTransitionAdapterFactory2.create(type, emptyArray())).willReturn(stateTransitionAdapter)

        // When
        val resolvedStateTransitionAdapter = stateTransitionAdapterResolver.resolve(type, emptyArray())

        // Then
        Assertions.assertThat(resolvedStateTransitionAdapter).isEqualTo(stateTransitionAdapter)
    }

    @Test
    fun resolve_givenTypeHasBeenResolved_shouldReturnTheMemoizedStateTransitionAdapter() {
        // Given
        val type = Array<Int>::class.java
        given(stateTransitionAdapterFactory1.create(type, emptyArray())).willAnswer { mock<StateTransitionAdapter<Any>>() }
        val firstResolvedStateTransitionAdapter = stateTransitionAdapterResolver.resolve(type, emptyArray())

        // When
        val secondResolvedStateTransitionAdapter = stateTransitionAdapterResolver.resolve(type, emptyArray())

        // Then
        Assertions.assertThat(secondResolvedStateTransitionAdapter).isSameAs(firstResolvedStateTransitionAdapter)
    }

    @Test
    fun resolve_givenTypeIsNotSupportedByAnyStateTransitionAdapterFactory_shouldThrowIllegalStateException() {
        // Given
        val type = Array<Int>::class.java
        given(stateTransitionAdapterFactory1.create(type, emptyArray())).willThrow(IllegalArgumentException("Unsupported type"))
        given(stateTransitionAdapterFactory2.create(type, emptyArray())).willThrow(IllegalArgumentException("Unsupported type"))

        // Then
        Assertions.assertThatIllegalStateException()
            .isThrownBy {
                stateTransitionAdapterResolver.resolve(type, emptyArray())
            }
            .withMessageContaining("Cannot resolve state transition adapter")
    }
}