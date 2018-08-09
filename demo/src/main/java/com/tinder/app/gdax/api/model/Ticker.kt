/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.app.gdax.api.model

import com.squareup.moshi.Json

data class Ticker(
    val type: String,
    @Json(name = "product_id")
    val product: ProductId,
    val sequence: Long,
    val time: String,
    val price: String,
    val side: String,
    @Json(name = "last_size")
    val size: String,
    @Json(name = "best_bid")
    val bestBid: String,
    @Json(name = "best_ask")
    val bestAsk: String
)
