package com.carpa.simple_scarlet_websocket_kotlin.network

import android.app.Application
import com.tinder.scarlet.Scarlet
import com.tinder.scarlet.lifecycle.android.AndroidLifecycle
import com.tinder.scarlet.messageadapter.moshi.MoshiMessageAdapter
import com.tinder.scarlet.retry.ExponentialBackoffStrategy
import com.tinder.scarlet.streamadapter.rxjava2.RxJava2StreamAdapterFactory
import com.tinder.scarlet.websocket.okhttp.newWebSocketFactory
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Type in [BASE_URL] the url of your WebSocket (otherwise the app will crash or keep failing trying to connect)
 */
private const val BASE_URL = "wss://your_server.ngrok.io"

/**
 * We need this class to be instantiate just once throughout the app.
 * Singleton pattern.
 */
class MyWebSocketAPI constructor(app: Application) {
    companion object {
        @Volatile
        private var INSTANCE: MyWebSocketAPI? = null
        fun getInstance(app: Application) =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: MyWebSocketAPI(app).also {
                    INSTANCE = it
                }
            }
    }
    /**
     * Specify some information about our connection.
     * [okHttpClient] is necessary to create the WebSocket.
     */
    private val okHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()

    /**
     * Instance of Scarlet: it will help us create our service
     */
    private val scarlet =
        Scarlet.Builder()
            // If the connection fails how and when should we retry to reconnect? -> backoffStrategy
            .backoffStrategy(ExponentialBackoffStrategy(2000, 4000))
            // We can declare our adapter: it adapts our custom messages to Message.
            // In this app I use Msg (my custom simple class), just as an example.
            .addMessageAdapterFactory(MoshiMessageAdapter.Factory())
            // We can declare our adapter: it adapts our custom streams to Stream
            // In this app I use Flowable, just as an example.
            .addStreamAdapterFactory(RxJava2StreamAdapterFactory())
            // This actually creates the WebSocket: it is the only compulsory parameter!
            .webSocketFactory(okHttpClient.newWebSocketFactory(BASE_URL))
            // WebSocket will be automatically closed when app is not in foreground. We could combine it
            // with other life cycles to stop the WebSocket connection at some point (e.g. when a user logs out)
            // I have put an example of in the lifecycle package, even though I don't use it in this app. (feel free to try it)
            .lifecycle(AndroidLifecycle.ofApplicationForeground(app))
            .build()

    /**
     * We need to create this service just once
     */
    val socketService by lazy {
        scarlet.create(SocketService::class.java)
    }
}

