/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet

sealed class SideEffect {
    data class ScheduleTimer(val retryCount: Int) : SideEffect()

    data class OpenConnection(val option: ClientOpenOption? = null) : SideEffect()
    data class CloseConnection(val option: ClientCloseOption? = null) : SideEffect()
    data class ForceCloseConnection(val option: ClientCloseOption? = null) : SideEffect()
    data class SendMessage(
        val topic: Topic,
        val message: Message,
        val clientMessageInfo: ClientMessageInfo? = null
    ) : SideEffect()

    data class Subscribe(val topic: Topic) : SideEffect()
    data class Unsubscribe(val topic: Topic) : SideEffect()

    // maybe side effect for the user?

}
