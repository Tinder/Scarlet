/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.app.sse.domain

data class MarketSnapshot(val stockPrices: List<StockPrice> = emptyList())
