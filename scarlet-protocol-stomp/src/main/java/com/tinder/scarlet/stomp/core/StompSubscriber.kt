package com.tinder.scarlet.stomp.core

import com.tinder.scarlet.stomp.core.models.StompHeader
import com.tinder.scarlet.stomp.core.models.StompMessage

typealias StompListener = (StompMessage) -> Unit

interface StompSubscriber {

    /**
     *
     */
    fun subscribe(destination: String, headers: StompHeader?, listener: StompListener)

    /**
     *
     */
    fun unsubscribe(destination: String)
}