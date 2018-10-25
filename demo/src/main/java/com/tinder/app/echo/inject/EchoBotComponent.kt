/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.app.echo.inject

import android.app.Application
import com.tinder.app.echo.api.BitmapMessageAdapter
import com.tinder.app.echo.api.EchoService
import com.tinder.app.echo.domain.LoggedInLifecycle
import com.tinder.app.echo.view.EchoBotFragment
import com.tinder.scarlet.v2.Lifecycle
import com.tinder.scarlet.v2.Scarlet
import com.tinder.scarlet.lifecycle.android.v2.AndroidLifecycle
import com.tinder.scarlet.streamadapter.rxjava2.RxJava2StreamAdapterFactory
import com.tinder.scarlet.websocket.ShutdownReason
import com.tinder.scarlet.websocket.okhttp.OkHttpWebSocket
import com.tinder.scarlet.websocket.okhttp.newWebSocketFactory
import dagger.Component
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor

@EchoBotScope
@Component(modules = [(EchoBotComponent.EchoBotModule::class)], dependencies = [(EchoBotComponent.Dependency::class)])
interface EchoBotComponent {

    fun inject(echoBotFragment: EchoBotFragment)

    interface Dependency {
        fun application(): Application
    }

    @Component.Builder
    interface Builder {
        fun dependency(dependency: Dependency): Builder

        fun build(): EchoBotComponent
    }

    @Module
    class EchoBotModule {
        @Provides
        @EchoBotScope
        fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
            .build()

        @Provides
        @EchoBotScope
        fun provideLifecycle(application: Application, loggedInLifecycle: LoggedInLifecycle): Lifecycle =
            AndroidLifecycle.ofApplicationForeground(application)
                .combineWith(loggedInLifecycle)

        @Provides
        @EchoBotScope
        fun provideEchoService(client: OkHttpClient, lifecycle: Lifecycle): EchoService {
            val protocol = OkHttpWebSocket(
                client,
                object : OkHttpWebSocket.RequestFactory {
                    override fun createOpenRequest(): OkHttpWebSocket.OpenRequest {
                        return OkHttpWebSocket.OpenRequest(Request.Builder().url("ws://demos.kaazing.com/echo").build())
                    }

                    override fun createCloseRequest(): OkHttpWebSocket.CloseRequest {
                        return OkHttpWebSocket.CloseRequest(ShutdownReason.GRACEFUL)
                    }
                }
            )
            val configuration = Scarlet.Configuration(
                protocol = protocol,
                lifecycle = lifecycle,
                messageAdapterFactories = listOf(BitmapMessageAdapter.Factory()),
                streamAdapterFactories = listOf(RxJava2StreamAdapterFactory())
            )
            val scarlet = Scarlet.Factory().create(configuration)
            return scarlet.create()
        }
    }

    interface ComponentProvider {
        val echoBotComponent: EchoBotComponent
    }
}
