package com.tinder.scarlet.stomp.core

import com.tinder.scarlet.stomp.core.models.StompHeader

interface StompSender {

    fun convertAndSend(payload: ByteArray, destination: String, headers: StompHeader?): Boolean

    fun convertAndSend(payload: String, destination: String, headers: StompHeader?): Boolean
}