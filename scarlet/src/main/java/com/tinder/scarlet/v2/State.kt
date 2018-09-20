/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.v2

sealed class State {
    data class WillConnect internal constructor(
        val retryCount: Int
    ) : State()

    data class Connecting internal constructor(
        val retryCount: Int
    ) : State()

    object Connected : State()

    object Disconnecting : State()

    object Disconnected : State()

    object Destroyed : State()
}
