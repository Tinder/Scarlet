/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet

data class StateTransition(
    val fromState: State,
    val event: Event,
    val toState: State,
    val sideEffect: SideEffect?
)
