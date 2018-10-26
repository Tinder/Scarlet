/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet

sealed class State {
    data class WillConnect internal constructor(
        val retryCount: Int
    ) : State()

    data class Connecting internal constructor(
        val retryCount: Int
    ) : State()

    object Connected : State()

    data class Disconnecting(val forceClosed: Boolean) : State()

    object Disconnected : State()

    object Destroyed : State()
}
