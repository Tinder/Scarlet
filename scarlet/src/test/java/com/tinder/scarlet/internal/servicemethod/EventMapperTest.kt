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

//@RunWith(Enclosed::class)
//internal class EventMapperTest {
//
//
//    @RunWith(Parameterized::class)
//    class FilterEventType(
//        private val events: List<Event>,
//        private val type: Class<Event>,
//        private val filteredEvents: List<Event>
//    ) {
//        @Suppress("UNUSED")
//        @get:Rule
//        val mockitoRule: MockitoRule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS)
//
//        private val eventMapper = EventMapper.FilterEventType(type)
//
//        @Test
//        fun mapToData_shouldReturnEvents() {
//            // When
//            val results = Flowable.fromIterable(events)
//                .flatMapMaybe(eventMapper::mapToData)
//                .blockingIterable()
//                .toList()
//
//            // Then
//            results.forEach { assertThat(it).isInstanceOf(type) }
//            assertThat(results).isEqualTo(filteredEvents)
//        }
//
//        companion object {
//            @Parameterized.Parameters
//            @JvmStatic
//            fun data() = listOf(
//                param(events = SERVER_MESSAGES, type = Event.OnLifecycle::class.java),
//                param(events = SERVER_MESSAGES, type = Event.OnWebSocket::class.java),
//                param(events = SERVER_MESSAGES, type = Event.OnStateChange::class.java),
//                param(events = SERVER_MESSAGES, type = Event.OnRetry::class.java),
//                param(events = SERVER_CLOSURE, type = Event.OnLifecycle::class.java),
//                param(events = SERVER_CLOSURE, type = Event.OnWebSocket::class.java),
//                param(events = SERVER_CLOSURE, type = Event.OnStateChange::class.java),
//                param(events = SERVER_CLOSURE, type = Event.OnRetry::class.java),
//                param(events = CLIENT_CLOSURE, type = Event.OnLifecycle::class.java),
//                param(events = CLIENT_CLOSURE, type = Event.OnWebSocket::class.java),
//                param(events = CLIENT_CLOSURE, type = Event.OnStateChange::class.java),
//                param(events = CLIENT_CLOSURE, type = Event.OnRetry::class.java),
//                param(events = CLIENT_ABORT, type = Event.OnLifecycle::class.java),
//                param(events = CLIENT_ABORT, type = Event.OnWebSocket::class.java),
//                param(events = CLIENT_ABORT, type = Event.OnStateChange::class.java),
//                param(events = CLIENT_ABORT, type = Event.OnRetry::class.java)
//            )
//
//            private fun <E : Event> param(events: List<Event>, type: Class<E>) =
//                arrayOf(events, type, events.filter { type.isInstance(it) })
//        }
//    }
//
//    @RunWith(Parameterized::class)
//    class ToWebSocketEvent(
//        private val events: List<Event>,
//        private val webSocketEvents: List<WebSocket.Event>
//    ) {
//        @Suppress("UNUSED")
//        @get:Rule
//        val mockitoRule: MockitoRule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS)
//
//        private val eventMapper = EventMapper.ToWebSocketEvent
//
//        @Test
//        fun mapToData_shouldReturnEvents() {
//            // When
//            val results = Flowable.fromIterable(events)
//                .flatMapMaybe(eventMapper::mapToData)
//                .blockingIterable()
//                .toList()
//
//            // Then
//            assertThat(results).isEqualTo(webSocketEvents)
//        }
//
//        companion object {
//            @Parameterized.Parameters
//            @JvmStatic
//            fun data() = listOf(
//                param(events = SERVER_MESSAGES),
//                param(events = SERVER_CLOSURE),
//                param(events = CLIENT_CLOSURE),
//                param(events = CLIENT_ABORT)
//            )
//
//            private fun param(events: List<Event>) =
//                arrayOf(events, events.mapNotNull { (it as? Event.OnWebSocket.Event<*>)?.event })
//        }
//    }
//
//    @RunWith(Parameterized::class)
//    class ToLifecycleState(
//        private val events: List<Event>,
//        private val lifecycleStates: List<Lifecycle.State>
//    ) {
//        @Suppress("UNUSED")
//        @get:Rule
//        val mockitoRule: MockitoRule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS)
//
//        private val eventMapper = EventMapper.ToLifecycleState
//
//        @Test
//        fun mapToData_shouldReturnEvents() {
//            // When
//            val results = Flowable.fromIterable(events)
//                .flatMapMaybe(eventMapper::mapToData)
//                .blockingIterable()
//                .toList()
//
//            // Then
//            assertThat(results).isEqualTo(lifecycleStates)
//        }
//
//        companion object {
//            @Parameterized.Parameters
//            @JvmStatic
//            fun data() = listOf(
//                param(events = SERVER_MESSAGES),
//                param(events = SERVER_CLOSURE),
//                param(events = CLIENT_CLOSURE),
//                param(events = CLIENT_ABORT)
//            )
//
//            private fun param(events: List<Event>) =
//                arrayOf(events, events.mapNotNull { (it as? Event.OnLifecycle.StateChange<*>)?.state })
//        }
//    }
//
//    @RunWith(Parameterized::class)
//    class ToDeserialization(
//        private val events: List<Event>,
//        private val expectedResults: List<Deserialization<Message>>
//    ) {
//        @Suppress("UNUSED")
//        @get:Rule
//        val mockitoRule: MockitoRule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS)
//
//        private val messageAdapter = mock<MessageAdapter<String>>()
//        private val eventMapper = EventMapper.ToDeserialization(messageAdapter)
//
//        @Test
//        fun mapToData_shouldReturnSerialization() {
//            // Given
//            given(messageAdapter.fromMessage(MESSAGE)).willReturn(TEXT)
//            given(messageAdapter.fromMessage(MALFORMED_MESSAGE)).willThrow(THROWABLE)
//
//            // When
//            val results = Flowable.fromIterable(events)
//                .flatMapMaybe(eventMapper::mapToData)
//                .blockingIterable()
//                .toList()
//
//            // Then
//            assertThat(results).isEqualTo(expectedResults)
//        }
//
//        companion object {
//            @Parameterized.Parameters
//            @JvmStatic
//            fun data() = listOf(
//                param(
//                    events = SERVER_MESSAGES,
//                    results = listOf(
//                        SUCCESSFUL_DESERIALIZATION,
//                        FAILED_DESERIALIZATION,
//                        FAILED_DESERIALIZATION,
//                        SUCCESSFUL_DESERIALIZATION,
//                        FAILED_DESERIALIZATION
//                    )
//                ),
//                param(
//                    events = SERVER_CLOSURE,
//                    results = listOf(
//                        SUCCESSFUL_DESERIALIZATION,
//                        FAILED_DESERIALIZATION
//                    )
//                ),
//                param(events = CLIENT_CLOSURE, results = listOf()),
//                param(events = CLIENT_ABORT, results = listOf())
//            )
//
//            private fun param(events: List<Event>, results: List<Deserialization<String>>) =
//                arrayOf(events, results)
//        }
//    }
//
//    @RunWith(Parameterized::class)
//    class ToDeserializedValue(
//        private val events: List<Event>,
//        private val expectedResults: List<Maybe<Message>>
//    ) {
//        @Suppress("UNUSED")
//        @get:Rule
//        val mockitoRule: MockitoRule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS)
//
//        private val messageAdapter = mock<MessageAdapter<Any>>()
//        private val eventMapper = EventMapper.ToDeserializedValue(EventMapper.ToDeserialization(messageAdapter))
//
//        @Test
//        fun mapToData_shouldReturnDeserializedValue() {
//            // Given
//            given(messageAdapter.fromMessage(MESSAGE)).willReturn(TEXT)
//            given(messageAdapter.fromMessage(MALFORMED_MESSAGE)).willThrow(THROWABLE)
//
//            // When
//            val results = Flowable.fromIterable(events)
//                .flatMapMaybe(eventMapper::mapToData)
//                .blockingIterable()
//                .toList()
//
//            // Then
//            assertThat(results).isEqualTo(expectedResults)
//        }
//
//        companion object {
//            @Parameterized.Parameters
//            @JvmStatic
//            fun data() = listOf(
//                param(events = SERVER_MESSAGES, results = listOf(TEXT, TEXT)),
//                param(events = SERVER_CLOSURE, results = listOf(TEXT)),
//                param(events = CLIENT_CLOSURE, results = listOf()),
//                param(events = CLIENT_ABORT, results = listOf())
//            )
//
//            private fun param(events: List<Event>, results: List<String>) = arrayOf(events, results)
//        }
//    }
//
//    private companion object {
//
//    }
//}
//

