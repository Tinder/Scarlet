/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.internal.statetransition

import com.tinder.scarlet.StateTransition
import java.lang.reflect.Type

internal interface StateTransitionAdapter<T : Any> {

    fun adapt(stateTransition: StateTransition): T?

    interface Factory {
        // TODO introduce a result type that indicates a type should not be supported
        fun create(type: Type, annotations: Array<Annotation>): StateTransitionAdapter<Any>
    }
}
