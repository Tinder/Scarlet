/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.internal.statetransition

import com.tinder.scarlet.State
import com.tinder.scarlet.StateTransition
import com.tinder.scarlet.utils.getRawType
import java.lang.reflect.Type

internal class StateTransitionToStateAdapter :
    StateTransitionAdapter<Any> {

    override fun adapt(stateTransition: StateTransition): Any? {
        return stateTransition.toState
    }

    class Factory : StateTransitionAdapter.Factory {
        override fun create(
            type: Type,
            annotations: Array<Annotation>
        ): StateTransitionAdapter<Any>? {
            val clazz = type.getRawType()
            if (clazz != State::class.java) {
                check(!State::class.java.isAssignableFrom(clazz))
                return null
            }
            return StateTransitionToStateAdapter()
        }
    }
}