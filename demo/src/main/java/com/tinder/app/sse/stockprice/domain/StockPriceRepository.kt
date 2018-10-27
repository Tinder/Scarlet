/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.app.sse.stockprice.domain

import io.reactivex.Flowable

interface StockPriceRepository {
    fun observeMarketSnapshot(): Flowable<MarketSnapshot>
}
