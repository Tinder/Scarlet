/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet

sealed class State {
    data class Opening internal constructor(
        val retryCount: Int,
        val clientOption: ClientOpenOption? = null
    ) : State()

    data class Opened internal constructor(
        val clientOption: ClientOpenOption? = null,
        val serverOption: ServerOpenOption? = null,
        val topics: Set<Topic>  = emptySet()
    ) : State()

    data class Closing internal constructor(
        val clientOption: ClientCloseOption? = null
    ) : State()

    data class Closed internal constructor(
        val clientOption: ClientCloseOption? = null,
        val serverOption: ServerCloseOption? = null
    ) : State()

    data class WaitingToRetry internal constructor(
        val retryCount: Int
    ) : State()

    object Destroyed : State()
}
