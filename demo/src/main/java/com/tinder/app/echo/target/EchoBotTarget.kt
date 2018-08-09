/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.app.echo.target

import com.tinder.app.echo.domain.ChatMessage

interface EchoBotTarget {
    fun setMessages(chatMessages: List<ChatMessage>)

    fun addMessage(chatMessage: ChatMessage)

    fun clearAllMessages()

    fun showLoggedIn()

    fun showLoggedOut()
}
