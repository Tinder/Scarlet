/*
 * © 2018 Match Group, LLC.
 */
package com.tinder.scarlet.stomp.okhttp.core

import com.tinder.scarlet.stomp.okhttp.models.StompHeader
import com.tinder.scarlet.stomp.okhttp.models.StompMessage

typealias StompListener = (StompMessage) -> Unit

/**
 * Interface use for subscribe and unsubscribe to STOMP queue.
 */
interface StompSubscriber {

    /**
     * Subscribe to given destination with headers.
     */
    fun subscribe(destination: String, headers: StompHeader?, listener: StompListener)

    /**
     * Unsubscribe from given destination.
     */
    fun unsubscribe(destination: String)
}