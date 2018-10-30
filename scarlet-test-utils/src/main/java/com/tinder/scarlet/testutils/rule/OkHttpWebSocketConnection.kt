/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.testutils.rule

import com.tinder.scarlet.LifecycleState
import com.tinder.scarlet.MessageAdapter
import com.tinder.scarlet.Scarlet
import com.tinder.scarlet.Stream
import com.tinder.scarlet.StreamAdapter
import com.tinder.scarlet.lifecycle.LifecycleRegistry
import com.tinder.scarlet.testutils.TestStreamObserver
import com.tinder.scarlet.testutils.any
import com.tinder.scarlet.testutils.test
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
import java.util.logging.Level
import java.util.logging.Logger

class OkHttpWebSocketConnection<SERVICE : Any>(
    private val clazz: Class<SERVICE>,
    private val observeWebSocketEvent: SERVICE.() -> Stream<WebSocketEvent>,
    private val serverConfiguration: Configuration,
    private val clientConfiguration: Configuration
) : TestRule {

    val client: SERVICE
        get() = clientAndServer.client
    val server: SERVICE
        get() = clientAndServer.server
    val clientWebSocketEventObserver: TestStreamObserver<WebSocketEvent>
        get() = clientAndServer.clientWebSocketEventObserver
    val serverWebSocketEventObserver: TestStreamObserver<WebSocketEvent>
        get() = clientAndServer.serverWebSocketEventObserver

    private val serverUrlString by lazy { mockWebServer.url("/").toString() }
    private val serverLifecycleRegistry = LifecycleRegistry()
    private val clientLifecycleRegistry = LifecycleRegistry()

    private val mockWebServer = MockWebServer()
    private val clientAndServer = ClientAndServer()

    override fun apply(base: Statement, description: Description): Statement {
        return RuleChain.outerRule(mockWebServer)
            .around(clientAndServer)
            .apply(base, description)
    }

    fun open() {
        serverLifecycleRegistry.onNext(LifecycleState.Started)
        clientLifecycleRegistry.onNext(LifecycleState.Started)
        blockUntilConnectionIsEstablish()
    }

    fun clientClosure() {
        clientLifecycleRegistry.onNext(LifecycleState.Stopped)
    }

    fun clientTerminate() {
        clientLifecycleRegistry.onNext(LifecycleState.Completed)
    }

    fun serverClosure() {
        serverLifecycleRegistry.onNext(LifecycleState.Stopped)
    }

    fun serverTerminate() {
        clientLifecycleRegistry.onNext(LifecycleState.Completed)
    }

    private fun blockUntilConnectionIsEstablish() {
        clientWebSocketEventObserver.awaitValues(
            any<WebSocketEvent.OnConnectionOpened>()
        )
        serverWebSocketEventObserver.awaitValues(
            any<WebSocketEvent.OnConnectionOpened>()
        )
    }

    private inner class ClientAndServer : ExternalResource() {
        lateinit var client: SERVICE
        lateinit var server: SERVICE
        lateinit var clientWebSocketEventObserver: TestStreamObserver<WebSocketEvent>
        lateinit var serverWebSocketEventObserver: TestStreamObserver<WebSocketEvent>

        override fun before() {
            createClientAndServer()
        }

        override fun after() {
            clientLifecycleRegistry.onNext(LifecycleState.Completed)
        }

        private fun createClientAndServer() {
            server = createServer()
            serverWebSocketEventObserver = server.observeWebSocketEvent().test()
            client = createClient()
            clientWebSocketEventObserver = client.observeWebSocketEvent().test()
            server.observeWebSocketEvent().start(object : Stream.Observer<WebSocketEvent> {
                override fun onNext(data: WebSocketEvent) {
                    LOGGER.info("server webSocket event: $data")
                }

                override fun onError(throwable: Throwable) {
                    LOGGER.log(
                        Level.WARNING,
                        "server webSocket error",
                        throwable
                    )
                }

                override fun onComplete() {
                    LOGGER.info("server webSocket completed")
                }
            })
            client.observeWebSocketEvent().start(object : Stream.Observer<WebSocketEvent> {
                override fun onNext(data: WebSocketEvent) {
                    LOGGER.info("client webSocket event: $data")
                }

                override fun onError(throwable: Throwable) {
                    LOGGER.log(
                        Level.WARNING,
                        "client webSocket error",
                        throwable
                    )
                }

                override fun onComplete() {
                    LOGGER.info("client webSocket completed")
                }
            })
        }

        private fun createServer(): SERVICE {
            val protocol = MockWebServerWebSocket(
                mockWebServer,
                MockWebServerWebSocket.SimpleRequestFactory {
                    OkHttpWebSocket.CloseRequest(serverConfiguration.shutdownReason)
                }
            )
            val configuration = Scarlet.Configuration(
                lifecycle = serverLifecycleRegistry,
                messageAdapterFactories = serverConfiguration.messageAdapterFactories,
                streamAdapterFactories = serverConfiguration.streamAdapterFactories,
                debug = true
            )
            return Scarlet(protocol, configuration)
                .create(clazz)
        }

        private fun createClient(): SERVICE {
            val protocol = OkHttpWebSocket(
                createOkHttpClient(),
                OkHttpWebSocket.SimpleRequestFactory(
                    { OkHttpWebSocket.OpenRequest(Request.Builder().url(serverUrlString).build()) },
                    { OkHttpWebSocket.CloseRequest(clientConfiguration.shutdownReason) })
            )
            val configuration = Scarlet.Configuration(
                lifecycle = clientLifecycleRegistry,
                messageAdapterFactories = clientConfiguration.messageAdapterFactories,
                streamAdapterFactories = clientConfiguration.streamAdapterFactories,
                debug = true
            )
            val scarlet = Scarlet(protocol, configuration)
            return scarlet.create(clazz)
        }

        private fun createOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .writeTimeout(500, TimeUnit.MILLISECONDS)
            .readTimeout(500, TimeUnit.MILLISECONDS)
            .build()
    }

    data class Configuration(
        val shutdownReason: ShutdownReason = ShutdownReason.GRACEFUL,
        val messageAdapterFactories: List<MessageAdapter.Factory> = emptyList(),
        val streamAdapterFactories: List<StreamAdapter.Factory> = emptyList()
    )

    companion object {
        private val LOGGER =
            Logger.getLogger(OkHttpWebSocketConnection::class.java.name)

        inline fun <reified SERVICE : Any> create(
            noinline observeWebSocketEvent: SERVICE.() -> Stream<WebSocketEvent>,
            serverConfiguration: Configuration = Configuration(),
            clientConfiguration: Configuration = Configuration()
        ): OkHttpWebSocketConnection<SERVICE> {
            return OkHttpWebSocketConnection(
                SERVICE::class.java,
                observeWebSocketEvent,
                serverConfiguration,
                clientConfiguration
            )
        }
    }
}