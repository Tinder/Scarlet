/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.v2.transitionadapter

import com.tinder.scarlet.utils.getRawType
import com.tinder.scarlet.v2.Event
import com.tinder.scarlet.v2.ProtocolEvent
import com.tinder.scarlet.v2.StateTransition
import java.lang.reflect.Type

class NoOpStateTransitionAdapter : StateTransition.Adapter<Any> {
    override fun adapt(stateTransition: StateTransition): StateTransition? {
        return stateTransition
    }

    class Factory : StateTransition.Adapter.Factory {
        override fun create(type: Type, annotations: Array<Annotation>): StateTransition.Adapter<Any> {
            val clazz = type.getRawType()
            require(clazz == StateTransition::class.java)
            return NoOpStateTransitionAdapter()
        }
    }
}

class EventStateTransitionAdapter : StateTransition.Adapter<Any> {
    override fun adapt(stateTransition: StateTransition): Any? {
        return stateTransition.event
    }

    class Factory : StateTransition.Adapter.Factory {
        override fun create(type: Type, annotations: Array<Annotation>): StateTransition.Adapter<Any> {
            val clazz = type.getRawType()
            require(!Event::class.java.isAssignableFrom(clazz)) {
                "Subclasses of Event is not supported"
            }
            require(clazz == Event::class.java)
            return EventStateTransitionAdapter()
        }
    }
}

class ProtocolEventStateTransitionAdapter : StateTransition.Adapter<Any> {
    override fun adapt(stateTransition: StateTransition): Any? {
        val event = stateTransition.event as? Event.OnProtocolEvent ?: return null
        return event.event
    }

    class Factory : StateTransition.Adapter.Factory {
        override fun create(type: Type, annotations: Array<Annotation>): StateTransition.Adapter<Any> {
            val clazz = type.getRawType()
            require(!ProtocolEvent::class.java.isAssignableFrom(clazz)) {
                "Subclasses of Event is not supported"
            }
            require(clazz == ProtocolEvent::class.java)
            return ProtocolEventStateTransitionAdapter()
        }
    }
}

class LifecycleStateTransitionAdapter {
    override fun adapt(stateTransition: StateTransition): Any? {
        val event = stateTransition.event as? Event.OnLifecycleStateChange ?: return null
        return event.lifecycleState
    }

    class Factory : StateTransition.Adapter.Factory {
        override fun create(type: Type, annotations: Array<Annotation>): StateTransition.Adapter<Any> {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }

}

class StateStateTransitionAdapter {
    override fun adapt(stateTransition: StateTransition): Any? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    class Factory : StateTransition.Adapter.Factory {
        override fun create(type: Type, annotations: Array<Annotation>): StateTransition.Adapter<Any> {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }


}

class DeserializationStateTransitionAdapter {

}

class DeserializedValueStateTransitionAdapter {

}

