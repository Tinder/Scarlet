/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.app.sse.domain

import io.reactivex.Flowable

interface StockPriceRepository {
    fun observeMarketSnapshot(): Flowable<MarketSnapshot>
}
