/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.app.gdax.api.model

import com.squareup.moshi.Json

data class Subscribe(
    val type: Type,
    @Json(name = "product_ids")
    val productIds: List<ProductId>,
    val channels: List<Channel>
) {
    enum class Type(val text: String) {
        SUBSCRIBE("subscribe"),
        UNSUBSCRIBE("unsubscribe")
    }

    enum class Channel(val text: String) {
        TICKER("ticker"),
        LEVEL2("level2"),
        HEARTBEAT("heartbeat")
    }
}
