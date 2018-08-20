/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet

data class StateTransition(
    val from: State,
    val to: State,
    val event: Event,
    val sideEffect: SideEffect?
)
