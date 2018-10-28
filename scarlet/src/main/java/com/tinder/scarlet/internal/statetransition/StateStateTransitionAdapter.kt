/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.internal.statetransition

import com.tinder.scarlet.State
import com.tinder.scarlet.StateTransition
import com.tinder.scarlet.utils.getRawType
import java.lang.reflect.Type

internal class StateStateTransitionAdapter :
    StateTransitionAdapter<Any> {
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