package com.carpa.simple_scarlet_websocket_kotlin.network

import com.squareup.moshi.Json

/**
 * Simple data class: what we want to send and to receive.
 */
data class Msg(
    @Json(name = "message") var message: String = ""
)