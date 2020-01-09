package com.tinder.scarlet.stomp.core

interface StompSender {

    fun convertAndSend(payload: String, destination: String, headers: StompHeader?): Boolean

}