package com.tinder.scarlet.mqtt

import com.tinder.scarlet.LifecycleState
import com.tinder.scarlet.ProtocolEvent
import com.tinder.scarlet.Scarlet
import com.tinder.scarlet.Stream
import com.tinder.scarlet.lifecycle.LifecycleRegistry
import com.tinder.scarlet.testutils.TestStreamObserver
import com.tinder.scarlet.testutils.test
import com.tinder.scarlet.ws.Receive
import com.tinder.scarlet.ws.Send
import org.apache.activemq.junit.EmbeddedActiveMQBroker
import org.apache.activemq.transport.stomp.Stomp
import org.apache.activemq.transport.stomp.StompConnection
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.logging.Level
import java.util.logging.Logger

// TODO mqtt sandboxes https://iot.eclipse.org/getting-started/#sandboxes


class PahoMqttClientIntegrationTest {
    @get:Rule
    val broker = object : EmbeddedActiveMQBroker() {
        override fun configure() {
            brokerService.addConnector("stomp://localhost:61613?trace=true")
            brokerService.addConnector("mqtt://localhost:1883")
        }
    }

    @Before
    fun before() {

    }

    @Test
    fun test() {
        val connection = StompConnection()
        connection.open("localhost", 61613)

        connection.connect("system", "manager")

        connection.begin("tx1")
        connection.send("/queue/test", "message1", "tx1", null)
        connection.send("/queue/test", "message2", "tx1", null)
        connection.commit("tx1")
        connection.subscribe("/queue/test", Stomp.Headers.Subscribe.AckModeValues.CLIENT)

        connection.begin("tx2")
        var message = connection.receive()
        LOGGER.info(message.getBody())
        connection.ack(message, "tx2")
        message = connection.receive()
        LOGGER.info(message.getBody())
        connection.ack(message, "tx2")
        connection.commit("tx2")
        connection.disconnect()
    }

    private lateinit var client: MqttService
    private lateinit var clientProtocolEventObserver: TestStreamObserver<ProtocolEvent>
    private val clientLifecycleRegistry = LifecycleRegistry()

    private lateinit var queueTestClient: MqttQueueTestService
    private val queueTestClientLifecycleRegistry = LifecycleRegistry()


    private fun createClientAndConnect() {
        createClients()
        clientProtocolEventObserver = client.observeProtocolEvent().test()
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

    private fun createClients() {
        val topic = "queue/test"
        val content = "Message from MqttPublishSample"
        val qos = 2
        val broker = "tcp://localhost:1883"
        val clientId = "JavaSample"
        val persistence = MemoryPersistence()

        val sampleClient1 = MqttClient(broker, clientId, persistence)
        val connOpts1 = MqttConnectOptions()
        connOpts1.isCleanSession = true
        LOGGER.info("Connecting to broker: $broker")
        sampleClient1.connect(connOpts1)
        LOGGER.info("Connected")
        LOGGER.info("Publishing message: $content")
        val message = MqttMessage(content.toByteArray())
        message.qos = qos
        sampleClient1.publish(topic, message)
        LOGGER.info("Message published")
        sampleClient1.disconnect()
        LOGGER.info("Disconnected")

        val sampleClient = MqttClient(broker, clientId, persistence)
        val connOpts = MqttConnectOptions()
        connOpts.isCleanSession = true
        LOGGER.info("Connecting to broker: $broker")
        sampleClient.connect(connOpts)
        LOGGER.info("Connected")
//        LOGGER.info("Publishing message: $content")
//        val message = MqttMessage(content.toByteArray())
//        message.qos = qos
//        sampleClient.publish(topic, message)
//        LOGGER.info("Message published")

        sampleClient.disconnect()
        LOGGER.info("Disconnected")

        val protocol = PahoMqttClient(
            object : PahoMqttClient.MqttClientFactory {
                override fun create(): MqttClient {
                    return sampleClient
                }
            },
            object : PahoMqttClient.MqttConnectOptionsFactory {
                override fun create(): MqttConnectOptions {
                    return connOpts
                }
            }
        )
        val configuration1 = Scarlet.Configuration(
            lifecycle = clientLifecycleRegistry,
            // TODO fix this retrying with trampoline
            debug = false
        )
        val scarlet = Scarlet(protocol, configuration1)

        val topicFilter = PahoMqttTopicFilter(
            "queue/test"
        )
        val configuration2 = Scarlet.Configuration(
            lifecycle = queueTestClientLifecycleRegistry,
            debug = false
        )
        val scarlet2 = Scarlet(topicFilter, configuration2, scarlet)

        client = scarlet.create<MqttService>()
        queueTestClient = scarlet2.create<MqttQueueTestService>()
    }

    @Test
    fun test2() {
        val connection = StompConnection()
        connection.open("localhost", 61613)

        connection.connect("system", "manager")
        connection.begin("tx1")
        connection.send("/queue/test", "message1", "tx1", null)
        connection.send("/queue/test", "message2", "tx1", null)
        connection.commit("tx1")

        connection.disconnect()

        createClientAndConnect()
        val queueTextObserver = queueTestClient.observeProtocolEvent().test()
        clientLifecycleRegistry.onNext(LifecycleState.Started)
        queueTestClientLifecycleRegistry.onNext(LifecycleState.Started)

        clientProtocolEventObserver.awaitCount(1)

        LOGGER.info("client event values ${clientProtocolEventObserver.values}")

        queueTextObserver.awaitCount(2)
        LOGGER.info("${queueTextObserver.values}")
    }

    companion object {
        private val LOGGER =
            Logger.getLogger(PahoMqttClientIntegrationTest::class.java.name)


        interface MqttService {
            @Receive
            fun observeProtocolEvent(): Stream<ProtocolEvent>
        }

        interface MqttQueueTestService {
            @Receive
            fun observeProtocolEvent(): Stream<ProtocolEvent>

            @Receive
            fun observeText(): Stream<String>

            @Receive
            fun observeByteArray(): Stream<ByteArray>

            @Send
            fun sendText(message: String)
        }
    }
}