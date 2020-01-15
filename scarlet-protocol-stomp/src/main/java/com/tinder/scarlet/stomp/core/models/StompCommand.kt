package com.tinder.scarlet.stomp.core.models

enum class StompCommand(
    val isBodyAllowed: Boolean = false,
    val isDestinationRequired: Boolean = false
) {
    // client
    CONNECT,
    DISCONNECT,
    SEND(isBodyAllowed = true, isDestinationRequired = true),
    SUBSCRIBE(isDestinationRequired = true),
    UNSUBSCRIBE,

    // server
    CONNECTED,
    MESSAGE(isBodyAllowed = true, isDestinationRequired = true),
    ERROR(isBodyAllowed = true),

    // heartbeat
    UNKNOWN
}