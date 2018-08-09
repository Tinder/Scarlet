/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.messageadapter.builtin

import com.tinder.scarlet.Message
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

internal class ByteArrayMessageAdapterTest {

    private val messageAdapter = ByteArrayMessageAdapter()

    @Test
    fun fromMessage() {
        // Given
        val bytes = "Goodbye".toByteArray()
        val bytesMessage = Message.Bytes(bytes)

        // When
        val adaptedData = messageAdapter.fromMessage(bytesMessage)

        // Then
        assertThat(adaptedData).isEqualTo(bytes)
    }

    @Test
    fun toMessage() {
        // Given
        val bytes = "Goodbye".toByteArray()

        // When
        val adaptedMessage = messageAdapter.toMessage(bytes)

        // Then
        assertThat(adaptedMessage).isInstanceOf(Message.Bytes::class.java)
        val (adaptedBytes) = (adaptedMessage as Message.Bytes)
        assertThat(adaptedBytes).isEqualTo(bytes)
    }
}
