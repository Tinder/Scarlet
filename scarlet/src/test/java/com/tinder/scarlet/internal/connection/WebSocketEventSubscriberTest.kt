/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.internal.connection

import com.nhaarman.mockito_kotlin.argThat
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.then
import com.tinder.scarlet.Event
import com.tinder.scarlet.WebSocket
import com.tinder.scarlet.internal.connection.subscriber.WebSocketEventSubscriber
import io.reactivex.Flowable
import io.reactivex.processors.ReplayProcessor
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.Test

internal class WebSocketEventSubscriberTest {
    private val connectionStateManager = mock<Connection.StateManager>()
    private val webSocketEventSubscriber = WebSocketEventSubscriber(connectionStateManager)

    @Test
    fun onNext_shouldEmitOnWebSocketEvent() {
        // Given
        val event = mock<WebSocket.Event>()
        val flowable = ReplayProcessor.create<WebSocket.Event>()
            .apply { onNext(event) }

        // When
        flowable.subscribe(webSocketEventSubscriber)

        // Then
        then(connectionStateManager).should().handleEvent(
            argThat<Event.OnWebSocket.Event<*>> { this.event == event })
    }

    @Test
    fun onComplete_shouldEmitOnWebSocketTerminate() {
        // Given
        val flowable = Flowable.empty<WebSocket.Event>()

        // When
        flowable.subscribe(webSocketEventSubscriber)

        // Then
        then(connectionStateManager).should().handleEvent(Event.OnWebSocket.Terminate)
    }

    @Test
    fun onError_shouldThrowException() {
        // Given
        val exception = RuntimeException("")
        val flowable = Flowable.error<WebSocket.Event>(exception)

        // Then
        assertThatExceptionOfType(RuntimeException::class.java)
            .isThrownBy { flowable.subscribe(webSocketEventSubscriber) }
    }
}
