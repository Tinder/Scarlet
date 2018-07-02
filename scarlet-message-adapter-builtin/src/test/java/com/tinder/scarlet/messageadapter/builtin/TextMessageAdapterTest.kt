/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.messageadapter.builtin

import com.tinder.scarlet.Message
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

internal class TextMessageAdapterTest {

    private val messageAdapter = TextMessageAdapter()

    @Test
    fun fromMessage() {
        // Given
        val text = "Hello"
        val textMessage = Message.Text(text)

        // When
        val adaptedData = messageAdapter.fromMessage(textMessage)

        // Then
        assertThat(adaptedData).isEqualTo(text)
    }

    @Test
    fun toMessage() {
        // Given
        val text = "Hello"
        val textMessage = Message.Text(text)

        // When
        val adaptedMessage = messageAdapter.toMessage(text)

        // Then
        assertThat(adaptedMessage).isEqualTo(textMessage)
    }
}
