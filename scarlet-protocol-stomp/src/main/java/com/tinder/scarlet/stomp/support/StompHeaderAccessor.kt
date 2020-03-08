package com.tinder.scarlet.stomp.support

import com.tinder.scarlet.stomp.core.models.StompHeader

/**
 * A base for classes providing strongly typed getters and setters as well as
 * behavior around specific categories of headers (e.g. STOMP headers).
 * Supports creating new headers, modifying existing headers,
 * or copying and modifying existing headers.
 */
class StompHeaderAccessor private constructor(headers: Map<String, String>) {

    companion object {

        /**
         * Custom header for the server to notify anything.
         */
        private const val STOMP_MESSAGE_HEADER = "message"

        fun of(headers: Map<String, String> = emptyMap()): StompHeaderAccessor =
            StompHeaderAccessor(headers)
    }

    private val mutableHeaders = mutableMapOf<String, String>().apply {
        putAll(headers)
    }

    operator fun set(key: String, value: String) {
        mutableHeaders[key] = value
    }

    var contentLength: Int?
        get() = try {
            mutableHeaders[StompHeader.CONTENT_LENGTH]?.toInt()
        } catch (ex: NumberFormatException) {
            null
        }
        set(value) {
            mutableHeaders[StompHeader.CONTENT_LENGTH] = value.toString()
        }

    fun heartBeat(sendInterval: Long, receiveInterval: Long) {
        mutableHeaders[StompHeader.HEARTBEAT] = "$sendInterval,$receiveInterval"
    }

    fun putAll(headers: Map<String, String>) {
        mutableHeaders.putAll(headers)
    }

    var subscriptionId: String?
        get() = mutableHeaders[StompHeader.ID]
        set(value) {
            if (value != null) {
                mutableHeaders[StompHeader.ID] = value
            }
        }

    var destination: String?
        get() = mutableHeaders[StompHeader.DESTINATION]
        set(value) {
            if (value != null) {
                mutableHeaders[StompHeader.DESTINATION] = value
            }
        }

    var acceptVersion: String?
        get() = mutableHeaders[StompHeader.ACCEPT_VERSION]
        set(value) {
            if (value != null) {
                mutableHeaders[StompHeader.ACCEPT_VERSION] = value
            }
        }

    var contentType: String?
        get() = mutableHeaders[StompHeader.CONTENT_TYPE]
        set(value) {
            if (value != null) {
                mutableHeaders[StompHeader.CONTENT_TYPE] = value
            }
        }

    var host: String?
        get() = mutableHeaders[StompHeader.HOST]
        set(value) {
            if (value != null) {
                mutableHeaders[StompHeader.HOST] = value
            }
        }

    var login: String?
        get() = mutableHeaders[StompHeader.LOGIN]
        set(value) {
            if (value != null) {
                mutableHeaders[StompHeader.LOGIN] = value
            }
        }

    var passcode: String?
        get() = mutableHeaders[StompHeader.PASSCODE]
        set(value) {
            if (value != null) {
                mutableHeaders[StompHeader.PASSCODE] = value
            }
        }

    fun message(message: String) {
        mutableHeaders[STOMP_MESSAGE_HEADER] = message
    }

    fun createHeader() = StompHeader(mutableHeaders)
}