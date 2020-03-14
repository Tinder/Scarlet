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
import com.tinder.scarlet.stomp.okhttp.client.OkHttpStompClient
import com.tinder.scarlet.stomp.okhttp.client.OkHttpStompDestination
import com.tinder.scarlet.stomp.okhttp.client.OkHttpStompMainChannel
import com.tinder.scarlet.testutils.TestStreamObserver
import com.tinder.scarlet.testutils.any
import com.tinder.scarlet.testutils.test
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.rules.ExternalResource
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import java.util.logging.Logger

class OkHttpStompWebSocketConnection<SERVICE : Any>(
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
            val protocol = OkHttpStompClient(
                configuration = OkHttpStompMainChannel.Configuration(
                    host = "ws://${clientConfiguration.host}:${clientConfiguration.port}"
                ),
                okHttpClient = createOkHttpClient(),
                requestFactory = {
                    OkHttpStompClient.ClientOpenRequest(
                        passcode = clientConfiguration.password,
                        login = clientConfiguration.login,
                        okHttpRequest = Request.Builder().url(
                            "http://${clientConfiguration.host}:${clientConfiguration.port}"
                        ).build()
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
            return mainScarlet
                .child(
                    OkHttpStompDestination(clientConfiguration.destination),
                    configuration
                )
                .create(clazz)
        }

        private fun createOkHttpClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .writeTimeout(500, TimeUnit.MILLISECONDS)
                .readTimeout(500, TimeUnit.MILLISECONDS)
                .build()
        }
    }

    data class Configuration(
        val host: String,
        val port: Int,
        val login: String,
        val password: String,
        val destination: String,
        val messageAdapterFactories: List<MessageAdapter.Factory> = emptyList(),
        val streamAdapterFactories: List<StreamAdapter.Factory> = emptyList()
    )

    companion object {
        private val LOGGER = Logger.getLogger(OkHttpStompWebSocketConnection::class.java.name)

        inline fun <reified SERVICE : Any> create(
            noinline observeProtocolEvent: SERVICE.() -> Stream<ProtocolEvent>,
            clientConfiguration: Configuration
        ): OkHttpStompWebSocketConnection<SERVICE> {
            return OkHttpStompWebSocketConnection(
                SERVICE::class.java,
                observeProtocolEvent,
                clientConfiguration
            )
        }
    }
}