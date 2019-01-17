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
import com.tinder.scarlet.socketio.SocketIoEvent
import com.tinder.scarlet.socketio.client.SocketIoClient
import com.tinder.scarlet.socketio.mockserver.MockSocketIoServer
import com.tinder.scarlet.socketio.mockserver.SocketIoEventName
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
    private val observeSocketIoEvent: SERVICE.() -> Stream<SocketIoEvent>,
    private val serverConfiguration: Configuration,
    private val clientConfiguration: Configuration
) : TestRule {

    val client: SERVICE
        get() = clientAndServer.client
    val server: SERVICE
        get() = clientAndServer.server
    val clientSocketIoEventObserver: TestStreamObserver<SocketIoEvent>
        get() = clientAndServer.clientSocketIoEventObserver
    val serverSocketIoEventObserver: TestStreamObserver<SocketIoEvent>
        get() = clientAndServer.serverSocketIoEventObserver

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
        clientSocketIoEventObserver.awaitValues(
            any<SocketIoEvent.OnConnectionOpened>()
        )
        serverSocketIoEventObserver.awaitValues(
            any<SocketIoEvent.OnConnectionOpened>()
        )
    }

    private inner class ClientAndServer : ExternalResource() {
        lateinit var client: SERVICE
        lateinit var server: SERVICE
        lateinit var clientSocketIoEventObserver: TestStreamObserver<SocketIoEvent>
        lateinit var serverSocketIoEventObserver: TestStreamObserver<SocketIoEvent>

        override fun before() {
            createClientAndServer()
        }

        override fun after() {
            serverLifecycleRegistry.onNext(LifecycleState.Completed)
            clientLifecycleRegistry.onNext(LifecycleState.Completed)
        }

        private fun createClientAndServer() {
            server = createServer()
            serverSocketIoEventObserver = server.observeSocketIoEvent().test()
            client = createClient()
            clientSocketIoEventObserver = client.observeSocketIoEvent().test()
            server.observeSocketIoEvent().start(object : Stream.Observer<SocketIoEvent> {
                override fun onNext(data: SocketIoEvent) {
                    LOGGER.info("server socket io event: $data")
                }

                override fun onError(throwable: Throwable) {
                    LOGGER.log(
                        Level.WARNING,
                        "server protocol error",
                        throwable
                    )
                }

                override fun onComplete() {
                    LOGGER.info("server protocol completed")
                }
            })
            client.observeSocketIoEvent().start(object : Stream.Observer<SocketIoEvent> {
                override fun onNext(data: SocketIoEvent) {
                    LOGGER.info("client socket io event: $data")
                }

                override fun onError(throwable: Throwable) {
                    LOGGER.log(
                        Level.WARNING,
                        "client protocol error",
                        throwable
                    )
                }

                override fun onComplete() {
                    LOGGER.info("client protocol completed")
                }
            })
        }

        private fun createServer(): SERVICE {
            val config = com.corundumstudio.socketio.Configuration().apply {
                hostname = "localhost"
                port = portNumber
                firstDataTimeout = 300
                pingTimeout = 300
                upgradeTimeout = 300
            }
            val protocol = MockSocketIoServer(config)
            val configuration = Scarlet.Configuration(
                lifecycle = serverLifecycleRegistry,
                messageAdapterFactories = serverConfiguration.messageAdapterFactories,
                streamAdapterFactories = serverConfiguration.streamAdapterFactories,
                debug = true
            )
            val mainScarlet = Scarlet(protocol, configuration)
            return mainScarlet
                .child(
                    SocketIoEventName(serverConfiguration.eventName),
                    configuration
                )
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
            return mainScarlet
                .child(
                    com.tinder.scarlet.socketio.client.SocketIoEventName(clientConfiguration.eventName),
                    configuration
                )
                .create(clazz)
        }
    }

    data class Configuration(
        val eventName: String,
        val messageAdapterFactories: List<MessageAdapter.Factory> = emptyList(),
        val streamAdapterFactories: List<StreamAdapter.Factory> = emptyList()
    )

    companion object {
        private val LOGGER =
            Logger.getLogger(SocketIoConnection::class.java.name)

        var portNumber = 9092

        inline fun <reified SERVICE : Any> create(
            noinline observeSocketIoEvent: SERVICE.() -> Stream<SocketIoEvent>,
            serverConfiguration: Configuration,
            clientConfiguration: Configuration
        ): SocketIoConnection<SERVICE> {
            portNumber += 1
            return SocketIoConnection(
                SERVICE::class.java,
                observeSocketIoEvent,
                serverConfiguration,
                clientConfiguration
            )
        }
    }
}