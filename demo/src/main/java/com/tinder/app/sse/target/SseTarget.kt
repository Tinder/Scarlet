/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.app.sse.target

import com.tinder.app.sse.domain.MarketSnapshot

interface SseTarget {

    fun showMarketSnapshot(marketSnapshot: MarketSnapshot)
}
