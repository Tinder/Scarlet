/*
 * © 2013 - 2018 Tinder, Inc., ALL RIGHTS RESERVED
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
