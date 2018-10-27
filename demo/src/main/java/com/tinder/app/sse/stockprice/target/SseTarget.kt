/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.app.sse.stockprice.target

import com.tinder.app.sse.stockprice.domain.MarketSnapshot

interface SseTarget {

    fun showMarketSnapshot(marketSnapshot: MarketSnapshot)
}
