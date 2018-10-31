/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.app.echo.inject

import android.app.Application
import com.tinder.app.echo.api.BitmapMessageAdapter
import com.tinder.app.echo.api.EchoService
import com.tinder.app.echo.domain.LoggedInLifecycle
import com.tinder.app.echo.view.EchoBotFragment
import com.tinder.scarlet.Lifecycle
import com.tinder.scarlet.Scarlet
import com.tinder.scarlet.lifecycle.android.AndroidLifecycle
import com.tinder.scarlet.streamadapter.rxjava2.RxJava2StreamAdapterFactory
import com.tinder.scarlet.websocket.okhttp.newWebSocketFactory
import dagger.Component
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
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
            val scarlet = Scarlet.Builder()
                .webSocketFactory(client.newWebSocketFactory("wss://demos.kaazing.com/echo"))
                .lifecycle(lifecycle)
                .addMessageAdapterFactory(BitmapMessageAdapter.Factory())
                .addStreamAdapterFactory(RxJava2StreamAdapterFactory())
                .build()
            return scarlet.create()
        }
    }

    interface ComponentProvider {
        val echoBotComponent: EchoBotComponent
    }
}
