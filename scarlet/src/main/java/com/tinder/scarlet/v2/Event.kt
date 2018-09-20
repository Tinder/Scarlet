/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.v2

sealed class Event {
    data class OnLifecycleStateChange internal constructor(val state: Lifecycle.State) : Event()

    data class OnProtocolEvent internal constructor(val event: Protocol.Event) : Event()

    object OnShouldConnect : Event()
}
