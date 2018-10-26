package com.tinder.scarlet.internal.statetransition

import com.tinder.scarlet.Event
import com.tinder.scarlet.ProtocolEventAdapter
import com.tinder.scarlet.ProtocolSpecificEvent
import com.tinder.scarlet.StateTransition
import com.tinder.scarlet.utils.getRawType
import java.lang.reflect.Type

internal class ProtocolSpecificEventStateTransitionAdapter(
    private val protocolEventAdatper: ProtocolEventAdapter
) : StateTransitionAdapter<Any> {
    override fun adapt(stateTransition: StateTransition): Any? {
        val event = stateTransition.event as? Event.OnProtocolEvent ?: return null

        return try {
            protocolEventAdatper.fromEvent(event.protocolEvent)
        } catch (throwable: Throwable) {
            null
        }
    }

    class Factory(
        private val protocolEventAdatperFactory: ProtocolEventAdapter.Factory
    ) : StateTransitionAdapter.Factory {
        override fun create(
            type: Type,
            annotations: Array<Annotation>
        ): StateTransitionAdapter<Any> {
            val clazz = type.getRawType()
            require(ProtocolSpecificEvent::class.java.isAssignableFrom(clazz))
            val protocolEventAdapter = protocolEventAdatperFactory.create(type, annotations)
            return ProtocolSpecificEventStateTransitionAdapter(
                protocolEventAdapter
            )
        }
    }
}