/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet

sealed class Event {
    object OnShouldOpenConnection : Event()

    data class OnShouldSendMessage(
        val topic: Topic,
        val message: Message
    ) : Event()

    data class OnShouldSubscribe(
        val topic: Topic
    ) : Event()

    data class OnShouldUnsubscribe(
        val topic: Topic
    ) : Event()

    data class OnTopicSubscribed(
        val topic: Topic
    ) : Event()

    data class OnTopicUnsubscribed(
        val topic: Topic
    ) : Event()


    object OnLifecycleStarted: Event()

    object OnLifecycleStopped: Event()

    object OnLifecycleDestroyed: Event()

    data class OnConnectionOpened(
        val clientOption: Any?,
        val serverOption: Any?
    ) : Event()

    data class OnConnectionClosed(
        val clientOption: Any?,
        val serverOption: Any?
    ) : Event()

    data class OnConnectionFailed(
        val throwable: Throwable
    ) : Event()

    data class OnMessageReceived(val message: Message, val option: Any?) : Event()

    data class OnMessageEnqueued(val message: Message, val option: Any?) : Event()

    data class OnMessageSent(val message: Message, val option: Any?) : Event()

    data class OnMessageDelivered(val message: Message, val option: Any?) : Event()
}
