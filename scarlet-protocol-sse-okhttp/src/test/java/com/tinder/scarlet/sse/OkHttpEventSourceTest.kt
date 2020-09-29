/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.sse

import com.tinder.scarlet.Stream
import com.tinder.scarlet.testutils.any
import com.tinder.scarlet.testutils.containingText3
import com.tinder.scarlet.testutils.rule.OkHttpSseConnection
import com.tinder.scarlet.testutils.test
import com.tinder.scarlet.ws.Receive
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions
import org.junit.Rule
import org.junit.Test

class OkHttpEventSourceTest {

    @get:Rule
    val server = MockWebServer()

    @get:Rule
    internal val connection = OkHttpSseConnection.create<Service>(
        observeEventSourceEvent = { observeEventSourceEvent() },
        clientConfiguration = OkHttpSseConnection.Configuration(
            serverUrl = { server.url("/").toString() }
        )
    )

    @Test
    fun givenConnectionIsEstablished_andServerSendsMessages_shouldReceiveMessages() {
        // Given
        val textMessage = "hey"
        server.enqueue(
            MockResponse()
                .setBody(
                    """data: $textMessage
                    |
                    |""".trimMargin()
                )
                .setHeader("content-type", "text/event-stream")
        )
        val testTextStreamObserver = connection.client.observeText().test()

        // When
        connection.open()

        // Then
        connection.clientEventSourceEventObserver.awaitValues(
            any<EventSourceEvent.OnConnectionOpened>(),
            any<EventSourceEvent.OnMessageReceived>().containingText3(textMessage)
        )
        Assertions.assertThat(testTextStreamObserver.values).containsExactly(textMessage)
    }

    @Test
    fun badContentType() {
        // Given
        server.enqueue(
            MockResponse()
                .setBody(
                    """data: hey
                    |
                    |""".trimMargin()
                )
                .setHeader("content-type", "text/plain")
        )

        // When
        connection.open()

        // Then
        connection.clientEventSourceEventObserver.awaitValues(
            any<EventSourceEvent.OnConnectionFailed>()
        )
    }

    @Test
    fun badResponseCode() {
        // Given
        server.enqueue(
            MockResponse()
                .setBody(
                    """data: hey
                    |
                    |""".trimMargin()
                )
                .setHeader("content-type", "text/event-stream")
                .setResponseCode(401)
        )

        // When
        connection.open()

        // Then
        connection.clientEventSourceEventObserver.awaitValues(
            any<EventSourceEvent.OnConnectionFailed>()
        )
    }

    companion object {
        internal interface Service {
            @Receive
            fun observeEventSourceEvent(): Stream<EventSourceEvent>

            @Receive
            fun observeText(): Stream<String>
        }
    }
}