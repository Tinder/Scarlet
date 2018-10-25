/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.app.sse.inject

import android.app.Application
import com.squareup.moshi.KotlinJsonAdapterFactory
import com.squareup.moshi.Moshi
import com.tinder.app.gdax.api.MoshiAdapters
import com.tinder.app.sse.api.SseStockPriceRepository
import com.tinder.app.sse.api.StockMarketService
import com.tinder.app.sse.domain.StockPriceRepository
import com.tinder.app.sse.view.SseFragment
import com.tinder.scarlet.v2.Lifecycle
import com.tinder.scarlet.v2.Scarlet
import com.tinder.scarlet.lifecycle.android.v2.AndroidLifecycle
import com.tinder.scarlet.messageadapter.moshi.MoshiMessageAdapter
import com.tinder.scarlet.streamadapter.rxjava2.RxJava2StreamAdapterFactory
import com.tinder.scarlet.websocket.ShutdownReason
import com.tinder.scarlet.websocket.okhttp.OkHttpWebSocket
import dagger.Binds
import dagger.Component
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import okhttp3.Request

@SseScope
@Component(
    modules = [SseComponent.SseModule::class, SseComponent.SseAbstractModule::class],
    dependencies = [(SseComponent.Dependency::class)]
)
interface SseComponent {

    fun inject(sseFragment: SseFragment)

    interface Dependency {
        fun application(): Application
    }

    @Component.Builder
    interface Builder {
        fun dependency(dependency: SseComponent.Dependency): Builder

        fun build(): SseComponent
    }

    @Module
    class SseModule {
        @Provides
        @SseScope
        fun provideOkHttpClient(): OkHttpClient {
            return OkHttpClient.Builder().build()
        }

        @Provides
        @SseScope
        fun provideLifecycle(application: Application): Lifecycle {
            return AndroidLifecycle.ofApplicationForeground(application)
        }

        @Provides
        @SseScope
        fun provideSseService(client: OkHttpClient, lifecycle: Lifecycle): StockMarketService {
            val moshi = Moshi.Builder()
                .add(MoshiAdapters())
                .add(KotlinJsonAdapterFactory())
                .build()
            val protocol = OkHttpWebSocket(
                client,
                object : OkHttpWebSocket.RequestFactory {
                    override fun createOpenRequest(): OkHttpWebSocket.OpenRequest {
                        return OkHttpWebSocket.OpenRequest(Request.Builder().url(URL).build())
                    }

                    override fun createCloseRequest(): OkHttpWebSocket.CloseRequest {
                        return OkHttpWebSocket.CloseRequest(ShutdownReason.GRACEFUL)
                    }
                }
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

        companion object {
            private val URL = """https://streamdata.motwin.net/
                    |http://stockmarket.streamdata.io/prices
                    |?X-Sd-Token=ZTUxYjBiZGEtZjEzOS00MDExLWE5ODktYjAxOTI4ZDhkZDk5""".trimMargin()
        }
    }

    @Module
    abstract class SseAbstractModule {
        @Binds
        @SseScope
        abstract fun bindSockPriceRepository(
            sseStockPriceRepository: SseStockPriceRepository
        ): StockPriceRepository
    }

    interface ComponentProvider {
        val sseComponent: SseComponent
    }
}
