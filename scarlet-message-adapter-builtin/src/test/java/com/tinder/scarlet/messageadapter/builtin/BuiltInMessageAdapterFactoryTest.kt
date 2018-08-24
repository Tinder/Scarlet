/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.messageadapter.builtin

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.Test

internal class BuiltInMessageAdapterFactoryTest {

    private val builtInMessageAdapterFactory = BuiltInMessageAdapterFactory()

    @Test
    fun create_givenStringType_shouldReturnTextMessageAdapter() {
        // When
        val messageAdapter = builtInMessageAdapterFactory.create(String::class.java, emptyArray())

        // Then
        assertThat(messageAdapter).isInstanceOf(TextMessageAdapter::class.java)
    }

    @Test
    fun create_givenByteArrayType_shouldReturnByteArrayMessageAdapter() {
        // When
        val messageAdapter = builtInMessageAdapterFactory.create(ByteArray::class.java, emptyArray())

        // Then
        assertThat(messageAdapter).isInstanceOf(ByteArrayMessageAdapter::class.java)
    }

    @Test
    fun create_givenOtherTypes_shouldThrowIllegalArgumentException() {
        unsupportedTypeExamples
            .forEach {
                assertThatIllegalArgumentException()
                    .isThrownBy {
                        builtInMessageAdapterFactory.create(it, emptyArray())
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
