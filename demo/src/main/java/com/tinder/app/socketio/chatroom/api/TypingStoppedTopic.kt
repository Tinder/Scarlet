/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.app.socketio.chatroom.api

import com.tinder.app.socketio.chatroom.api.model.TypingStatusUpdate
import com.tinder.scarlet.ws.Receive
import com.tinder.scarlet.ws.Send
import io.reactivex.Flowable

interface TypingStoppedTopic {
    @Receive
    fun observeTypingStopped(): Flowable<TypingStatusUpdate>

    @Send
    fun sendTypingStopped(typingStatusUpdate: TypingStatusUpdate)
}