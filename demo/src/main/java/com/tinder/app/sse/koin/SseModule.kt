package com.tinder.app.sse.koin

import com.squareup.moshi.KotlinJsonAdapterFactory
import com.squareup.moshi.Moshi
import com.tinder.app.gdax.api.MoshiAdapters
import com.tinder.app.sse.api.SseStockPriceRepository
import com.tinder.app.sse.api.StockMarketService
import com.tinder.app.sse.domain.StockPriceRepository
import com.tinder.app.sse.presenter.SsePresenter
import com.tinder.scarlet.Scarlet
import com.tinder.scarlet.lifecycle.android.AndroidLifecycle
import com.tinder.scarlet.messageadapter.moshi.MoshiMessageAdapter
import com.tinder.scarlet.sse.OkHttpEventSource
import com.tinder.scarlet.streamadapter.rxjava2.RxJava2StreamAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.Request
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
                OkHttpEventSource.OpenRequest(
                    Request.Builder().url(URL).build()
                )
            }
        )
        val configuration = Scarlet.Configuration(
            protocol = protocol,
            lifecycle = get("foreground"),
            messageAdapterFactories = listOf(MoshiMessageAdapter.Factory(moshi)),
            streamAdapterFactories = listOf(RxJava2StreamAdapterFactory())
        )
        val scarlet = Scarlet.Factory().create(configuration)
        scarlet.create<StockMarketService>()
    }

    single { SseStockPriceRepository(get()) as StockPriceRepository }

    factory { SsePresenter(get()) }

}

private val URL = """https://streamdata.motwin.net/
                    |http://stockmarket.streamdata.io/prices
                    |?X-Sd-Token=ZTUxYjBiZGEtZjEzOS00MDExLWE5ODktYjAxOTI4ZDhkZDk5""".trimMargin()
