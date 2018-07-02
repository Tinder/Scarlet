/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.streamadapter.builtin

import com.tinder.scarlet.Stream
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.Test

internal class BuiltInStreamAdapterFactoryTest {

    private val builtInStreamAdapterFactory = BuiltInStreamAdapterFactory()

    @Test
    fun create_givenStreamType_shouldReturnIdentityStreamAdapter() {

        // When
        val streamAdapter = builtInStreamAdapterFactory.create(Stream::class.java)

        // Then
        assertThat(streamAdapter).isInstanceOf(IdentityStreamAdapter::class.java)
    }

    @Test
    fun create_givenOtherTypes_shouldThrowIllegalArgumentException() {
        unsupportedTypeExamples
            .forEach {
                assertThatIllegalArgumentException()
                    .isThrownBy {
                        builtInStreamAdapterFactory.create(it)
                    }
            }
    }

    companion object {
        private val unsupportedTypeExamples = listOf(
            Unit::class.java,
            Boolean::class.java,
            Number::class.java,
            Array<Any>::class.java,
            Map::class.java,
            Collection::class.java
        )
    }
}
