/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.app.socketio.chatroom.api

import com.tinder.scarlet.ProtocolEvent
import com.tinder.scarlet.StateTransition
import com.tinder.scarlet.ws.Receive
import io.reactivex.Flowable

interface ChatRoomService {
    @Receive
    fun observeStateTransition(): Flowable<StateTransition>

    @Receive
    fun observeProtocolEvent(): Flowable<ProtocolEvent>
}