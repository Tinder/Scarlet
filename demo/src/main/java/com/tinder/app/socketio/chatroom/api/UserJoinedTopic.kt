/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.app.socketio.chatroom.api

import com.tinder.app.socketio.chatroom.api.model.UserCountUpdate
import com.tinder.scarlet.ws.Receive
import io.reactivex.Flowable

interface UserJoinedTopic {
    @Receive
    fun observeUserJoined(): Flowable<UserCountUpdate>
}