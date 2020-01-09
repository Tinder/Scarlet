package com.tinder.scarlet.stomp.core

import com.tinder.scarlet.stomp.support.StompHeaderAccessor

class StompMessage private constructor(
    val command: StompCommand,
    val payload: String?,
    val header: StompHeader
) {

    class Builder {

        private var payload: String? = null
        private var headers: StompHeaderAccessor = StompHeaderAccessor.of()

        fun withPayload(payload: String): Builder {
            this.payload = payload
            return this
        }

        fun withHeaders(stompHeader: StompHeader): Builder {
            this.headers.putAll(stompHeader)
            return this
        }

        fun create(command: StompCommand): StompMessage {
            val createHeader = headers.createHeader()
            if (command.isDestinationRequired) check(!createHeader.destination.isNullOrEmpty()) { "Command $command required destination" }
            if (!command.isBodyAllowed) check(payload.isNullOrEmpty()) { "Command $command doesn't support body" }
            return StompMessage(command, payload, createHeader)
        }

    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as StompMessage

        if (command != other.command) return false
        if (payload != other.payload) return false
        if (header != other.header) return false

        return true
    }

    override fun hashCode(): Int {
        var result = command.hashCode()
        result = 31 * result + (payload?.hashCode() ?: 0)
        result = 31 * result + header.hashCode()
        return result
    }

}