/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.app.socketio.chatroom.api

import com.tinder.scarlet.socketio.SocketIoEvent
import com.tinder.scarlet.ws.Receive
import com.tinder.scarlet.ws.Send
import io.reactivex.Flowable

interface AddUserTopic {
    @Receive
    fun observeSocketIoEvent(): Flowable<SocketIoEvent>

    @Send
    fun sendAddUser(username: String)
}