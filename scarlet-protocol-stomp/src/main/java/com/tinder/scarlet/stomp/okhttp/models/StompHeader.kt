/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.stomp.okhttp.models

/**
 * Represents STOMP frame headers.
 */
class StompHeader(
    private val headers: Map<String, String>
) : Map<String, String> by headers {

    companion object {

        // Standard Headers
        /**
         * The implied text encoding for MIME types starting with text/ is UTF-8.
         * If you are using a text based MIME type with a different encoding then you SHOULD append ;charset=<encoding> to the MIME type.
         * For example, text/html;charset=utf-16 SHOULD be used if your sending an HTML body in UTF-16 encoding.
         */
        const val CONTENT_TYPE = "content-type"
        /**
         * This header is an octet count for the length of the message body.
         */
        const val CONTENT_LENGTH = "content-length"
        /**
         * This will cause the server to acknowledge the processing of the client frame with a RECEIPT frame
         */
        const val RECEIPT = "receipt"
        /**
         * The name of a virtual host that the client wishes to connect to.
         */
        const val HOST = "host"
        /**
         * The versions of the STOMP protocol the client supports.
         */
        const val ACCEPT_VERSION = "accept-version"
        /**
         * The user identifier used to authenticate against a secured STOMP server.
         */
        const val LOGIN = "login"
        /**
         * The password used to authenticate against a secured STOMP server.
         */
        const val PASSCODE = "passcode"
        /**
         * Heart-beating can optionally be used to test the healthiness of the underlying TCP
         * connection and to make sure that the remote end is alive and kicking.
         * When used, the heart-beat header MUST contain two positive integers separated by a comma.
         *
         * For detail information see <a href="https://stomp.github.io/stomp-specification-1.2.html#Heart-beating">https://stomp.github.io</a>
         */
        const val HEARTBEAT = "heart-beat"
        /**
         * A session identifier that uniquely identifies the session.
         */
        const val SESSION = "session"
        /**
         * A field that contains information about the STOMP server.
         * The field MUST contain a server-name field and MAY be followed by optional comment fields delimited by a space character.
         */
        const val SERVER = "server"

        /**
         * The header indicates where to send the message.
         */
        const val DESTINATION = "destination"
        /**
         * Since a single connection can have multiple open subscriptions with a server,
         * an id header MUST be included in the frame to uniquely identify the subscription.
         * The id header allows the client and server to relate subsequent MESSAGE or UNSUBSCRIBE frames to the original subscription.
         */
        const val ID = "id"
        /**
         * The valid values for the ack header are auto, client, or client-individual.
         * If the header is not set, it defaults to auto.
         *
         * For detail information see <a href="https://stomp.github.io/stomp-specification-1.2.html#SUBSCRIBE_ack_Header">https://stomp.github.io</a>
         */
        const val ACK = "ack"
        /**
         * The header matching the identifier of the subscription that is receiving the message.
         */
        const val SUBSCRIPTION = "subscription"
        /**
         * The header with a unique identifier for that message.
         */
        const val MESSAGE_ID = "message-id"
        /**
         * The header receipt-id, where the value is the value of the receipt header in the frame which this is a receipt for.
         */
        const val RECEIPT_ID = "receipt-id"
    }

    val destination: String?
        get() = headers[DESTINATION]

    /**
     * Get heartBeat in pair format where first value is sendInterval, second value is receiveInterval.
     */
    val heartBeat: Pair<Long, Long>
        get() {
            val heartBeat = headers[HEARTBEAT] ?: return 0L to 0L
            val (sendInterval, receiveInterval) = heartBeat.split(',')
            return sendInterval.toLong() to receiveInterval.toLong()
        }

    override fun toString(): String {
        return "StompHeader(headers=$headers)"
    }
}