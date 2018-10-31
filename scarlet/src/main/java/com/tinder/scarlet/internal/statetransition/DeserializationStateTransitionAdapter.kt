/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.internal.statetransition

import com.tinder.scarlet.Deserialization
import com.tinder.scarlet.Event
import com.tinder.scarlet.MessageAdapter
import com.tinder.scarlet.ProtocolEvent
import com.tinder.scarlet.StateTransition
import com.tinder.scarlet.internal.utils.MessageAdapterResolver
import com.tinder.scarlet.utils.getParameterUpperBound
import com.tinder.scarlet.utils.getRawType
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

internal class DeserializationStateTransitionAdapter(
    private val messageAdapter: MessageAdapter<Any>
) : StateTransitionAdapter<Any> {
    override fun adapt(stateTransition: StateTransition): Any? {
        val event = stateTransition.event as? Event.OnProtocolEvent ?: return null
        val protocolEvent = event.protocolEvent as? ProtocolEvent.OnMessageReceived
            ?: return null
        val message = protocolEvent.message
        return try {
            val deserializedValue = messageAdapter.fromMessage(protocolEvent.message)
            Deserialization.Success(deserializedValue, message)
        } catch (throwable: Throwable) {
            Deserialization.Error<Any>(throwable, message)
        }
    }

    class Factory(
        private val messageAdapterResolver: MessageAdapterResolver
    ) : StateTransitionAdapter.Factory {
        override fun create(
            type: Type,
            annotations: Array<Annotation>
        ): StateTransitionAdapter<Any> {
            val clazz = type.getRawType()
            require(clazz == Deserialization::class.java)
            val messageType = (type as ParameterizedType).getFirstTypeArgument()
            val messageAdapter = messageAdapterResolver.resolve(messageType, annotations)
            return DeserializationStateTransitionAdapter(
                messageAdapter
            )
        }
    }

    companion object {
        private fun ParameterizedType.getFirstTypeArgument(): Type = getParameterUpperBound(0)
    }
}