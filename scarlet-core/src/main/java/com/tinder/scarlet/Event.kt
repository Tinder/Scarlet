/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet

sealed class Event {

    data class OnSendMethodCalled(
        val data: Any
    ) : Event()

    data class OnReceiveMethodCalled(
        // TODO this is important
        val dataMapper: Any
    ) : Event()

    // TODO where does this come from?
    data class OnTopicSubscriptionStarted(
        val topic: Topic
    ) : Event()

    data class OnTopicSubscriptionStopped(
        val topic: Topic
    ) : Event()

    // TODO get client start option
    object OnLifecycleStarted: Event()

    // TODO get client close option
    object OnLifecycleStopped: Event()

    object OnLifecycleDestroyed: Event()

    object OnTimerTick : Event()

    data class OnConnectionOpeningAcknowledged(
        val serverOption: ServerOpenOption
    ) : Event()

    data class OnConnectionClosingAcknowledged(
        val serverOption: ServerCloseOption
    ) : Event()

    data class OnConnectionFailed(
        val throwable: Throwable
    ) : Event()

    data class OnMessageReceived(val message: Message, val serverMessageInfo: ServerMessageInfo) : Event()

    data class OnMessageEnqueued(val message: Message, val clientMessageInfo: ClientMessageInfo) : Event()

    data class OnMessageSent(val message: Message, val clientMessageInfo: ClientMessageInfo) : Event()

    data class OnMessageDelivered(val message: Message, val clientMessageInfo: ClientMessageInfo) : Event()
}
