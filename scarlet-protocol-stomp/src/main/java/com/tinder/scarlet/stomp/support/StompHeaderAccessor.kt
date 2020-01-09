package com.tinder.scarlet.stomp.support

import com.tinder.scarlet.stomp.core.StompHeader

class StompHeaderAccessor private constructor(headers: Map<String, String>) {

    companion object {

        fun of(
            headers: Map<String, String> = emptyMap()
        ): StompHeaderAccessor {
            return StompHeaderAccessor(headers)
        }

    }

    private val mutableHeaders = mutableMapOf<String, String>().apply {
        putAll(headers)
    }

    fun putAll(headers: Map<String, String>) {
        mutableHeaders.putAll(headers)
    }

    fun subscriptionId(subscriptionId: String) {
        mutableHeaders[StompHeader.ID] = subscriptionId
    }

    fun destination(destination: String) {
        mutableHeaders[StompHeader.DESTINATION] = destination
    }

    fun acceptVersion(acceptVersion: String) {
        mutableHeaders[StompHeader.ACCEPT_VERSION] = acceptVersion
    }

    fun contentType(contentType: String) {
        mutableHeaders[StompHeader.CONTENT_TYPE] = contentType
    }

    fun host(host: String) {
        mutableHeaders[StompHeader.HOST] = host
    }

    fun login(login: String) {
        mutableHeaders[StompHeader.LOGIN] = login
    }

    fun passcode(passcode: String) {
        mutableHeaders[StompHeader.PASSCODE] = passcode
    }

    fun createHeader() = StompHeader(mutableHeaders)

}