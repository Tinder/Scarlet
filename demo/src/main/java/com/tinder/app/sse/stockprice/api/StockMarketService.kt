/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.app.sse.stockprice.api

import com.tinder.scarlet.sse.EventSourceEvent
import com.tinder.scarlet.ws.Receive
import io.reactivex.Flowable

interface StockMarketService {
    @Receive
    fun observeEventSourceEvent(): Flowable<EventSourceEvent>
}
