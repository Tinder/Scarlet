/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.internal.statetransition

import com.tinder.scarlet.Event
import com.tinder.scarlet.LifecycleState
import com.tinder.scarlet.StateTransition
import com.tinder.scarlet.utils.getRawType
import java.lang.reflect.Type

internal class LifecycleStateTransitionAdapter :
    StateTransitionAdapter<Any> {
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