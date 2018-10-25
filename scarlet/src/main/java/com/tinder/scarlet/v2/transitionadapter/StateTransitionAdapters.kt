/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.v2.transitionadapter

import com.tinder.scarlet.Deserialization
import com.tinder.scarlet.MessageAdapter
import com.tinder.scarlet.internal.servicemethod.MessageAdapterResolver
import com.tinder.scarlet.utils.getParameterUpperBound
import com.tinder.scarlet.utils.getRawType
import com.tinder.scarlet.v2.StateTransitionAdapter
import com.tinder.scarlet.v2.Event
import com.tinder.scarlet.v2.LifecycleState
import com.tinder.scarlet.v2.ProtocolEvent
import com.tinder.scarlet.v2.ProtocolEventAdapter
import com.tinder.scarlet.v2.ProtocolSpecificEvent
import com.tinder.scarlet.v2.State
import com.tinder.scarlet.v2.StateTransition
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

internal class NoOpStateTransitionAdapter : StateTransitionAdapter<Any> {
    override fun adapt(stateTransition: StateTransition): StateTransition? {
        return stateTransition
    }

    class Factory : StateTransitionAdapter.Factory {
        override fun create(
            type: Type,
            annotations: Array<Annotation>
        ): StateTransitionAdapter<Any> {
            val clazz = type.getRawType()
            require(clazz == StateTransition::class.java)
            return NoOpStateTransitionAdapter()
        }
    }
}

internal class EventStateTransitionAdapter : StateTransitionAdapter<Any> {
    override fun adapt(stateTransition: StateTransition): Any? {
        return stateTransition.event
    }

    class Factory : StateTransitionAdapter.Factory {
        override fun create(
            type: Type,
            annotations: Array<Annotation>
        ): StateTransitionAdapter<Any> {
            val clazz = type.getRawType()
            require(!Event::class.java.isAssignableFrom(clazz)) {
                "Subclasses of Event is not supported"
            }
            require(clazz == Event::class.java)
            return EventStateTransitionAdapter()
        }
    }
}

internal class StateStateTransitionAdapter : StateTransitionAdapter<Any> {
    override fun adapt(stateTransition: StateTransition): Any? {
        return stateTransition.toState
    }

    class Factory : StateTransitionAdapter.Factory {
        override fun create(
            type: Type,
            annotations: Array<Annotation>
        ): StateTransitionAdapter<Any> {
            val clazz = type.getRawType()
            require(!State::class.java.isAssignableFrom(clazz)) {
                "Subclasses of State is not supported"
            }
            require(clazz == State::class.java)
            return StateStateTransitionAdapter()
        }
    }
}

internal class ProtocolEventStateTransitionAdapter : StateTransitionAdapter<Any> {
    override fun adapt(stateTransition: StateTransition): Any? {
        val event = stateTransition.event as? Event.OnProtocolEvent ?: return null
        return event.protocolEvent
    }

    class Factory : StateTransitionAdapter.Factory {
        override fun create(
            type: Type,
            annotations: Array<Annotation>
        ): StateTransitionAdapter<Any> {
            val clazz = type.getRawType()
            require(!ProtocolEvent::class.java.isAssignableFrom(clazz)) {
                "Subclasses of ProtocolEvent is not supported"
            }
            require(clazz == ProtocolEvent::class.java)
            return ProtocolEventStateTransitionAdapter()
        }
    }
}

internal class ProtocolSpecificEventStateTransitionAdapter(
    private val protocolEventAdatper: ProtocolEventAdapter
) : StateTransitionAdapter<Any> {
    override fun adapt(stateTransition: StateTransition): Any? {
        val event = stateTransition.event as? Event.OnProtocolEvent ?: return null

        return try {
            protocolEventAdatper.fromEvent(event.protocolEvent)
        } catch (throwable: Throwable) {
            null
        }
    }

    class Factory(
        private val protocolEventAdatperFactory: ProtocolEventAdapter.Factory
    ) : StateTransitionAdapter.Factory {
        override fun create(
            type: Type,
            annotations: Array<Annotation>
        ): StateTransitionAdapter<Any> {
            val clazz = type.getRawType()
            require(ProtocolSpecificEvent::class.java.isAssignableFrom(clazz))
            val protocolEventAdapter = protocolEventAdatperFactory.create(type, annotations)
            return ProtocolSpecificEventStateTransitionAdapter(protocolEventAdapter)
        }
    }
}

internal class LifecycleStateTransitionAdapter : StateTransitionAdapter<Any> {
    override fun adapt(stateTransition: StateTransition): Any? {
        val event = stateTransition.event as? Event.OnLifecycleStateChange ?: return null
        return event.lifecycleState
    }

    class Factory : StateTransitionAdapter.Factory {
        override fun create(
            type: Type,
            annotations: Array<Annotation>
        ): StateTransitionAdapter<Any> {
            val clazz = type.getRawType()
            require(!LifecycleState::class.java.isAssignableFrom(clazz)) {
                "Subclasses of LifecycleState is not supported"
            }
            require(clazz == LifecycleState::class.java)
            return LifecycleStateTransitionAdapter()
        }
    }
}

internal class DeserializationStateTransitionAdapter(
    private val messageAdapter: MessageAdapter<Any>
) : StateTransitionAdapter<Any> {
    override fun adapt(stateTransition: StateTransition): Any? {
        val event = stateTransition.event as? Event.OnProtocolEvent ?: return null
        val protocolEvent = event.protocolEvent as? ProtocolEvent.OnMessageReceived ?: return null
        return try {
            val deserializedValue = messageAdapter.fromMessage(protocolEvent.message)
            Deserialization.Success(deserializedValue)
        } catch (throwable: Throwable) {
            Deserialization.Error<Any>(throwable)
        }
    }

    class Factory(
        private val messageAdapterResolver: MessageAdapterResolver
    ) : StateTransitionAdapter.Factory {
        override fun create(
            type: Type,
            annotations: Array<Annotation>
        ): StateTransitionAdapter<Any> {
            val clazz = type.getRawType()
            require(!Deserialization::class.java.isAssignableFrom(clazz)) {
                "Subclasses of Deserialization is not supported"
            }
            require(clazz == Deserialization::class.java)
            val messageType = (type as ParameterizedType).getFirstTypeArgument()
            val messageAdapter = messageAdapterResolver.resolve(messageType, annotations)
            return DeserializationStateTransitionAdapter(messageAdapter)
        }
    }

    companion object {
        private fun ParameterizedType.getFirstTypeArgument(): Type = getParameterUpperBound(0)
    }
}

internal class DeserializedValueStateTransitionAdapter(
    private val messageAdapter: MessageAdapter<Any>
) : StateTransitionAdapter<Any> {
    override fun adapt(stateTransition: StateTransition): Any? {
        val event = stateTransition.event as? Event.OnProtocolEvent ?: return null
        val protocolEvent = event.protocolEvent as? ProtocolEvent.OnMessageReceived ?: return null
        return try {
            messageAdapter.fromMessage(protocolEvent.message)
        } catch (throwable: Throwable) {
            null
        }
    }

    class Factory(
        private val messageAdapterResolver: MessageAdapterResolver
    ) : StateTransitionAdapter.Factory {
        override fun create(
            type: Type,
            annotations: Array<Annotation>
        ): StateTransitionAdapter<Any> {
            val messageAdapter = messageAdapterResolver.resolve(type, annotations)
            return DeserializedValueStateTransitionAdapter(messageAdapter)
        }
    }
}
