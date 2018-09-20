/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.v2

sealed class Event {
    data class OnLifecycleStateChange internal constructor(val lifecycleState: Lifecycle.State) : Event()

    data class OnProtocolEvent internal constructor(val protocolEvent: ProtocolEvent) : Event()

    object OnShouldConnect : Event()
}
