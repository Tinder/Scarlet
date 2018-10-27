package com.tinder.app.socketio.chatroom.view

import androidx.lifecycle.LiveData
import androidx.lifecycle.LiveDataReactiveStreams
import androidx.lifecycle.ViewModel
import com.tinder.app.socketio.chatroom.domain.ChatMessageRepository
import com.tinder.app.socketio.chatroom.domain.model.ChatMessage

class ChatRoomViewModel(
    private val chatMessageRepository: ChatMessageRepository
) : ViewModel() {

    val chatMessages: LiveData<List<ChatMessage>>
        get() = LiveDataReactiveStreams.fromPublisher(chatMessageRepository.observeChatMessage())

    fun sendText(text: String) {
        chatMessageRepository.addNewMessage(text)
    }

}