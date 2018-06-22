/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.internal.servicemethod

import com.nhaarman.mockito_kotlin.given
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.only
import com.nhaarman.mockito_kotlin.then
import com.tinder.scarlet.Message
import com.tinder.scarlet.MessageAdapter
import com.tinder.scarlet.internal.connection.Connection
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
internal class SendServiceMethodTest {
    private val webSocketClient = mock<Connection>()
    private val messageAdapter = mock<MessageAdapter<Any>>()
    private val serviceMethod = ServiceMethod.Send(webSocketClient, messageAdapter)

    @Test
    fun execute_shouldOnlySendMessage() {
        // Given
        val data = 1
        val expectedMessage = Message.Text("1")
        given(messageAdapter.toMessage(data)).willReturn(expectedMessage)
        given(webSocketClient.send(expectedMessage)).willReturn(true)

        // When
        val isSuccessful = serviceMethod.execute(data) as Boolean

        // Then
        then(webSocketClient).should(only()).send(expectedMessage)
        assertThat(isSuccessful).isTrue()
    }
}
