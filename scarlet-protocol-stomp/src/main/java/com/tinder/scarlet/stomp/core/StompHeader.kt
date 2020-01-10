package com.tinder.scarlet.stomp.core

class StompHeader(
    private val headers: Map<String, String>
) : Map<String, String> by headers {

    companion object {

        // Standard headers (as defined in the spec)
        const val CONTENT_TYPE = "content-type" // SEND, MESSAGE, ERROR

        const val CONTENT_LENGTH = "content-length" // SEND, MESSAGE, ERROR

        const val RECEIPT = "receipt" // any client frame other than CONNECT

        // CONNECT
        const val HOST = "host"

        const val ACCEPT_VERSION = "accept-version"

        const val LOGIN = "login"

        const val PASSCODE = "passcode"

        const val HEARTBEAT = "heart-beat"

        // CONNECTED

        const val SESSION = "session"

        const val SERVER = "server"

        // SEND

        const val DESTINATION = "destination"

        // SUBSCRIBE, UNSUBSCRIBE
        const val ID = "id"

        const val ACK = "ack"

        // MESSAGE
        const val SUBSCRIPTION = "subscription"

        const val MESSAGE_ID = "message-id"

        // RECEIPT
        const val RECEIPT_ID = "receipt-id"

    }

    val destination: String?
        get() = headers[DESTINATION]

    val heartBeat: Pair<Long, Long>
        get() {
            val heartBeat = headers[HEARTBEAT] ?: return 0L to 0L
            val (sendInterval, receiveInterval) = heartBeat.split(",")
            return sendInterval.toLong() to receiveInterval.toLong()
        }
}