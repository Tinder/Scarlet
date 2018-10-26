/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.app.sse.api.model

data class SseMessage(
    val event: Event,
    val data: String
) {

    enum class Event(val stringValue : String) {
        DATA("data"),
        PATCH("patch")
    }
}
