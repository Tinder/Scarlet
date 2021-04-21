/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet

sealed class Event {
    data class OnLifecycleStateChange internal constructor(val lifecycleState: LifecycleState) :
        Event()

    data class OnProtocolEvent internal constructor(val protocolEvent: ProtocolEvent) : Event()

    object OnShouldConnect : Event()
}
