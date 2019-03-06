/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.internal.statetransition

import com.tinder.scarlet.Event
import com.tinder.scarlet.LifecycleState
import com.tinder.scarlet.StateTransition
import com.tinder.scarlet.utils.getRawType
import java.lang.reflect.Type

internal class StateTransitionToLifecycleStateAdapter :
    StateTransitionAdapter<Any> {

    override fun adapt(stateTransition: StateTransition): Any? {
        val event = stateTransition.event as? Event.OnLifecycleStateChange ?: return null
        return event.lifecycleState
    }

    class Factory : StateTransitionAdapter.Factory {
        override fun create(
            type: Type,
            annotations: Array<Annotation>
        ): StateTransitionAdapter<Any>? {
            val clazz = type.getRawType()
            if (clazz != LifecycleState::class.java) {
                check(!LifecycleState::class.java.isAssignableFrom(clazz))
                return null
            }
            return StateTransitionToLifecycleStateAdapter()
        }
    }
}