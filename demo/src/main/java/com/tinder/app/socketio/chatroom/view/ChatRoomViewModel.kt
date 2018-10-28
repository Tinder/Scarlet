package com.tinder.app.socketio.chatroom.view

import androidx.lifecycle.LiveData
import androidx.lifecycle.LiveDataReactiveStreams
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tinder.app.socketio.chatroom.domain.ChatMessageRepository
import com.tinder.app.socketio.chatroom.domain.model.ChatMessage

class ChatRoomViewModel(
    private val chatMessageRepository: ChatMessageRepository
) : ViewModel() {

    val chatMessages: LiveData<List<ChatMessage>> =
        LiveDataReactiveStreams.fromPublisher(chatMessageRepository.observeChatMessage())

    val typingSignal = MutableLiveData<Boolean>()

    fun sendText(text: String) {
        chatMessageRepository.sendNewMessage(text)
    }

    fun handleTyping() {
        if (typingSignal.value == false) {
            chatMessageRepository.sendTypingStarted()
        }
        typingSignal.postValue(true)
    }

    fun handleTypingStopped() {
        if (typingSignal.value == true) {
            chatMessageRepository.sendTypingStopped()
        }
        typingSignal.postValue(false)
    }

}