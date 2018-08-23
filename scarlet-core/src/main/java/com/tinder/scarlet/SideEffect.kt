/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet

sealed class SideEffect {
    data class ScheduleConnection(val retryCount: Int) : SideEffect()

    data class OpenConnection(val option: Any? = null) : SideEffect()
    data class CloseConnection(val option: Any? = null) : SideEffect()
    data class ForceCloseConnection(val option: Any? = null) : SideEffect()
    data class SendMessage(
        val topic: Topic,
        val message: Message,
        val option: Any? = null
    ) : SideEffect()


    // maybe side effect for the user?

}
