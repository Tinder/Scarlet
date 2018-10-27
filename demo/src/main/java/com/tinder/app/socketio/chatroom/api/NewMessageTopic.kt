package com.tinder.app.socketio.chatroom.api

import com.tinder.app.socketio.chatroom.api.model.NewMessageUpdate
import com.tinder.scarlet.ws.Receive
import com.tinder.scarlet.ws.Send
import io.reactivex.Flowable

interface NewMessageTopic {
    @Receive
    fun observeNewMessage(): Flowable<NewMessageUpdate>

    @Send
    fun sendNewMessage(message: String)
}