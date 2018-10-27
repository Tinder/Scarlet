package com.tinder.app.websocket.echo.koin

import com.tinder.app.websocket.echo.api.BitmapMessageAdapter
import com.tinder.app.websocket.echo.api.EchoService
import com.tinder.app.websocket.echo.domain.AuthStatusRepository
import com.tinder.app.websocket.echo.domain.ChatMessageRepository
import com.tinder.app.websocket.echo.domain.LoggedInLifecycle
import com.tinder.app.websocket.echo.view.EchoBotViewModel
import com.tinder.scarlet.Scarlet
import com.tinder.scarlet.lifecycle.android.AndroidLifecycle
import com.tinder.scarlet.streamadapter.rxjava2.RxJava2StreamAdapterFactory
import com.tinder.scarlet.websocket.ShutdownReason
import com.tinder.scarlet.websocket.okhttp.OkHttpWebSocket
import okhttp3.Request
import org.koin.androidx.viewmodel.ext.koin.viewModel
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
                { OkHttpWebSocket.OpenRequest(Request.Builder().url("wss://demos.kaazing.com/echo").build()) },
                { OkHttpWebSocket.CloseRequest(ShutdownReason.GRACEFUL) }
            )
        )
        val configuration = Scarlet.Configuration(
            lifecycle = get("login"),
            messageAdapterFactories = listOf(BitmapMessageAdapter.Factory()),
            streamAdapterFactories = listOf(RxJava2StreamAdapterFactory())
        )
        Scarlet.Factory()
            .create(protocol, configuration)
            .create<EchoService>()
    }

    single { ChatMessageRepository(get()) }

    single { AuthStatusRepository() }

    viewModel { EchoBotViewModel(get(), get()) }
}