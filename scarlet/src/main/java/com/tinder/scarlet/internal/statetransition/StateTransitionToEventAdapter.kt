/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.internal.statetransition

import com.tinder.scarlet.Event
import com.tinder.scarlet.StateTransition
import com.tinder.scarlet.utils.getRawType
import java.lang.reflect.Type

internal class StateTransitionToEventAdapter :
    StateTransitionAdapter<Any> {

    override fun adapt(stateTransition: StateTransition): Any? {
        return stateTransition.event
    }

    class Factory : StateTransitionAdapter.Factory {
        override fun create(
            type: Type,
            annotations: Array<Annotation>
        ): StateTransitionAdapter<Any>? {
            val clazz = type.getRawType()
            if (clazz != Event::class.java) {
                check(!Event::class.java.isAssignableFrom(clazz))
                return null
            }
            return StateTransitionToEventAdapter()
        }
    }
}