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
import com.tinder.scarlet.sse.EventSourceEvent
import com.tinder.scarlet.sse.OkHttpEventSource
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

class OkHttpSseConnection<SERVICE : Any>(
    private val clazz: Class<SERVICE>,
    private val observeEventSourceEvent: SERVICE.() -> Stream<EventSourceEvent>,
    private val clientConfiguration: Configuration
) : TestRule {

    val client: SERVICE
        get() = clientAndServer.client
    val clientEventSourceEventObserver: TestStreamObserver<EventSourceEvent>
        get() = clientAndServer.clientEventSourceEventObserver

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
        clientEventSourceEventObserver.awaitValuesIncluding(
            any<EventSourceEvent.OnConnectionOpened>()
        )
    }

    private inner class ClientAndServer : ExternalResource() {
        lateinit var client: SERVICE
        lateinit var clientEventSourceEventObserver: TestStreamObserver<EventSourceEvent>

        override fun before() {
            createClientAndServer()
        }

        override fun after() {
            clientLifecycleRegistry.onNext(LifecycleState.Completed)
        }

        private fun createClientAndServer() {
            client = createClient()
            clientEventSourceEventObserver = client.observeEventSourceEvent().test()
            client.observeEventSourceEvent().start(object : Stream.Observer<EventSourceEvent> {
                override fun onNext(data: EventSourceEvent) {
                    LOGGER.info("$this: client sse event: $data")
                }

                override fun onError(throwable: Throwable) {
                    LOGGER.log(
                        Level.WARNING,
                        "$this: client sse error",
                        throwable
                    )
                }

                override fun onComplete() {
                    LOGGER.info("client sse completed")
                }
            })
        }

        private fun createClient(): SERVICE {
            val protocol = OkHttpEventSource(
                createOkHttpClient(),
                OkHttpEventSource.SimpleRequestFactory {
                    Request.Builder().url(clientConfiguration.serverUrl()).build()
                }
            )
            val configuration = Scarlet.Configuration(
                lifecycle = clientLifecycleRegistry,
                messageAdapterFactories = clientConfiguration.messageAdapterFactories,
                streamAdapterFactories = clientConfiguration.streamAdapterFactories,
                debug = true
            )
            return Scarlet(protocol, configuration)
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
        val serverUrl: () -> String,
        val messageAdapterFactories: List<MessageAdapter.Factory> = emptyList(),
        val streamAdapterFactories: List<StreamAdapter.Factory> = emptyList()
    )

    companion object {
        private val LOGGER =
            Logger.getLogger(OkHttpSseConnection::class.java.name)

        inline fun <reified SERVICE : Any> create(
            noinline observeEventSourceEvent: SERVICE.() -> Stream<EventSourceEvent>,
            clientConfiguration: Configuration
        ): OkHttpSseConnection<SERVICE> {
            return OkHttpSseConnection(
                SERVICE::class.java,
                observeEventSourceEvent,
                clientConfiguration
            )
        }
    }
}