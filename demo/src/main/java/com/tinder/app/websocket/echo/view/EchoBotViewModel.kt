/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.app.websocket.echo.view

import androidx.lifecycle.LiveData
import androidx.lifecycle.LiveDataReactiveStreams
import androidx.lifecycle.ViewModel
import com.tinder.app.websocket.echo.domain.AuthStatus
import com.tinder.app.websocket.echo.domain.AuthStatusRepository
import com.tinder.app.websocket.echo.domain.ChatMessage
import com.tinder.app.websocket.echo.domain.ChatMessageRepository

class EchoBotViewModel(
    private val chatMessageRepository: ChatMessageRepository,
    private val authStatusRepository: AuthStatusRepository
) : ViewModel() {

    val chatMessages: LiveData<List<ChatMessage>>
        get() = LiveDataReactiveStreams.fromPublisher(chatMessageRepository.observeChatMessage())

    val authStatus: LiveData<AuthStatus>
        get() = LiveDataReactiveStreams.fromPublisher(authStatusRepository.observeAuthStatus())

    fun sendText(text: String) {
        chatMessageRepository.addTextMessage(text)
    }

    fun sendImage(imagePath: String) {
        chatMessageRepository.addImageMessage(imagePath, IMAGE_MAX_WIDTH)
    }

    fun clearAllMessages() {
        chatMessageRepository.clearAllMessages()
    }

    fun toggleAuthStatus() {
        authStatusRepository.toggleAuthStatus()
    }

    private companion object {
        private const val IMAGE_MAX_WIDTH = 200
    }
}
