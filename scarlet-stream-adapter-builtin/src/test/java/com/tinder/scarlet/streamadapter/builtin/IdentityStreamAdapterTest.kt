/*
 * © 2013 - 2018 Tinder, Inc., ALL RIGHTS RESERVED
 */

package com.tinder.scarlet.streamadapter.builtin

import com.nhaarman.mockito_kotlin.mock
import com.tinder.scarlet.Stream
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

internal class IdentityStreamAdapterTest {

    private val streamAdapter = IdentityStreamAdapter<String>()

    @Test
    fun adapt_shouldReturnTheSameStream() {
        // Given
        val stream = mock<Stream<String>>()

        // When
        val adaptedStream = streamAdapter.adapt(stream)

        // Then
        assertThat(adaptedStream).isSameAs(stream)
    }

}
