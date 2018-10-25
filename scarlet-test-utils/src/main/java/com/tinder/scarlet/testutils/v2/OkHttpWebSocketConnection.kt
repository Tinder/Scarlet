package com.tinder.scarlet.testutils.v2

import com.tinder.scarlet.MessageAdapter
import com.tinder.scarlet.Stream
import com.tinder.scarlet.StreamAdapter
import com.tinder.scarlet.testutils.TestStreamObserver
import com.tinder.scarlet.testutils.any
import com.tinder.scarlet.testutils.test
import com.tinder.scarlet.v2.LifecycleState
import com.tinder.scarlet.v2.Scarlet
import com.tinder.scarlet.v2.lifecycle.LifecycleRegistry
import com.tinder.scarlet.websocket.ShutdownReason
import com.tinder.scarlet.websocket.WebSocketEvent
import com.tinder.scarlet.websocket.mockwebserver.MockWebServerWebSocket
import com.tinder.scarlet.websocket.okhttp.OkHttpWebSocket
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockWebServer
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.util.concurrent.TimeUnit

class OkHttpWebSocketConnection<SERVICE : Any>(
    private val configuration: Configuration,
    private val clazz: Class<SERVICE>,
    private val observeWebSocketEvent: SERVICE.() -> Stream<WebSocketEvent>
) : TestRule {

    lateinit var client: SERVICE
    lateinit var server: SERVICE
    lateinit var clientWebSocketEventObserver: TestStreamObserver<WebSocketEvent>
    lateinit var serverWebSocketEventObserver: TestStreamObserver<WebSocketEvent>

    private val serverUrlString by lazy { mockWebServer.url("/").toString() }
    private val serverLifecycleRegistry = LifecycleRegistry()
    private val clientLifecycleRegistry = LifecycleRegistry()

    private val mockWebServer = MockWebServer()

    override fun apply(base: Statement, description: Description): Statement {
        return RuleChain.outerRule(mockWebServer)
            .around(object : ExternalResource() {
                override fun after() {
                    serverLifecycleRegistry.onNext(LifecycleState.Completed)
                    clientLifecycleRegistry.onNext(LifecycleState.Completed)
                }
            })
            .apply(base, description)
    }

    fun givenConnectionIsEstablished() {
        createClientAndServer()
        serverLifecycleRegistry.onNext(LifecycleState.Started)
        clientLifecycleRegistry.onNext(LifecycleState.Started)
        blockUntilConnectionIsEstablish()
    }

    private fun createClientAndServer() {
        server = createServer()
        serverWebSocketEventObserver = server.observeWebSocketEvent().test()
        client = createClient()
        clientWebSocketEventObserver = client.observeWebSocketEvent().test()
    }

    private fun createServer(): SERVICE {
        val protocol = MockWebServerWebSocket(
            mockWebServer,
            MockWebServerWebSocket.SimpleRequestFactory {
                OkHttpWebSocket.CloseRequest(configuration.serverShutdownReason)
            }
        )
        return Scarlet.Factory()
            .create(
                Scarlet.Configuration(
                    protocol = protocol,
                    lifecycle = serverLifecycleRegistry,
                    messageAdapterFactories = configuration.messageAdapterFactories,
                    streamAdapterFactories = configuration.streamAdapterFactories,
                    debug = true
                )
            )
            .create(clazz)
    }

    private fun createClient(): SERVICE {
        val protocol = OkHttpWebSocket(
            createOkHttpClient(),
            OkHttpWebSocket.SimpleRequestFactory(
                { OkHttpWebSocket.OpenRequest(Request.Builder().url(serverUrlString).build()) },
                { OkHttpWebSocket.CloseRequest(configuration.clientShutdownReason) })
        )
        val configuration = Scarlet.Configuration(
            protocol = protocol,
            lifecycle = clientLifecycleRegistry,
            messageAdapterFactories = configuration.messageAdapterFactories,
            streamAdapterFactories = configuration.streamAdapterFactories,
            debug = true
        )
        val scarlet = Scarlet.Factory().create(configuration)
        return scarlet.create(clazz)
    }

    private fun createOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .writeTimeout(500, TimeUnit.MILLISECONDS)
        .readTimeout(500, TimeUnit.MILLISECONDS)
        .build()

    private fun blockUntilConnectionIsEstablish() {
        clientWebSocketEventObserver.awaitValues(
            any<WebSocketEvent.OnConnectionOpened>()
        )
        serverWebSocketEventObserver.awaitValues(
            any<WebSocketEvent.OnConnectionOpened>()
        )
    }

    data class Configuration(
        val serverShutdownReason: ShutdownReason = ShutdownReason.GRACEFUL,
        val clientShutdownReason: ShutdownReason = ShutdownReason.GRACEFUL,
        val messageAdapterFactories: List<MessageAdapter.Factory> = emptyList(),
        val streamAdapterFactories: List<StreamAdapter.Factory> = emptyList()
    )

    companion object {
        inline fun <reified SERVICE : Any> create(
            configuration: Configuration,
            noinline observeWebSocketEvent: SERVICE.() -> Stream<WebSocketEvent>
        ): OkHttpWebSocketConnection<SERVICE> {
            return OkHttpWebSocketConnection(
                configuration,
                SERVICE::class.java,
                observeWebSocketEvent
            )
        }
    }
}