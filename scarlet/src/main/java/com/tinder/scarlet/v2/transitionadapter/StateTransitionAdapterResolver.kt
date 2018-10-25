/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.v2.transitionadapter

import com.tinder.scarlet.internal.servicemethod.TypeAnnotationsPair
import com.tinder.scarlet.v2.StateTransitionAdapter
import io.reactivex.exceptions.CompositeException
import java.lang.reflect.Type

class StateTransitionAdapterResolver(
    private val stateTransitionAdapterFactories: List<StateTransitionAdapter.Factory>
) {

    private val stateTransitionAdapterCache = mutableMapOf<TypeAnnotationsPair, StateTransitionAdapter<Any>>()

    fun resolve(type: Type, annotations: Array<Annotation>): StateTransitionAdapter<Any> {
        val key = TypeAnnotationsPair(type, annotations)
        if (stateTransitionAdapterCache.contains(key)) {
            return stateTransitionAdapterCache[key]!!
        }
        val stateTransitionAdapter = findStateTransitionAdapter(type, annotations)
        stateTransitionAdapterCache[key] = stateTransitionAdapter
        return stateTransitionAdapter
    }

    private fun findStateTransitionAdapter(type: Type, annotations: Array<Annotation>): StateTransitionAdapter<Any> {
        val throwables = mutableListOf<Throwable>()
        for (adapterFactory in stateTransitionAdapterFactories) {
            try {
                return adapterFactory.create(type, annotations)
            } catch (e: Throwable) {
                // This type is not supported by this adapter
                throwables.add(e)
            }
        }
        val compositeException = CompositeException(*throwables.toTypedArray())
        throw IllegalStateException(
            "Cannot resolve state transition adapter for type: $type, annotations: $annotations.",
            compositeException
        )
    }
}
