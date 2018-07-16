/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.app.sse.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.github.fge.jsonpatch.JsonPatch
import com.tinder.app.sse.api.model.SseMessage
import com.tinder.app.sse.domain.MarketSnapshot
import com.tinder.app.sse.domain.StockPrice
import com.tinder.app.sse.domain.StockPriceRepository
import io.reactivex.Flowable
import io.reactivex.processors.BehaviorProcessor
import timber.log.Timber
import javax.inject.Inject

class SseStockPriceRepository @Inject constructor(
    private val stockMarketService: StockMarketService
) : StockPriceRepository {

    private val markerSnapshotProcessor = BehaviorProcessor.create<MarketSnapshot>()

    private val mapper = ObjectMapper()
    private var data: ArrayNode = mapper.readTree("[]") as ArrayNode

    init {
        publishMarketSnapshot()

        stockMarketService.observeMessage()
            .doOnNext { message ->
                data = when (message.event) {
                    SseMessage.Event.DATA -> mapper.readTree(message.data)
                    SseMessage.Event.PATCH -> {
                        val patchNode = mapper.readTree(message.data)
                        val patch = JsonPatch.fromJson(patchNode)
                        patch.apply(data)
                    }
                } as ArrayNode
            }
            .retry()
            .subscribe({
                publishMarketSnapshot()
            }, { e ->
                Timber.e(e)
            })
    }

    override fun observeMarketSnapshot(): Flowable<MarketSnapshot> {
        return markerSnapshotProcessor.hide()
    }

    private fun publishMarketSnapshot() {
        val stockPrices = data.map { StockPrice(
            it["title"].asText(),
            it["price"].asInt()
        ) }
        markerSnapshotProcessor.onNext(MarketSnapshot(stockPrices))
    }
}
