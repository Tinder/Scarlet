/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.v2

data class StateTransition(
    val fromState: State,
    val event: Event,
    val toState: State,
    val sideEffect: SideEffect?
) {

}
