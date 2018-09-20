/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.v2

import java.lang.reflect.Type

data class StateTransition(
    val fromState: State,
    val event: Event,
    val toState: State,
    val sideEffect: SideEffect?
) {

    interface Adapter<T : Any> {

        fun adapt(stateTransition: StateTransition): T?

        interface Factory {
            fun create(type: Type, annotations: Array<Annotation>): Adapter<Any>
        }
    }
}
