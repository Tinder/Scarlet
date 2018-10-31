/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.app.socketio.chatroom.domain.model

import org.joda.time.DateTime

data class ChatMessage(
    val id: Int,
    val value: String,
    val source: Source,
    val timestamp: DateTime = DateTime.now()
) {

    sealed class Source {
        object Sent : Source()
        data class Received(val username: String) : Source()
    }
}
