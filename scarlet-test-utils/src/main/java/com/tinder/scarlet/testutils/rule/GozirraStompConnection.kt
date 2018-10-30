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
import com.tinder.scarlet.stomp.GozirraStompClient
import com.tinder.scarlet.stomp.GozirraStompDestination
import com.tinder.scarlet.testutils.TestStreamObserver
import com.tinder.scarlet.testutils.any
import com.tinder.scarlet.testutils.test
import org.junit.rules.ExternalResource
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.util.logging.Level
import java.util.logging.Logger

class GozirraStompConnection<SERVICE : Any>(
    private val clazz: Class<SERVICE>,
    private val observeProtocolEvent: SERVICE.() -> Stream<ProtocolEvent>,
    private val clientConfiguration: Configuration
) : TestRule {

    val client: SERVICE
        get() = clientAndServer.client
    val clientProtocolEventObserver: TestStreamObserver<ProtocolEvent>
        get() = clientAndServer.clientProtocolEventObserver

    private val clientLifecycleRegistry = LifecycleRegistry()

    private val clientAndServer = ClientAndServer()

    override fun apply(base: Statement, description: Description): Statement {
        return clientAndServer
            .apply(base, description)
    }

    fun open() {
        clientLifecycleRegistry.onNext(LifecycleState.Started)
        blockUntilConnectionIsEstablish()
    }

    fun clientClosure() {
        clientLifecycleRegistry.onNext(LifecycleState.Stopped)
    }

    fun clientTerminate() {
        clientLifecycleRegistry.onNext(LifecycleState.Completed)
    }

    private fun blockUntilConnectionIsEstablish() {
        clientProtocolEventObserver.awaitValues(
            any<ProtocolEvent.OnOpened>()
        )
    }

    private inner class ClientAndServer : ExternalResource() {
        lateinit var client: SERVICE
        lateinit var clientProtocolEventObserver: TestStreamObserver<ProtocolEvent>

        override fun before() {
            createClientAndServer()
        }

        override fun after() {
            clientLifecycleRegistry.onNext(LifecycleState.Completed)
        }

        private fun createClientAndServer() {
            client = createClient()
            clientProtocolEventObserver = client.observeProtocolEvent().test()
            client.observeProtocolEvent().start(object : Stream.Observer<ProtocolEvent> {
                override fun onNext(data: ProtocolEvent) {
                    LOGGER.info("$this: client stomp event: $data")
                }

                override fun onError(throwable: Throwable) {
                    LOGGER.log(
                        Level.WARNING,
                        "$this: client stomp error",
                        throwable
                    )
                }

                override fun onComplete() {
                    LOGGER.info("client stomp completed")
                }
            })
        }

        private fun createClient(): SERVICE {
            val protocol = GozirraStompClient(
                GozirraStompClient.SimpleRequestFactory {
                    GozirraStompClient.ClientOpenRequest(
                        clientConfiguration.host,
                        clientConfiguration.port,
                        clientConfiguration.login,
                        clientConfiguration.password
                    )
                }
            )
            val configuration = Scarlet.Configuration(
                lifecycle = clientLifecycleRegistry,
                messageAdapterFactories = clientConfiguration.messageAdapterFactories,
                streamAdapterFactories = clientConfiguration.streamAdapterFactories,
                debug = true
            )
            val mainScarlet = Scarlet(protocol, configuration)
            return Scarlet(
                GozirraStompDestination(
                    "/queue/test",
                    GozirraStompDestination.SimpleRequestFactory {
                        emptyMap()
                    }
                ),
                configuration,
                mainScarlet
            )
                .create(clazz)
        }

    }

    data class Configuration(
        val host: String,
        val port: Int,
        val login: String,
        val password: String,
        val messageAdapterFactories: List<MessageAdapter.Factory> = emptyList(),
        val streamAdapterFactories: List<StreamAdapter.Factory> = emptyList()
    )

    companion object {
        private val LOGGER =
            Logger.getLogger(GozirraStompConnection::class.java.name)

        inline fun <reified SERVICE : Any> create(
            noinline observeProtocolEvent: SERVICE.() -> Stream<ProtocolEvent>,
            clientConfiguration: Configuration
        ): GozirraStompConnection<SERVICE> {
            return GozirraStompConnection(
                SERVICE::class.java,
                observeProtocolEvent,
                clientConfiguration
            )
        }
    }
}