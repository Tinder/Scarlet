/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.internal.statetransition

import com.tinder.scarlet.Event
import com.tinder.scarlet.StateTransition
import com.tinder.scarlet.utils.getRawType
import java.lang.reflect.Type

internal class EventStateTransitionAdapter :
    StateTransitionAdapter<Any> {
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