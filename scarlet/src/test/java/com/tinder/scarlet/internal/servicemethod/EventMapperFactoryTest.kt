/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.internal.servicemethod

import com.nhaarman.mockito_kotlin.given
import com.nhaarman.mockito_kotlin.mock
import com.tinder.scarlet.Deserialization
import com.tinder.scarlet.Event
import com.tinder.scarlet.Lifecycle
import com.tinder.scarlet.MessageAdapter
import com.tinder.scarlet.State
import com.tinder.scarlet.Stream
import com.tinder.scarlet.WebSocket
import com.tinder.scarlet.utils.getParameterUpperBound
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Proxy
import java.lang.reflect.Type

@RunWith(MockitoJUnitRunner::class)
internal class EventMapperFactoryTest {

    private val messageAdapterResolver = mock<MessageAdapterResolver>()
    private val eventMapperFactory = EventMapper.Factory(messageAdapterResolver)

    @Test
    fun create_givenEvent_shouldCreateNoOp() {
        // Given
        val (type, annotations) = getReturnTypeAndAnnotations { streamOfEvent() }

        // When
        val eventMapper = eventMapperFactory.create(type, annotations)

        // Then
        assertThat(eventMapper).isInstanceOf(EventMapper.NoOp::class.java)
    }

    @Test
    fun create_givenEvenSubclass_shouldThrowIllegalArgumentException() {
        // Given
        val typeAndAnnotations = listOf(
            getReturnTypeAndAnnotations { streamOfEventSubclassOnLifecycle() },
            getReturnTypeAndAnnotations { streamOfEventSubclassOnWebSocket() },
            getReturnTypeAndAnnotations { streamOfEventSubclassOnStateChange() },
            getReturnTypeAndAnnotations { streamOfEventSubclassOnRetry() },
            getReturnTypeAndAnnotations { streamOfEventSubclassOnLifecycleStateChange() },
            getReturnTypeAndAnnotations { streamOfEventSubclassOnLifecycleStateChangeStarted() },
            getReturnTypeAndAnnotations { streamOfEventSubclassOnLifecycleStateChangeStopped() },
            getReturnTypeAndAnnotations { streamOfEventSubclassOnLifecycleStateChangeStoppedWithReason() },
            getReturnTypeAndAnnotations { streamOfEventSubclassOnLifecycleStateChangeStoppedAndAborted() },
            getReturnTypeAndAnnotations { streamOfEventSubclassOnLifecycleTerminate() },
            getReturnTypeAndAnnotations { streamOfEventSubclassOnWebSocketEvent() },
            getReturnTypeAndAnnotations { streamOfEventSubclassOnWebSocketEventOnConnectionOpenedAny() },
            getReturnTypeAndAnnotations { streamOfEventSubclassOnWebSocketEventOnConnectionOpenedMyWebSocket() },
            getReturnTypeAndAnnotations { streamOfEventSubclassOnWebSocketEventOnMessageReceived() },
            getReturnTypeAndAnnotations { streamOfEventSubclassOnWebSocketEventOnConnectionClosing() },
            getReturnTypeAndAnnotations { streamOfEventSubclassOnWebSocketEventOnConnectionClosed() },
            getReturnTypeAndAnnotations { streamOfEventSubclassOnWebSocketEventOnConnectionFailed() },
            getReturnTypeAndAnnotations { streamOfEventSubclassOnWebSocketTerminate() })

        // Then
        typeAndAnnotations.forEach { (type, annotations) ->

            assertThatIllegalArgumentException().isThrownBy {
                eventMapperFactory.create(type, annotations)
            }
        }
    }

    @Test
    fun create_givenLifecycleState_shouldCreateToLifecycleState() {
        // Given
        val (type, annotations) = getReturnTypeAndAnnotations { streamOfLifecycleState() }

        // When
        val eventMapper = eventMapperFactory.create(type, annotations)

        // Then
        assertThat(eventMapper).isInstanceOf(EventMapper.ToLifecycleState::class.java)
    }

    @Test
    fun create_givenLifecycleStateSubclass_shouldThrowIllegalArgumentException() {
        // Given
        val typeAndAnnotations = listOf(
            getReturnTypeAndAnnotations { streamOfLifecycleStateSubclassStarted() },
            getReturnTypeAndAnnotations { streamOfLifecycleStateSubclassStopped() })

        // Then
        typeAndAnnotations.forEach { (type, annotations) ->
            assertThatIllegalArgumentException().isThrownBy {
                eventMapperFactory.create(type, annotations)
            }
        }
    }

    @Test
    fun create_givenWebSocketEvent_shouldCreateToWebSocketEvent() {
        // Given
        val (type, annotations) = getReturnTypeAndAnnotations { streamOfWebSocketEvent() }

        // When
        val eventMapper = eventMapperFactory.create(type, annotations)

        // Then
        assertThat(eventMapper).isInstanceOf(EventMapper.ToWebSocketEvent::class.java)
    }

    @Test
    fun create_givenWebSocketEventSubclass_shouldThrowIllegalArgumentException() {
        // Given
        val typeAndAnnotations = listOf(
            getReturnTypeAndAnnotations { streamOfWebSocketEventSubclassOnConnectionOpened() },
            getReturnTypeAndAnnotations { streamOfWebSocketEventSubclassOnMessageReceived() })

        // Then
        typeAndAnnotations.forEach { (type, annotations) ->
            assertThatIllegalArgumentException().isThrownBy {
                eventMapperFactory.create(type, annotations)
            }
        }
    }

    @Test
    fun create_givenState_shouldCreateState() {
        // Given
        val (type, annotations) = getReturnTypeAndAnnotations { streamOfState() }

        // When
        val eventMapper = eventMapperFactory.create(type, annotations)

        // Then
        assertThat(eventMapper).isInstanceOf(EventMapper.ToState::class.java)
    }

    @Test
    fun create_givenStateSubclass_shouldThrowIllegalArgumentException() {
        // Given
        val typeAndAnnotations = listOf(
            getReturnTypeAndAnnotations { streamOfStateSubclassConnected() },
            getReturnTypeAndAnnotations { streamOfStateSubclassDisconnected() })

        // Then
        typeAndAnnotations.forEach { (type, annotations) ->
            assertThatIllegalArgumentException().isThrownBy {
                eventMapperFactory.create(type, annotations)
            }
        }
    }

    @Test
    fun create_givenStreamOfDeserializationOfString_shouldCreateToDeserialization() {
        // Given
        val (type, annotations) = getReturnTypeAndAnnotations { streamOfDeserializationOfString() }
        val messageAdapter = mock<MessageAdapter<Any>>()
        val expectedMessageType = type.getFirstParameterType().getFirstParameterType()
        given(messageAdapterResolver.resolve(expectedMessageType, annotations)).willReturn(messageAdapter)

        // When
        val eventMapper = eventMapperFactory.create(type, annotations)

        // Then
        assertThat(eventMapper).isInstanceOf(EventMapper.ToDeserialization::class.java)
    }

    @Test
    fun create_givenStreamOfDeserializationOfString_andHasBeenCached_shouldReturnTheCachedValue() {
        // Given
        val (type, annotations) = getReturnTypeAndAnnotations { streamOfDeserializationOfString() }
        val messageAdapter = mock<MessageAdapter<Any>>()
        val expectedMessageType = type.getFirstParameterType().getFirstParameterType()
        given(messageAdapterResolver.resolve(expectedMessageType, annotations)).willReturn(messageAdapter)
        val cachedEventMapper = eventMapperFactory.create(type, annotations)

        // When
        val eventMapper = eventMapperFactory.create(type, annotations)

        // Then
        assertThat(eventMapper).isSameAs(cachedEventMapper)
    }

    @Test
    fun create_givenStreamOfString_shouldCreateToDeserializedValue() {
        // Given
        val (type, annotations) = getReturnTypeAndAnnotations { streamOfString() }
        val messageAdapter = mock<MessageAdapter<Any>>()
        val expectedMessageType = type.getFirstParameterType()
        given(messageAdapterResolver.resolve(expectedMessageType, annotations)).willReturn(messageAdapter)

        // When
        val eventMapper = eventMapperFactory.create(type, annotations)

        // Then
        assertThat(eventMapper).isInstanceOf(EventMapper.ToDeserializedValue::class.java)
    }

    companion object {

        private annotation class MyAnnotation

        private interface MyWebSocket : WebSocket

        @Suppress("UNUSED")
        private interface Types {
            @MyAnnotation
            fun streamOfEvent(): Stream<Event>

            fun streamOfEventSubclassOnLifecycle(): Stream<Event.OnLifecycle>

            fun streamOfEventSubclassOnWebSocket(): Stream<Event.OnWebSocket>

            fun streamOfEventSubclassOnStateChange(): Stream<Event.OnStateChange<State>>

            fun streamOfEventSubclassOnRetry(): Stream<Event.OnRetry>

            fun streamOfEventSubclassOnLifecycleStateChange(): Stream<Event.OnLifecycle.StateChange<Lifecycle.State>>

            fun streamOfEventSubclassOnLifecycleStateChangeStarted(): Stream<Event.OnLifecycle.StateChange<Lifecycle.State.Started>>

            fun streamOfEventSubclassOnLifecycleStateChangeStopped(): Stream<Event.OnLifecycle.StateChange<Lifecycle.State.Stopped>>

            fun streamOfEventSubclassOnLifecycleStateChangeStoppedWithReason():
                    Stream<Event.OnLifecycle.StateChange<Lifecycle.State.Stopped.WithReason>>

            fun streamOfEventSubclassOnLifecycleStateChangeStoppedAndAborted():
                    Stream<Event.OnLifecycle.StateChange<Lifecycle.State.Stopped.AndAborted>>

            fun streamOfEventSubclassOnLifecycleTerminate(): Stream<Event.OnLifecycle.Terminate>

            fun streamOfEventSubclassOnWebSocketEvent(): Stream<Event.OnWebSocket.Event<WebSocket.Event>>

            fun streamOfEventSubclassOnWebSocketEventOnConnectionOpenedAny():
                    Stream<Event.OnWebSocket.Event<WebSocket.Event.OnConnectionOpened<*>>>

            fun streamOfEventSubclassOnWebSocketEventOnConnectionOpenedMyWebSocket():
                    Stream<Event.OnWebSocket.Event<WebSocket.Event.OnConnectionOpened<MyWebSocket>>>

            fun streamOfEventSubclassOnWebSocketEventOnMessageReceived():
                    Stream<Event.OnWebSocket.Event<WebSocket.Event.OnMessageReceived>>

            fun streamOfEventSubclassOnWebSocketEventOnConnectionClosing():
                    Stream<Event.OnWebSocket.Event<WebSocket.Event.OnConnectionClosing>>

            fun streamOfEventSubclassOnWebSocketEventOnConnectionClosed():
                    Stream<Event.OnWebSocket.Event<WebSocket.Event.OnConnectionClosed>>

            fun streamOfEventSubclassOnWebSocketEventOnConnectionFailed():
                    Stream<Event.OnWebSocket.Event<WebSocket.Event.OnConnectionFailed>>

            fun streamOfEventSubclassOnWebSocketTerminate(): Stream<Event.OnWebSocket.Terminate>

            fun streamOfEventSubclassOnStateChangeConnected(): Stream<Event.OnStateChange<State.Connected>>

            fun streamOfLifecycleState(): Stream<Lifecycle.State>

            fun streamOfLifecycleStateSubclassStarted(): Stream<Lifecycle.State.Started>

            fun streamOfLifecycleStateSubclassStopped(): Stream<Lifecycle.State.Stopped>

            fun streamOfWebSocketEvent(): Stream<WebSocket.Event>

            fun streamOfWebSocketEventSubclassOnConnectionOpened(): Stream<WebSocket.Event.OnConnectionOpened<*>>

            fun streamOfWebSocketEventSubclassOnMessageReceived(): Stream<WebSocket.Event.OnMessageReceived>

            fun streamOfState(): Stream<State>

            fun streamOfStateSubclassConnected(): Stream<State.Connected>

            fun streamOfStateSubclassDisconnected(): Stream<State.Disconnected>

            fun streamOfDeserializationOfString(): Stream<Deserialization<String>>

            fun streamOfString(): Stream<String>
        }

        private fun getReturnTypeAndAnnotations(methodCall: Types.() -> Any) = ReturnTypeResolver().resolve(methodCall)

        private fun Type.getFirstParameterType(): Type = (this as ParameterizedType).getParameterUpperBound(0)

        private class ReturnTypeResolver {
            private lateinit var method: Method

            private val methodRecorder = Proxy.newProxyInstance(
                Types::class.java.classLoader,
                arrayOf(Types::class.java)
            ) { _, method, _ ->
                this.method = method
                null
            } as Types

            fun resolve(methodCall: Types.() -> Any): Pair<ParameterizedType, Array<Annotation>> {
                methodRecorder.methodCall()
                return (method.genericReturnType as ParameterizedType) to method.annotations
            }
        }
    }
}
