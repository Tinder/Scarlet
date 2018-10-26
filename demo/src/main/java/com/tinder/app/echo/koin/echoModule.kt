package com.tinder.app.echo.koin

import com.tinder.app.echo.api.BitmapMessageAdapter
import com.tinder.app.echo.api.EchoService
import com.tinder.app.echo.domain.AuthStatusRepository
import com.tinder.app.echo.domain.ChatMessageRepository
import com.tinder.app.echo.domain.LoggedInLifecycle
import com.tinder.app.echo.presenter.EchoBotPresenter
import com.tinder.scarlet.Scarlet
import com.tinder.scarlet.lifecycle.android.AndroidLifecycle
import com.tinder.scarlet.streamadapter.rxjava2.RxJava2StreamAdapterFactory
import com.tinder.scarlet.websocket.ShutdownReason
import com.tinder.scarlet.websocket.okhttp.OkHttpWebSocket
import okhttp3.Request
import org.koin.dsl.module.module

val echoModule = module {

    factory("login") {
        AndroidLifecycle.ofApplicationForeground(get())
            .combineWith(LoggedInLifecycle(get()))
    }

    single {
        val protocol = OkHttpWebSocket(
            get(),
            OkHttpWebSocket.SimpleRequestFactory(
                { OkHttpWebSocket.OpenRequest(Request.Builder().url("ws://demos.kaazing.com/echo").build()) },
                { OkHttpWebSocket.CloseRequest(ShutdownReason.GRACEFUL) }
            )
        )
        val configuration = Scarlet.Configuration(
            protocol = protocol,
            lifecycle = get("login"),
            messageAdapterFactories = listOf(BitmapMessageAdapter.Factory()),
            streamAdapterFactories = listOf(RxJava2StreamAdapterFactory())
        )
        val scarlet = Scarlet.Factory().create(configuration)
        scarlet.create<EchoService>()
    }

    single { ChatMessageRepository(get()) }

    single { AuthStatusRepository() }

    factory { EchoBotPresenter(get(), get()) }
}