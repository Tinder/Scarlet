package com.tinder.scarlet.streamadapter.coroutines

import com.tinder.scarlet.Lifecycle
import com.tinder.scarlet.Scarlet
import com.tinder.scarlet.WebSocket
import com.tinder.scarlet.lifecycle.LifecycleRegistry
import com.tinder.scarlet.testutils.TestStreamObserver
import com.tinder.scarlet.testutils.ValueAssert
import com.tinder.scarlet.testutils.any
import com.tinder.scarlet.websocket.mockwebserver.newWebSocketFactory
import com.tinder.scarlet.websocket.okhttp.newWebSocketFactory
import com.tinder.streamadapter.coroutines.CoroutinesStreamAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockWebServer
import org.junit.rules.ExternalResource
import java.util.concurrent.TimeUnit

abstract class ClientServerModel<T : Any>(private val clazz: Class<T>) : ExternalResource() {

    val mockWebServer = MockWebServer()
    private val serverUrlString by lazy { mockWebServer.url("/").toString() }

    private val serverLifecycleRegistry = LifecycleRegistry()
    private lateinit var server: T
    private lateinit var serverEventObserver: TestStreamObserver<WebSocket.Event>

    private val clientLifecycleRegistry = LifecycleRegistry()
    private lateinit var client: T
    private lateinit var clientEventObserver: TestStreamObserver<WebSocket.Event>

    internal fun givenConnectionIsEstablished(): Pair<T, T> {
        createClientAndServer()
        serverLifecycleRegistry.onNext(Lifecycle.State.Started)
        clientLifecycleRegistry.onNext(Lifecycle.State.Started)
        blockUntilConnectionIsEstablish()
        return client to server
    }

    internal fun createClientAndServer() {
        server = createServer()
        client = createClient()
        clientEventObserver = observeWebSocketEvents(client) // client.observeEvents().test()
        serverEventObserver = observeWebSocketEvents(server) // server.observeEvents().test()
    }
    abstract fun observeWebSocketEvents(service: T): TestStreamObserver<WebSocket.Event>
    internal fun createServer(): T {
        val webSocketFactory = mockWebServer.newWebSocketFactory()
        val scarlet = Scarlet.Builder()
            .webSocketFactory(webSocketFactory)
            .lifecycle(serverLifecycleRegistry)
            .addStreamAdapterFactory(CoroutinesStreamAdapterFactory())
            .build()
        return scarlet.create(clazz)
    }

    internal fun createClient(): T {
        val okHttpClient = OkHttpClient.Builder()
            .writeTimeout(500, TimeUnit.MILLISECONDS)
            .readTimeout(500, TimeUnit.MILLISECONDS)
            .build()
        val webSocketFactory = okHttpClient.newWebSocketFactory(serverUrlString)
        val scarlet = Scarlet.Builder()
            .webSocketFactory(webSocketFactory)
            .lifecycle(clientLifecycleRegistry)
            .addStreamAdapterFactory(CoroutinesStreamAdapterFactory())
            .build()
        return scarlet.create(clazz)
    }

    internal fun awaitValues(vararg values: ValueAssert<Any>) {
        serverEventObserver.awaitValues(*values)
    }

    internal fun blockUntilConnectionIsEstablish() {
        clientEventObserver.awaitValues(
            any<WebSocket.Event.OnConnectionOpened<*>>()
        )
        serverEventObserver.awaitValues(
            any<WebSocket.Event.OnConnectionOpened<*>>()
        )
    }

    override fun after() {
        mockWebServer.shutdown()
    }

    override fun before() {
        mockWebServer.start()
    }
}
