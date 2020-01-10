package com.tinder.scarlet.stomp.okhttp

import com.tinder.scarlet.stomp.core.StompMessage

interface MessageHandler {

    /**
     * Convert given raw data string to stomp message
     */
    fun handle(data: String): StompMessage

}