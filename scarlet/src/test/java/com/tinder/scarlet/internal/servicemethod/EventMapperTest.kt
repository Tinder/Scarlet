/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.internal.servicemethod

import com.nhaarman.mockito_kotlin.given
import com.nhaarman.mockito_kotlin.mock
import com.tinder.scarlet.Deserialization
import com.tinder.scarlet.Event
import com.tinder.scarlet.Lifecycle
import com.tinder.scarlet.Message
import com.tinder.scarlet.MessageAdapter
import com.tinder.scarlet.State
import com.tinder.scarlet.WebSocket
import io.reactivex.Flowable
import io.reactivex.Maybe
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.mockito.quality.Strictness

@RunWith(Enclosed::class)
internal class EventMapperTest {

    @RunWith(Parameterized::class)
    class NoOp(
        private val events: List<Event>
    ) {
        @Suppress("UNUSED")
        @get:Rule
        val mockitoRule: MockitoRule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS)

        private val eventMapper = EventMapper.NoOp

        @Test
        fun mapToData_shouldReturnEvents() {
            // When
            val results = Flowable.fromIterable(events)
                .flatMapMaybe(eventMapper::mapToData)
                .blockingIterable()
                .toList()

            // Then
            assertThat(results).isEqualTo(events)
        }

        companion object {
            @Parameterized.Parameters
            @JvmStatic
            fun data() = listOf(
                param(events = SERVER_MESSAGES),
                param(events = SERVER_CLOSURE),
                param(events = CLIENT_CLOSURE),
                param(events = CLIENT_ABORT)
            )

            private fun param(events: List<Event>) = arrayOf(events)
        }
    }

    @RunWith(Parameterized::class)
    class FilterEventType(
        private val events: List<Event>,
        private val type: Class<Event>,
        private val filteredEvents: List<Event>
    ) {
        @Suppress("UNUSED")
        @get:Rule
        val mockitoRule: MockitoRule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS)

        private val eventMapper = EventMapper.FilterEventType(type)

        @Test
        fun mapToData_shouldReturnEvents() {
            // When
            val results = Flowable.fromIterable(events)
                .flatMapMaybe(eventMapper::mapToData)
                .blockingIterable()
                .toList()

            // Then
            results.forEach { assertThat(it).isInstanceOf(type) }
            assertThat(results).isEqualTo(filteredEvents)
        }

        companion object {
            @Parameterized.Parameters
            @JvmStatic
            fun data() = listOf(
                param(events = SERVER_MESSAGES, type = Event.OnLifecycle::class.java),
                param(events = SERVER_MESSAGES, type = Event.OnWebSocket::class.java),
                param(events = SERVER_MESSAGES, type = Event.OnStateChange::class.java),
                param(events = SERVER_MESSAGES, type = Event.OnRetry::class.java),
                param(events = SERVER_CLOSURE, type = Event.OnLifecycle::class.java),
                param(events = SERVER_CLOSURE, type = Event.OnWebSocket::class.java),
                param(events = SERVER_CLOSURE, type = Event.OnStateChange::class.java),
                param(events = SERVER_CLOSURE, type = Event.OnRetry::class.java),
                param(events = CLIENT_CLOSURE, type = Event.OnLifecycle::class.java),
                param(events = CLIENT_CLOSURE, type = Event.OnWebSocket::class.java),
                param(events = CLIENT_CLOSURE, type = Event.OnStateChange::class.java),
                param(events = CLIENT_CLOSURE, type = Event.OnRetry::class.java),
                param(events = CLIENT_ABORT, type = Event.OnLifecycle::class.java),
                param(events = CLIENT_ABORT, type = Event.OnWebSocket::class.java),
                param(events = CLIENT_ABORT, type = Event.OnStateChange::class.java),
                param(events = CLIENT_ABORT, type = Event.OnRetry::class.java)
            )

            private fun <E : Event> param(events: List<Event>, type: Class<E>) =
                arrayOf(events, type, events.filter { type.isInstance(it) })
        }
    }

    @RunWith(Parameterized::class)
    class ToWebSocketEvent(
        private val events: List<Event>,
        private val webSocketEvents: List<WebSocket.Event>
    ) {
        @Suppress("UNUSED")
        @get:Rule
        val mockitoRule: MockitoRule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS)

        private val eventMapper = EventMapper.ToWebSocketEvent

        @Test
        fun mapToData_shouldReturnEvents() {
            // When
            val results = Flowable.fromIterable(events)
                .flatMapMaybe(eventMapper::mapToData)
                .blockingIterable()
                .toList()

            // Then
            assertThat(results).isEqualTo(webSocketEvents)
        }

        companion object {
            @Parameterized.Parameters
            @JvmStatic
            fun data() = listOf(
                param(events = SERVER_MESSAGES),
                param(events = SERVER_CLOSURE),
                param(events = CLIENT_CLOSURE),
                param(events = CLIENT_ABORT)
            )

            private fun param(events: List<Event>) =
                arrayOf(events, events.mapNotNull { (it as? Event.OnWebSocket.Event<*>)?.event })
        }
    }

    @RunWith(Parameterized::class)
    class ToLifecycleState(
        private val events: List<Event>,
        private val lifecycleStates: List<Lifecycle.State>
    ) {
        @Suppress("UNUSED")
        @get:Rule
        val mockitoRule: MockitoRule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS)

        private val eventMapper = EventMapper.ToLifecycleState

        @Test
        fun mapToData_shouldReturnEvents() {
            // When
            val results = Flowable.fromIterable(events)
                .flatMapMaybe(eventMapper::mapToData)
                .blockingIterable()
                .toList()

            // Then
            assertThat(results).isEqualTo(lifecycleStates)
        }

        companion object {
            @Parameterized.Parameters
            @JvmStatic
            fun data() = listOf(
                param(events = SERVER_MESSAGES),
                param(events = SERVER_CLOSURE),
                param(events = CLIENT_CLOSURE),
                param(events = CLIENT_ABORT)
            )

            private fun param(events: List<Event>) =
                arrayOf(events, events.mapNotNull { (it as? Event.OnLifecycle.StateChange<*>)?.state })
        }
    }

    @RunWith(Parameterized::class)
    class ToDeserialization(
        private val events: List<Event>,
        private val expectedResults: List<Deserialization<Message>>
    ) {
        @Suppress("UNUSED")
        @get:Rule
        val mockitoRule: MockitoRule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS)

        private val messageAdapter = mock<MessageAdapter<String>>()
        private val eventMapper = EventMapper.ToDeserialization(messageAdapter)

        @Test
        fun mapToData_shouldReturnSerialization() {
            // Given
            given(messageAdapter.fromMessage(MESSAGE)).willReturn(TEXT)
            given(messageAdapter.fromMessage(MALFORMED_MESSAGE)).willThrow(THROWABLE)

            // When
            val results = Flowable.fromIterable(events)
                .flatMapMaybe(eventMapper::mapToData)
                .blockingIterable()
                .toList()

            // Then
            assertThat(results).isEqualTo(expectedResults)
        }

        companion object {
            @Parameterized.Parameters
            @JvmStatic
            fun data() = listOf(
                param(
                    events = SERVER_MESSAGES,
                    results = listOf(
                        SUCCESSFUL_DESERIALIZATION,
                        FAILED_DESERIALIZATION,
                        FAILED_DESERIALIZATION,
                        SUCCESSFUL_DESERIALIZATION,
                        FAILED_DESERIALIZATION
                    )
                ),
                param(
                    events = SERVER_CLOSURE,
                    results = listOf(
                        SUCCESSFUL_DESERIALIZATION,
                        FAILED_DESERIALIZATION
                    )
                ),
                param(events = CLIENT_CLOSURE, results = listOf()),
                param(events = CLIENT_ABORT, results = listOf())
            )

            private fun param(events: List<Event>, results: List<Deserialization<String>>) =
                arrayOf(events, results)
        }
    }

    @RunWith(Parameterized::class)
    class ToDeserializedValue(
        private val events: List<Event>,
        private val expectedResults: List<Maybe<Message>>
    ) {
        @Suppress("UNUSED")
        @get:Rule
        val mockitoRule: MockitoRule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS)

        private val messageAdapter = mock<MessageAdapter<Any>>()
        private val eventMapper = EventMapper.ToDeserializedValue(EventMapper.ToDeserialization(messageAdapter))

        @Test
        fun mapToData_shouldReturnDeserializedValue() {
            // Given
            given(messageAdapter.fromMessage(MESSAGE)).willReturn(TEXT)
            given(messageAdapter.fromMessage(MALFORMED_MESSAGE)).willThrow(THROWABLE)

            // When
            val results = Flowable.fromIterable(events)
                .flatMapMaybe(eventMapper::mapToData)
                .blockingIterable()
                .toList()

            // Then
            assertThat(results).isEqualTo(expectedResults)
        }

        companion object {
            @Parameterized.Parameters
            @JvmStatic
            fun data() = listOf(
                param(events = SERVER_MESSAGES, results = listOf(TEXT, TEXT)),
                param(events = SERVER_CLOSURE, results = listOf(TEXT)),
                param(events = CLIENT_CLOSURE, results = listOf()),
                param(events = CLIENT_ABORT, results = listOf())
            )

            private fun param(events: List<Event>, results: List<String>) = arrayOf(events, results)
        }
    }

    private companion object {
        private const val TEXT = "hello"
        private val MESSAGE = Message.Text(TEXT)
        private val WEB_SOCKET_EVENT_ON_MESSAGE_RECEIVED = WebSocket.Event.OnMessageReceived(MESSAGE)
        private val MALFORMED_MESSAGE = mock<Message>()
        private val WEB_SOCKET_EVENT_ON_MALFORMED_MESSAGE_RECEIVED =
            WebSocket.Event.OnMessageReceived(MALFORMED_MESSAGE)
        private val THROWABLE = IllegalArgumentException()
        private val SUCCESSFUL_DESERIALIZATION = Deserialization.Success(TEXT)
        private val FAILED_DESERIALIZATION = Deserialization.Error<String>(THROWABLE)

        private val ON_LIFECYCLE_START = Event.OnLifecycle.StateChange(Lifecycle.State.Started)
        private val ON_LIFECYCLE_STOP_WITH_REASON =
            Event.OnLifecycle.StateChange(mock<Lifecycle.State.Stopped.WithReason>())
        private val ON_LIFECYCLE_STOP_AND_ABORT =
            Event.OnLifecycle.StateChange(mock<Lifecycle.State.Stopped.AndAborted>())
        private val ON_LIFECYCLE_TERMINATED = Event.OnLifecycle.Terminate

        private val ON_WEB_SOCKET_CONNECTION_OPENED =
            Event.OnWebSocket.Event(mock<WebSocket.Event.OnConnectionOpened<*>>())
        private val ON_WEB_SOCKET_MESSAGE_RECEIVED = Event.OnWebSocket.Event(WEB_SOCKET_EVENT_ON_MESSAGE_RECEIVED)
        private val ON_WEB_SOCKET_MESSAGE_RECEIVED_MALFORMED =
            Event.OnWebSocket.Event(WEB_SOCKET_EVENT_ON_MALFORMED_MESSAGE_RECEIVED)
        private val ON_WEB_SOCKET_CONNECTION_CLOSING =
            Event.OnWebSocket.Event(mock<WebSocket.Event.OnConnectionClosing>())
        private val ON_WEB_SOCKET_CONNECTION_CLOSED =
            Event.OnWebSocket.Event(mock<WebSocket.Event.OnConnectionClosed>())
        private val ON_WEB_SOCKET_CONNECTION_FAILED =
            Event.OnWebSocket.Event(mock<WebSocket.Event.OnConnectionFailed>())
        private val ON_WEB_SOCKET_CONNECTION_TERMINATED = Event.OnWebSocket.Terminate

        private val ON_RETRY = Event.OnRetry

        private val ON_STATE_CHANGE_TO_CONNECTING = Event.OnStateChange(mock<State.Connecting>())
        private val ON_STATE_CHANGE_TO_CONNECTED = Event.OnStateChange(mock<State.Connected>())
        private val ON_STATE_CHANGE_TO_WAITING_TO_RETRY = Event.OnStateChange(mock<State.WaitingToRetry>())
        private val ON_STATE_CHANGE_TO_DISCONNECTING = Event.OnStateChange(mock<State.Disconnecting>())
        private val ON_STATE_CHANGE_TO_DISCONNECTED = Event.OnStateChange(mock<State.Disconnected>())
        private val ON_STATE_CHANGE_TO_DESTROYED = Event.OnStateChange(mock<State.Destroyed>())

        val SERVER_MESSAGES = listOf(
            ON_LIFECYCLE_START,
            ON_STATE_CHANGE_TO_CONNECTING,
            ON_WEB_SOCKET_CONNECTION_OPENED,
            ON_STATE_CHANGE_TO_CONNECTED,
            ON_WEB_SOCKET_MESSAGE_RECEIVED,
            ON_WEB_SOCKET_MESSAGE_RECEIVED_MALFORMED,
            ON_WEB_SOCKET_MESSAGE_RECEIVED_MALFORMED,
            ON_WEB_SOCKET_MESSAGE_RECEIVED,
            ON_WEB_SOCKET_MESSAGE_RECEIVED_MALFORMED
        )

        val SERVER_CLOSURE = listOf(
            ON_LIFECYCLE_START,
            ON_STATE_CHANGE_TO_CONNECTING,
            ON_WEB_SOCKET_CONNECTION_OPENED,
            ON_STATE_CHANGE_TO_CONNECTED,
            ON_WEB_SOCKET_MESSAGE_RECEIVED,
            ON_WEB_SOCKET_MESSAGE_RECEIVED_MALFORMED,
            ON_WEB_SOCKET_CONNECTION_CLOSING,
            ON_WEB_SOCKET_CONNECTION_CLOSED,
            ON_WEB_SOCKET_CONNECTION_TERMINATED,
            ON_STATE_CHANGE_TO_WAITING_TO_RETRY,
            ON_RETRY,
            ON_STATE_CHANGE_TO_CONNECTING,
            ON_WEB_SOCKET_CONNECTION_TERMINATED,
            ON_STATE_CHANGE_TO_WAITING_TO_RETRY
        )

        val CLIENT_CLOSURE = listOf(
            ON_LIFECYCLE_START,
            ON_STATE_CHANGE_TO_CONNECTING,
            ON_WEB_SOCKET_CONNECTION_OPENED,
            ON_STATE_CHANGE_TO_CONNECTED,
            ON_LIFECYCLE_STOP_WITH_REASON,
            ON_STATE_CHANGE_TO_DISCONNECTING,
            ON_WEB_SOCKET_CONNECTION_CLOSING,
            ON_WEB_SOCKET_CONNECTION_CLOSED,
            ON_WEB_SOCKET_CONNECTION_TERMINATED,
            ON_STATE_CHANGE_TO_DISCONNECTED,
            ON_LIFECYCLE_TERMINATED,
            ON_STATE_CHANGE_TO_DESTROYED
        )

        val CLIENT_ABORT = listOf(
            ON_LIFECYCLE_START,
            ON_STATE_CHANGE_TO_CONNECTING,
            ON_WEB_SOCKET_CONNECTION_OPENED,
            ON_STATE_CHANGE_TO_CONNECTED,
            ON_LIFECYCLE_STOP_AND_ABORT,
            ON_STATE_CHANGE_TO_DISCONNECTING,
            ON_WEB_SOCKET_CONNECTION_FAILED,
            ON_WEB_SOCKET_CONNECTION_TERMINATED,
            ON_STATE_CHANGE_TO_DISCONNECTED,
            ON_LIFECYCLE_TERMINATED,
            ON_STATE_CHANGE_TO_DESTROYED
        )
    }
}
