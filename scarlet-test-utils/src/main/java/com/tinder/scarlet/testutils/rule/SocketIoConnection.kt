/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.testutils.rule

import com.tinder.scarlet.LifecycleState
import com.tinder.scarlet.MessageAdapter
import com.tinder.scarlet.ProtocolEvent
import com.tinder.scarlet.Scarlet
import com.tinder.scarlet.Stream
import com.tinder.scarlet.StreamAdapter
import com.tinder.scarlet.lifecycle.LifecycleRegistry
import com.tinder.scarlet.socketio.client.SocketIoClient
import com.tinder.scarlet.socketio.server.MockSocketIoServer
import com.tinder.scarlet.socketio.server.SocketIoEventName
import com.tinder.scarlet.testutils.TestStreamObserver
import com.tinder.scarlet.testutils.any
import com.tinder.scarlet.testutils.test
import org.junit.rules.ExternalResource
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.util.logging.Level
import java.util.logging.Logger

class SocketIoConnection<SERVICE : Any>(
    private val clazz: Class<SERVICE>,
    private val observeProtocolEvent: SERVICE.() -> Stream<ProtocolEvent>,
    private val serverConfiguration: Configuration,
    private val clientConfiguration: Configuration
) : TestRule {

    val client: SERVICE
        get() = clientAndServer.client
    val server: SERVICE
        get() = clientAndServer.server
    val clientProtocolEventObserver: TestStreamObserver<ProtocolEvent>
        get() = clientAndServer.clientProtocolEventObserver
    val serverProtocolEventObserver: TestStreamObserver<ProtocolEvent>
        get() = clientAndServer.serverProtocolEventObserver

    private val serverUrlString by lazy { "http://localhost:$portNumber" }
    private val serverLifecycleRegistry = LifecycleRegistry()
    private val clientLifecycleRegistry = LifecycleRegistry()

    private val clientAndServer = ClientAndServer()

    override fun apply(base: Statement, description: Description): Statement {
        return clientAndServer
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
        clientProtocolEventObserver.awaitValues(
            any<ProtocolEvent.OnOpened>()
        )
        serverProtocolEventObserver.awaitValues(
            any<ProtocolEvent.OnOpened>()
        )
    }

    private inner class ClientAndServer : ExternalResource() {
        lateinit var client: SERVICE
        lateinit var server: SERVICE
        lateinit var clientProtocolEventObserver: TestStreamObserver<ProtocolEvent>
        lateinit var serverProtocolEventObserver: TestStreamObserver<ProtocolEvent>

        override fun before() {
            createClientAndServer()
        }

        override fun after() {
            serverLifecycleRegistry.onNext(LifecycleState.Completed)
            clientLifecycleRegistry.onNext(LifecycleState.Completed)
        }

        private fun createClientAndServer() {
            server = createServer()
            serverProtocolEventObserver = server.observeProtocolEvent().test()
            client = createClient()
            clientProtocolEventObserver = client.observeProtocolEvent().test()
            server.observeProtocolEvent().start(object : Stream.Observer<ProtocolEvent> {
                override fun onNext(data: ProtocolEvent) {
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
            client.observeProtocolEvent().start(object : Stream.Observer<ProtocolEvent> {
                override fun onNext(data: ProtocolEvent) {
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
            val config = com.corundumstudio.socketio.Configuration().apply {
                setHostname("localhost")
                setPort(portNumber)
            }
            val protocol = MockSocketIoServer(config)
            val configuration = Scarlet.Configuration(
                lifecycle = serverLifecycleRegistry,
                messageAdapterFactories = serverConfiguration.messageAdapterFactories,
                streamAdapterFactories = serverConfiguration.streamAdapterFactories,
                debug = true
            )
            val mainScarlet = Scarlet(protocol, configuration)
            return Scarlet(SocketIoEventName("a"), configuration, mainScarlet)
                .create(clazz)
        }

        private fun createClient(): SERVICE {
            val protocol = SocketIoClient(
                { serverUrlString }
            )
            val configuration = Scarlet.Configuration(
                lifecycle = clientLifecycleRegistry,
                messageAdapterFactories = clientConfiguration.messageAdapterFactories,
                streamAdapterFactories = clientConfiguration.streamAdapterFactories,
                debug = true
            )
            val mainScarlet = Scarlet(protocol, configuration)
            return Scarlet(
                com.tinder.scarlet.socketio.client.SocketIoEventName("a"),
                configuration,
                mainScarlet
            )
                .create(clazz)
        }

    }

    data class Configuration(
        val messageAdapterFactories: List<MessageAdapter.Factory> = emptyList(),
        val streamAdapterFactories: List<StreamAdapter.Factory> = emptyList()
    )

    companion object {
        private val LOGGER =
            Logger.getLogger(SocketIoConnection::class.java.name)

        var portNumber = 9092

        inline fun <reified SERVICE : Any> create(
            noinline observeProtocolEvent: SERVICE.() -> Stream<ProtocolEvent>,
            serverConfiguration: Configuration = Configuration(),
            clientConfiguration: Configuration = Configuration()
        ): SocketIoConnection<SERVICE> {
            portNumber += 1
            return SocketIoConnection(
                SERVICE::class.java,
                observeProtocolEvent,
                serverConfiguration,
                clientConfiguration
            )
        }
    }
}