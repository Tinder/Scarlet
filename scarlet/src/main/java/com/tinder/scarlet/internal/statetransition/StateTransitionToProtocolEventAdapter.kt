/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.internal.statetransition

import com.tinder.scarlet.Event
import com.tinder.scarlet.ProtocolEvent
import com.tinder.scarlet.StateTransition
import com.tinder.scarlet.utils.getRawType
import java.lang.reflect.Type

internal class StateTransitionToProtocolEventAdapter : StateTransitionAdapter<Any> {

    override fun adapt(stateTransition: StateTransition): Any? {
        val event = stateTransition.event as? Event.OnProtocolEvent ?: return null
        return event.protocolEvent
    }

    class Factory : StateTransitionAdapter.Factory {
        override fun create(
            type: Type,
            annotations: Array<Annotation>
        ): StateTransitionAdapter<Any>? {
            val clazz = type.getRawType()
            if (clazz != ProtocolEvent::class.java) {
                check(!ProtocolEvent::class.java.isAssignableFrom(clazz))
                return null
            }
            return StateTransitionToProtocolEventAdapter()
        }
    }
}