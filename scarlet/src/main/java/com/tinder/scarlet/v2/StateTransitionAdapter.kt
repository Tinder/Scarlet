/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.v2

import java.lang.reflect.Type

interface StateTransitionAdapter<T : Any> {

    fun adapt(stateTransition: StateTransition): T?

    interface Factory {
        fun create(type: Type, annotations: Array<Annotation>): StateTransitionAdapter<Any>
    }
}
