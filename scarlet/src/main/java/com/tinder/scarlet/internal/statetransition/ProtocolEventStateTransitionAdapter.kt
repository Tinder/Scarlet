package com.tinder.scarlet.internal.statetransition

import com.tinder.scarlet.Event
import com.tinder.scarlet.ProtocolEvent
import com.tinder.scarlet.StateTransition
import com.tinder.scarlet.utils.getRawType
import java.lang.reflect.Type

internal class ProtocolEventStateTransitionAdapter :
    StateTransitionAdapter<Any> {
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