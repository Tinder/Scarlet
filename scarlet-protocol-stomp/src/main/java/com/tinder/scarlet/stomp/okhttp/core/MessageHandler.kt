/*
 * © 2018 Match Group, LLC.
 */
package com.tinder.scarlet.stomp.okhttp.core

import com.tinder.scarlet.stomp.okhttp.models.StompMessage

interface MessageHandler {

    /**
     * Convert given raw data byte array to stomp message
     */
    fun handle(data: ByteArray): StompMessage?
}