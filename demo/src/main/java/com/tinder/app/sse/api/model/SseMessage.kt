/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.app.sse.api.model

import com.squareup.moshi.Json

data class SseMessage(
    val event: Event,
    val data: String
) {

    enum class Event {
        @Json(name = "data")
        DATA,
        @Json(name = "patch")
        PATCH
    }
}
