package com.tinder.scarlet.stomp.support

import com.tinder.scarlet.stomp.core.StompHeader

class StompHeaderAccessor private constructor(headers: Map<String, String>) {

    companion object {

        private const val STOMP_MESSAGE_HEADER = "message"

        fun of(
            headers: Map<String, String> = emptyMap()
        ): StompHeaderAccessor {
            return StompHeaderAccessor(headers)
        }

    }

    private val mutableHeaders = mutableMapOf<String, String>().apply {
        putAll(headers)
    }

    operator fun set(key: String, value: String) {
        mutableHeaders[key] = value
    }

    fun heartBeat(sendInterval: Long, receiveInterval: Long) {
        mutableHeaders[StompHeader.HEARTBEAT] = "$sendInterval,$receiveInterval"
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

    fun message(message: String) {
        mutableHeaders[STOMP_MESSAGE_HEADER] = message
    }

    fun createHeader() = StompHeader(mutableHeaders)

}