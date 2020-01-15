package com.tinder.scarlet.stomp.okhttp

import com.tinder.scarlet.stomp.core.models.StompMessage

interface MessageHandler {

    /**
     * Convert given raw data byte array to stomp message
     */
    fun handle(data: ByteArray): StompMessage
}