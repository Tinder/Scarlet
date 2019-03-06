/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.internal.statetransition

import com.tinder.scarlet.Event
import com.tinder.scarlet.ProtocolSpecificEventAdapter
import com.tinder.scarlet.ProtocolSpecificEvent
import com.tinder.scarlet.StateTransition
import com.tinder.scarlet.utils.getRawType
import java.lang.reflect.Type

internal class StateTransitionToProtocolSpecificEventAdapter(
    private val protocolSpecificEventAdatper: ProtocolSpecificEventAdapter
) : StateTransitionAdapter<Any> {
    override fun adapt(stateTransition: StateTransition): Any? {
        val event = stateTransition.event as? Event.OnProtocolEvent ?: return null

        return try {
            protocolSpecificEventAdatper.fromEvent(event.protocolEvent)
        } catch (throwable: Throwable) {
            null
        }
    }

    class Factory(
        private val protocolSpecificEventAdatperFactory: ProtocolSpecificEventAdapter.Factory
    ) : StateTransitionAdapter.Factory {
        override fun create(
            type: Type,
            annotations: Array<Annotation>
        ): StateTransitionAdapter<Any>? {
            val clazz = type.getRawType()
            if (!ProtocolSpecificEvent::class.java.isAssignableFrom(clazz)) {
                return null
            }
            val protocolEventAdapter = protocolSpecificEventAdatperFactory.create(type, annotations)
            return StateTransitionToProtocolSpecificEventAdapter(
                protocolEventAdapter
            )
        }
    }
}