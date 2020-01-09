package com.tinder.scarlet.stomp.core

typealias StompListener = (StompMessage) -> Unit

interface StompSubscriber {

    fun subscribe(destination: String, headers: StompHeader?, listener: StompListener)

    fun unsubscribe(destination: String)

}