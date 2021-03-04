/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.app.gdax.api.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
enum class ProductId(val text: String) {
    BTC_USD("BTC-USD"),
    ETH_USD("ETH-USD"),
    LTC_USD("LTC-USD")
}
