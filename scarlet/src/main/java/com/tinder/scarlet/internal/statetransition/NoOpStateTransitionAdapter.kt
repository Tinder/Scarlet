/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.internal.statetransition

import com.tinder.scarlet.StateTransition
import com.tinder.scarlet.utils.getRawType
import java.lang.reflect.Type

internal class NoOpStateTransitionAdapter :
    StateTransitionAdapter<Any> {
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