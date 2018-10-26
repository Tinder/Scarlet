package com.tinder.app.gdax.koin

import com.squareup.moshi.KotlinJsonAdapterFactory
import com.squareup.moshi.Moshi
import com.tinder.app.gdax.api.GdaxService
import com.tinder.app.gdax.api.MoshiAdapters
import com.tinder.app.gdax.domain.TransactionRepository
import com.tinder.app.gdax.presenter.GdaxPresenter
import com.tinder.scarlet.Scarlet
import com.tinder.scarlet.messageadapter.moshi.MoshiMessageAdapter
import com.tinder.scarlet.streamadapter.rxjava2.RxJava2StreamAdapterFactory
import com.tinder.scarlet.websocket.ShutdownReason
import com.tinder.scarlet.websocket.okhttp.OkHttpWebSocket
import okhttp3.Request
import org.koin.dsl.module.module

val gdaxModule = module {

    single {
        val moshi = Moshi.Builder()
            .add(MoshiAdapters())
            .add(KotlinJsonAdapterFactory())
            .build()
        val protocol = OkHttpWebSocket(
            get(),
            OkHttpWebSocket.SimpleRequestFactory(
                { OkHttpWebSocket.OpenRequest(Request.Builder().url("wss://ws-feed.gdax.com").build()) },
                { OkHttpWebSocket.CloseRequest(ShutdownReason.GRACEFUL) }
            )
        )
        val configuration = Scarlet.Configuration(
            protocol = protocol,
            lifecycle = get("foreground"),
            messageAdapterFactories = listOf(MoshiMessageAdapter.Factory(moshi)),
            streamAdapterFactories = listOf(RxJava2StreamAdapterFactory())
        )
        val scarlet = Scarlet.Factory().create(configuration)
        scarlet.create< GdaxService>()
    }

    single { TransactionRepository(get()) }

    factory { GdaxPresenter(get()) }

}