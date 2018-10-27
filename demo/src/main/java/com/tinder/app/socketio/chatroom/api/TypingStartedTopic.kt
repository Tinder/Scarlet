package com.tinder.app.socketio.chatroom.api

import com.tinder.app.socketio.chatroom.api.model.TypingStatusUpdate
import com.tinder.scarlet.ws.Receive
import com.tinder.scarlet.ws.Send
import io.reactivex.Flowable

interface TypingStartedTopic {
    @Receive
    fun observeTypingStarted(): Flowable<TypingStatusUpdate>

    @Send
    fun sendTypingStarted(typingStatusUpdate: TypingStatusUpdate)
}