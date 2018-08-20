/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet

sealed class State {
    data class Opening internal constructor(
        val retryCount: Int,
        val clientOption: Any? = null
    ) : State()

    data class Opened internal constructor(
        val clientOption: Any? = null,
        val serverOption: Any? = null,
        val topics: Set<Topic>  = emptySet()
    ) : State()

    data class Closing internal constructor(
        val clientOption: Any? = null
    ) : State()

    data class Closed internal constructor(
        val clientOption: Any? = null,
        val serverOption: Any? = null
    ) : State()

    data class WillOpen internal constructor(
        val retryCount: Int
    ) : State()

    object Destroyed : State()
}
