/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.app.gdax.inject

import android.app.Application
import com.squareup.moshi.KotlinJsonAdapterFactory
import com.squareup.moshi.Moshi
import com.tinder.app.gdax.api.GdaxService
import com.tinder.app.gdax.api.MoshiAdapters
import com.tinder.app.gdax.view.GdaxFragment
import com.tinder.scarlet.Lifecycle
import com.tinder.scarlet.Scarlet
import com.tinder.scarlet.lifecycle.android.AndroidLifecycle
import com.tinder.scarlet.messageadapter.moshi.MoshiMessageAdapter
import com.tinder.scarlet.streamadapter.rxjava2.RxJava2StreamAdapterFactory
import com.tinder.scarlet.websocket.ShutdownReason
import com.tinder.scarlet.websocket.okhttp.OkHttpWebSocket
import dagger.Component
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor

@GdaxScope
@Component(
    modules = [(GdaxComponent.GdaxModule::class)],
    dependencies = [(GdaxComponent.Dependency::class)]
)
interface GdaxComponent {

    fun inject(gdaxFragment: GdaxFragment)

    interface Dependency {
        fun application(): Application
    }

    @Component.Builder
    interface Builder {
        fun dependency(dependency: Dependency): Builder

        fun build(): GdaxComponent
    }

    @Module
    class GdaxModule {
        @Provides
        @GdaxScope
        fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
            .build()

        @Provides
        @GdaxScope
        fun provideLifecycle(application: Application): Lifecycle {
            return AndroidLifecycle.ofApplicationForeground(application)
        }

        @Provides
        @GdaxScope
        fun provideGdaxService(client: OkHttpClient, lifecycle: Lifecycle): GdaxService {
            val moshi = Moshi.Builder()
                .add(MoshiAdapters())
                .add(KotlinJsonAdapterFactory())
                .build()
            val protocol = OkHttpWebSocket(
                client,
                OkHttpWebSocket.SimpleRequestFactory(
                    { OkHttpWebSocket.OpenRequest(Request.Builder().url("wss://ws-feed.gdax.com").build()) },
                    { OkHttpWebSocket.CloseRequest(ShutdownReason.GRACEFUL) }
                )
            )
            val configuration = Scarlet.Configuration(
                protocol = protocol,
                lifecycle = lifecycle,
                messageAdapterFactories = listOf(MoshiMessageAdapter.Factory(moshi)),
                streamAdapterFactories = listOf(RxJava2StreamAdapterFactory())
            )
            val scarlet = Scarlet.Factory().create(configuration)
            return scarlet.create()
        }
    }

    interface ComponentProvider {
        val gdaxComponent: GdaxComponent
    }
}
