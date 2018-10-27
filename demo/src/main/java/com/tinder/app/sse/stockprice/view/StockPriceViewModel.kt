package com.tinder.app.sse.stockprice.view

import androidx.lifecycle.LiveDataReactiveStreams
import androidx.lifecycle.ViewModel
import com.tinder.app.sse.stockprice.domain.StockPriceRepository

class StockPriceViewModel(
    private val repository: StockPriceRepository
) : ViewModel() {

    val marketSnapshot = LiveDataReactiveStreams.fromPublisher(repository.observeMarketSnapshot())

}