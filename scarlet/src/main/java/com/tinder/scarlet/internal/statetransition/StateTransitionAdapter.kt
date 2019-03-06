/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.internal.statetransition

import com.tinder.scarlet.StateTransition
import java.lang.reflect.Type

internal interface StateTransitionAdapter<T : Any> {

    fun adapt(stateTransition: StateTransition): T?

    interface Factory {
        fun create(type: Type, annotations: Array<Annotation>): StateTransitionAdapter<Any>?
    }
}
