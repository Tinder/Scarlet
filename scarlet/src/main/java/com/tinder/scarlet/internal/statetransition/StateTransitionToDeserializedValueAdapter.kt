/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.internal.statetransition

import com.tinder.scarlet.Event
import com.tinder.scarlet.MessageAdapter
import com.tinder.scarlet.ProtocolEvent
import com.tinder.scarlet.StateTransition
import com.tinder.scarlet.internal.utils.MessageAdapterResolver
import java.lang.reflect.Type

internal class StateTransitionToDeserializedValueAdapter(
    private val messageAdapter: MessageAdapter<Any>
) : StateTransitionAdapter<Any> {
    override fun adapt(stateTransition: StateTransition): Any? {
        val event = stateTransition.event as? Event.OnProtocolEvent ?: return null
        val protocolEvent = event.protocolEvent as? ProtocolEvent.OnMessageReceived ?: return null
        return try {
            messageAdapter.fromMessage(protocolEvent.message)
        } catch (throwable: Throwable) {
            null
        }
    }

    class Factory(
        private val messageAdapterResolver: MessageAdapterResolver
    ) : StateTransitionAdapter.Factory {
        override fun create(
            type: Type,
            annotations: Array<Annotation>
        ): StateTransitionAdapter<Any>? {
            return try {
                val messageAdapter = messageAdapterResolver.resolve(type, annotations)
                StateTransitionToDeserializedValueAdapter(
                    messageAdapter
                )
            } catch (throwable: Throwable) {
                null
            }
        }
    }
}
