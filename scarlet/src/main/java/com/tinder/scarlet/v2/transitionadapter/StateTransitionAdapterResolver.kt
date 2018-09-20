/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.v2.transitionadapter

import com.tinder.scarlet.v2.StateTransition
import io.reactivex.exceptions.CompositeException
import java.lang.reflect.Type

class StateTransitionAdapterResolver(
    private val stateTransitionAdapterFactories: List<StateTransition.Adapter.Factory>
) {

    fun resolve(type: Type, annotations: Array<Annotation>): StateTransition.Adapter<Any> {
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
        throw IllegalStateException("Cannot resolve state transition adapter for type $type.", compositeException)
    }
}
