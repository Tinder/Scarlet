/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.app.websocket.gdax.koin

import com.squareup.moshi.KotlinJsonAdapterFactory
import com.squareup.moshi.Moshi
import com.tinder.app.websocket.gdax.api.GdaxService
import com.tinder.app.websocket.gdax.api.MoshiAdapters
import com.tinder.app.websocket.gdax.domain.TransactionRepository
import com.tinder.app.websocket.gdax.view.GdaxViewModel
import com.tinder.scarlet.Scarlet
import com.tinder.scarlet.messageadapter.moshi.MoshiMessageAdapter
import com.tinder.scarlet.streamadapter.rxjava2.RxJava2StreamAdapterFactory
import com.tinder.scarlet.websocket.ShutdownReason
import com.tinder.scarlet.websocket.okhttp.OkHttpWebSocket
import okhttp3.Request
import org.koin.androidx.viewmodel.ext.koin.viewModel
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
            lifecycle = get("foreground"),
            messageAdapterFactories = listOf(MoshiMessageAdapter.Factory(moshi)),
            streamAdapterFactories = listOf(RxJava2StreamAdapterFactory())
        )
        Scarlet(protocol, configuration)
            .create<GdaxService>()
    }

    single { TransactionRepository(get()) }

    viewModel { GdaxViewModel(get()) }
}