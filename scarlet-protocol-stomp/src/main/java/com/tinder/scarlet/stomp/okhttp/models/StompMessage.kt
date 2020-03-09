/*
 * © 2018 Match Group, LLC.
 */
package com.tinder.scarlet.stomp.okhttp.models

import com.tinder.scarlet.stomp.okhttp.support.StompHeaderAccessor

/**
 * Stomp message representation with headers, payload and command.
 * @see StompCommand
 */
class StompMessage private constructor(
    val command: StompCommand,
    val payload: ByteArray,
    val headers: StompHeader
) {

    class Builder {

        private var payload: ByteArray = ByteArray(0)
        private var headers: StompHeaderAccessor = StompHeaderAccessor.of()

        fun withPayload(payload: ByteArray): Builder {
            this.payload = payload.copyOf()
            return this
        }

        fun withPayload(payload: String): Builder {
            this.payload = payload.toByteArray()
            return this
        }

        fun withHeaders(stompHeader: StompHeader): Builder {
            this.headers.putAll(stompHeader)
            return this
        }

        fun create(command: StompCommand): StompMessage {
            val createHeader = headers.createHeader()
            if (command.isDestinationRequired) check(!createHeader.destination.isNullOrEmpty()) { "Command $command required destination" }
            if (!command.isBodyAllowed) check(payload.isEmpty()) { "Command $command doesn't support body" }
            return StompMessage(
                command = command,
                payload = payload,
                headers = createHeader
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as StompMessage

        if (command != other.command) return false
        if (!payload.contentEquals(other.payload)) return false
        if (headers != other.headers) return false

        return true
    }

    override fun hashCode(): Int {
        var result = command.hashCode()
        result = 31 * result + payload.contentHashCode()
        result = 31 * result + headers.hashCode()
        return result
    }
}