/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.app.sse.stockprice.koin

import com.squareup.moshi.KotlinJsonAdapterFactory
import com.squareup.moshi.Moshi
import com.tinder.app.sse.stockprice.api.SseStockPriceRepository
import com.tinder.app.sse.stockprice.api.StockMarketService
import com.tinder.app.sse.stockprice.domain.StockPriceRepository
import com.tinder.app.sse.stockprice.view.StockPriceViewModel
import com.tinder.app.websocket.gdax.api.MoshiAdapters
import com.tinder.scarlet.Scarlet
import com.tinder.scarlet.messageadapter.moshi.MoshiMessageAdapter
import com.tinder.scarlet.sse.OkHttpEventSource
import com.tinder.scarlet.streamadapter.rxjava2.RxJava2StreamAdapterFactory
import okhttp3.Request
import org.koin.androidx.viewmodel.ext.koin.viewModel
import org.koin.dsl.module.module

val sseModule = module {

    single {
        val moshi = Moshi.Builder()
            .add(MoshiAdapters())
            .add(KotlinJsonAdapterFactory())
            .build()
        val protocol = OkHttpEventSource(
            get(),
            OkHttpEventSource.SimpleRequestFactory {
                Request.Builder().url(URL).build()
            }
        )
        val configuration = Scarlet.Configuration(
            lifecycle = get("foreground"),
            messageAdapterFactories = listOf(MoshiMessageAdapter.Factory(moshi)),
            streamAdapterFactories = listOf(RxJava2StreamAdapterFactory())
        )
        val scarlet = Scarlet(protocol, configuration)
        scarlet.create<StockMarketService>()
    }

    single { SseStockPriceRepository(get()) as StockPriceRepository }

    viewModel { StockPriceViewModel(get()) }
}

private val URL = """https://streamdata.motwin.net/
                    |http://stockmarket.streamdata.io/prices
                    |?X-Sd-Token=ZTUxYjBiZGEtZjEzOS00MDExLWE5ODktYjAxOTI4ZDhkZDk5""".trimMargin()
