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
import com.tinder.scarlet.mqtt.PahoMqttClient
import com.tinder.scarlet.mqtt.PahoMqttTopicFilter
import com.tinder.scarlet.testutils.TestStreamObserver
import com.tinder.scarlet.testutils.any
import com.tinder.scarlet.testutils.test
import org.eclipse.paho.client.mqttv3.MqttAsyncClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.junit.rules.ExternalResource
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.util.logging.Level
import java.util.logging.Logger

class PahoMqttConnection<SERVICE : Any>(
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
                    LOGGER.info("$this: client mqtt event: $data")
                }

                override fun onError(throwable: Throwable) {
                    LOGGER.log(
                        Level.WARNING,
                        "$this: client mqtt error",
                        throwable
                    )
                }

                override fun onComplete() {
                    LOGGER.info("client mqtt completed")
                }
            })
        }

        private fun createClient(): SERVICE {
            val sampleClient = MqttAsyncClient(
                clientConfiguration.brokerUrl,
                clientConfiguration.clientId,
                MemoryPersistence()
            )
            val connOpts = MqttConnectOptions()

            val protocol = PahoMqttClient(
                object : PahoMqttClient.MqttClientFactory {
                    override fun create(): MqttAsyncClient {
                        return sampleClient
                    }
                },
                object : PahoMqttClient.MqttConnectOptionsFactory {
                    override fun create(): MqttConnectOptions {
                        return connOpts
                    }
                }
            )
            val configuration = Scarlet.Configuration(
                lifecycle = clientLifecycleRegistry,
                messageAdapterFactories = clientConfiguration.messageAdapterFactories,
                streamAdapterFactories = clientConfiguration.streamAdapterFactories,
                // TODO fix this retrying with trampoline
                debug = false
            )
            val mainScarlet = Scarlet(protocol, configuration)
            val topicFilter = PahoMqttTopicFilter(
                clientConfiguration.topicFilter
            )
            return mainScarlet
                .child(
                    topicFilter,
                    configuration
                )
                .create(clazz)
        }
    }

    data class Configuration(
        val brokerUrl: String,
        val clientId: String,
        val topicFilter: String,
        val messageAdapterFactories: List<MessageAdapter.Factory> = emptyList(),
        val streamAdapterFactories: List<StreamAdapter.Factory> = emptyList()
    )

    companion object {
        private val LOGGER =
            Logger.getLogger(PahoMqttConnection::class.java.name)

        inline fun <reified SERVICE : Any> create(
            noinline observeProtocolEvent: SERVICE.() -> Stream<ProtocolEvent>,
            clientConfiguration: Configuration
        ): PahoMqttConnection<SERVICE> {
            return PahoMqttConnection(
                SERVICE::class.java,
                observeProtocolEvent,
                clientConfiguration
            )
        }
    }
}