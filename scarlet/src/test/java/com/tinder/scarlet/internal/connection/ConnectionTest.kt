/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.internal.connection

import com.nhaarman.mockito_kotlin.given
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.then
import com.tinder.scarlet.Event
import com.tinder.scarlet.Message
import com.tinder.scarlet.Session
import com.tinder.scarlet.State
import com.tinder.scarlet.WebSocket
import io.reactivex.processors.PublishProcessor
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
internal class ConnectionTest {

    private val stateManager = mock<Connection.StateManager>()
    private val connection = Connection(stateManager)

    @Test
    fun startForever_shouldDelegateToStateManager() {
        // When
        connection.startForever()

        // Then
        then(stateManager).should().subscribe()
    }

    @Test
    fun observeEvent_shouldDelegateToStateManager() {
        // Given
        val processor = PublishProcessor.create<Event>()
        given(connection.observeEvent()).willReturn(processor)

        // When
        connection.observeEvent().test()

        // Then
        then(stateManager).should().observeEvent()
        assertThat(processor.hasSubscribers()).isTrue()
    }

    @Test
    fun send_givenIsConnected_shouldSendMessage() {
        // Given
        val webSocket = mock<WebSocket>()
        val state = State.Connected(Session(webSocket, mock()))
        given(stateManager.state).willReturn(state)
        val message = mock<Message>()

        // When
        connection.send(message)

        // Then
        then(webSocket).should().send(message)
    }
}
