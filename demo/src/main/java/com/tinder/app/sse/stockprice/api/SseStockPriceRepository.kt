/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.app.sse.stockprice.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.github.fge.jsonpatch.JsonPatch
import com.tinder.app.sse.stockprice.api.model.SseMessage
import com.tinder.app.sse.stockprice.domain.MarketSnapshot
import com.tinder.app.sse.stockprice.domain.StockPrice
import com.tinder.app.sse.stockprice.domain.StockPriceRepository
import com.tinder.scarlet.Message
import com.tinder.scarlet.sse.EventSourceEvent
import io.reactivex.Flowable
import io.reactivex.processors.BehaviorProcessor
import timber.log.Timber

class SseStockPriceRepository constructor(
    private val stockMarketService: StockMarketService
) : StockPriceRepository {

    private val markerSnapshotProcessor = BehaviorProcessor.create<MarketSnapshot>()

    private val mapper = ObjectMapper()
    private var data: ArrayNode = mapper.readTree("[]") as ArrayNode

    init {
        publishMarketSnapshot()

        stockMarketService.observeEventSourceEvent()
            .filter { it is EventSourceEvent.OnMessageReceived }
            .cast(EventSourceEvent.OnMessageReceived::class.java)
            .map {
                val textMessage = it.message as Message.Text
                val event = when (it.type) {
                    SseMessage.Event.DATA.stringValue -> SseMessage.Event.DATA
                    SseMessage.Event.PATCH.stringValue -> {
                        SseMessage.Event.PATCH
                    }
                    else -> SseMessage.Event.DATA
                }
                SseMessage(event, textMessage.value)
            }
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
        val stockPrices = data.map {
            StockPrice(
                it["title"].asText(),
                it["price"].asInt()
            )
        }
        markerSnapshotProcessor.onNext(MarketSnapshot(stockPrices))
    }
}
