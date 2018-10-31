/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.app.sse.stockprice.domain

data class MarketSnapshot(val stockPrices: List<StockPrice> = emptyList())
