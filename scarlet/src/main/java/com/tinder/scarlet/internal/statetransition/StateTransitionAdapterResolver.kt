/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.internal.statetransition

import com.tinder.scarlet.internal.utils.TypeAnnotationsPair
import java.lang.reflect.Type

internal class StateTransitionAdapterResolver(
    private val stateTransitionAdapterFactories: List<StateTransitionAdapter.Factory>
) {

    private val stateTransitionAdapterCache =
        mutableMapOf<TypeAnnotationsPair, StateTransitionAdapter<Any>>()

    fun resolve(type: Type, annotations: Array<Annotation>): StateTransitionAdapter<Any> {
        val key = TypeAnnotationsPair(type, annotations)
        return stateTransitionAdapterCache.getOrPut(key) {
            findStateTransitionAdapter(
                type,
                annotations
            )
        }
    }

    private fun findStateTransitionAdapter(
        type: Type,
        annotations: Array<Annotation>
    ): StateTransitionAdapter<Any> {
        for (adapterFactory in stateTransitionAdapterFactories) {
            val adapter = adapterFactory.create(type, annotations)
            // This type is not supported by this adapter
            if (adapter != null) {
                return adapter
            }
        }
        throw IllegalStateException(
            "Cannot resolve state transition adapter for type: $type, annotations: $annotations."
        )
    }
}
