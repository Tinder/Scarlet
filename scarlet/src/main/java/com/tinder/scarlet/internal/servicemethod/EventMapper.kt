/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.internal.servicemethod

import com.tinder.scarlet.Deserialization
import com.tinder.scarlet.Event
import com.tinder.scarlet.Lifecycle
import com.tinder.scarlet.Message
import com.tinder.scarlet.MessageAdapter
import com.tinder.scarlet.State
import com.tinder.scarlet.WebSocket
import com.tinder.scarlet.utils.getParameterUpperBound
import com.tinder.scarlet.utils.getRawType
import io.reactivex.Maybe
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

internal sealed class EventMapper<T : Any> {

    abstract fun mapToData(event: Event): Maybe<T>

    object NoOp : EventMapper<Any>() {
        override fun mapToData(event: Event): Maybe<Any> = Maybe.just(event)
    }

    class FilterEventType<E : Event>(private val clazz: Class<E>) : EventMapper<E>() {
        override fun mapToData(event: Event): Maybe<E> = if (clazz.isInstance(event)) {
            @Suppress("UNCHECKED_CAST")
            Maybe.just(event as E)
        } else {
            Maybe.empty()
        }
    }

    object ToLifecycleState : EventMapper<Lifecycle.State>() {
        private val filterEventType = FilterEventType(Event.OnLifecycle.StateChange::class.java)

        override fun mapToData(event: Event): Maybe<Lifecycle.State> = filterEventType.mapToData(event).map { it.state }
    }

    object ToWebSocketEvent : EventMapper<WebSocket.Event>() {
        private val filterEventType = FilterEventType(Event.OnWebSocket.Event::class.java)

        override fun mapToData(event: Event): Maybe<WebSocket.Event> = filterEventType.mapToData(event).map { it.event }
    }

    object ToState : EventMapper<State>() {
        private val filterEventType = FilterEventType(Event.OnStateChange::class.java)

        override fun mapToData(event: Event): Maybe<State> = filterEventType.mapToData(event).map { it.state }
    }

    class ToDeserialization<T : Any>(
        private val messageAdapter: MessageAdapter<T>
    ) : EventMapper<Deserialization<T>>() {
        private val toWebSocketEvent = ToWebSocketEvent

        override fun mapToData(event: Event): Maybe<Deserialization<T>> = toWebSocketEvent.mapToData(event)
            .filter { it is WebSocket.Event.OnMessageReceived }
            .map { (it as WebSocket.Event.OnMessageReceived).message.deserialize() }

        private fun Message.deserialize(): Deserialization<T> = try {
            Deserialization.Success(messageAdapter.fromMessage(this))
        } catch (throwable: Throwable) {
            Deserialization.Error(throwable)
        }
    }

    class ToDeserializedValue<T : Any>(
        private val toDeserialization: ToDeserialization<T>
    ) : EventMapper<T>() {
        override fun mapToData(event: Event): Maybe<T> = toDeserialization.mapToData(event)
            .filter { it is Deserialization.Success }
            .map { (it as Deserialization.Success).value }
    }

    class Factory(
        private val messageAdapterResolver: MessageAdapterResolver
    ) {

        private val toDeserializationCache = mutableMapOf<MessageAdapter<Any>, ToDeserialization<*>>()

        fun create(returnType: ParameterizedType, annotations: Array<Annotation>): EventMapper<*> {
            val receivingClazz = returnType.getFirstTypeArgument().getRawType()

            if (receivingClazz == Event::class.java) {
                return NoOp
            }
            require(!Event::class.java.isAssignableFrom(receivingClazz)) {
                "Subclasses of Event is not supported"
            }
            if (Lifecycle.State::class.java == receivingClazz) {
                return ToLifecycleState
            }
            require(!Lifecycle.State::class.java.isAssignableFrom(receivingClazz)) {
                "Subclasses of Lifecycle.Event is not supported"
            }
            if (WebSocket.Event::class.java == receivingClazz) {
                return ToWebSocketEvent
            }
            require(!WebSocket.Event::class.java.isAssignableFrom(receivingClazz)) {
                "Subclasses of WebSocket.Event is not supported"
            }
            if (State::class.java == receivingClazz) {
                return ToState
            }
            require(!State::class.java.isAssignableFrom(receivingClazz)) {
                "Subclasses of State is not supported"
            }
            val messageAdapter = resolveMessageAdapter(returnType, annotations)
            val toDeserialization = createToDeserializationIfNeeded(messageAdapter)
            return when (receivingClazz) {
                Deserialization::class.java -> toDeserialization
                else -> ToDeserializedValue(toDeserialization)
            }
        }

        private fun createToDeserializationIfNeeded(messageAdapter: MessageAdapter<Any>): ToDeserialization<*> {
            if (toDeserializationCache.contains(messageAdapter)) {
                return toDeserializationCache[messageAdapter]!!
            }
            val toDeserialization = ToDeserialization(messageAdapter)
            toDeserializationCache[messageAdapter] = toDeserialization
            return toDeserialization
        }

        private fun resolveMessageAdapter(
            returnType: ParameterizedType,
            annotations: Array<Annotation>
        ): MessageAdapter<Any> {
            val receivingType = returnType.getFirstTypeArgument()
            val messageType = when (receivingType.getRawType()) {
                Deserialization::class.java -> (receivingType as ParameterizedType).getFirstTypeArgument()
                else -> receivingType
            }
            return messageAdapterResolver.resolve(messageType, annotations)
        }
    }

    companion object {
        private fun ParameterizedType.getFirstTypeArgument(): Type = getParameterUpperBound(0)
    }
}
