/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.app.sse.inject

import android.app.Application
import com.squareup.moshi.KotlinJsonAdapterFactory
import com.squareup.moshi.Moshi
import com.tinder.app.sse.api.SseStockPriceRepository
import com.tinder.app.sse.api.StockMarketService
import com.tinder.app.sse.domain.StockPriceRepository
import com.tinder.app.sse.view.SseFragment
import com.tinder.scarlet.Lifecycle
import com.tinder.scarlet.Scarlet
import com.tinder.scarlet.lifecycle.android.AndroidLifecycle
import com.tinder.scarlet.messageadapter.moshi.MoshiMessageAdapter
import com.tinder.scarlet.streamadapter.rxjava2.RxJava2StreamAdapterFactory
import com.tinder.scarlet.websocket.oksse.newSseWebSocketFactory
import dagger.Binds
import dagger.Component
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient

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
            return OkHttpClient.Builder()
//            .addInterceptor { chain ->
//                val request = chain.request().newBuilder()
//                    .addHeader("X-Sd-Token", "ZTUxYjBiZGEtZjEzOS00MDExLWE5ODktYjAxOTI4ZDhkZDk5")
//                    .build()
//                chain.proceed(request)
//            }
//                TODO: this will break the connection addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                .build()
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
                .add(KotlinJsonAdapterFactory())
                .build()
            val scarlet = Scarlet.Builder()
                .webSocketFactory(client.newSseWebSocketFactory(URL))
                .lifecycle(lifecycle)
                .addMessageAdapterFactory(MoshiMessageAdapter.Factory(moshi))
                .addStreamAdapterFactory(RxJava2StreamAdapterFactory())
                .build()
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
        abstract fun bindSockPriceRepository(sseStockPriceRepository: SseStockPriceRepository): StockPriceRepository
    }

    interface ComponentProvider {
        val sseComponent: SseComponent
    }
}
