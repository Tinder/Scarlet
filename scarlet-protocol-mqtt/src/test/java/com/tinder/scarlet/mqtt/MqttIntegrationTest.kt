package com.tinder.scarlet.mqtt

import com.tinder.scarlet.LifecycleState
import com.tinder.scarlet.ProtocolEvent
import com.tinder.scarlet.Scarlet
import com.tinder.scarlet.Stream
import com.tinder.scarlet.lifecycle.LifecycleRegistry
import com.tinder.scarlet.stomp.GozirraStompClient
import com.tinder.scarlet.stomp.GozirraStompDestination
import com.tinder.scarlet.testutils.TestStreamObserver
import com.tinder.scarlet.testutils.test
import com.tinder.scarlet.ws.Receive
import com.tinder.scarlet.ws.Send
import org.apache.activemq.junit.EmbeddedActiveMQBroker
import org.apache.activemq.transport.stomp.Stomp
import org.apache.activemq.transport.stomp.StompConnection
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.logging.Level
import java.util.logging.Logger


class MqttIntegrationTest {
    @get:Rule
    val broker = object : EmbeddedActiveMQBroker() {
        override fun configure() {
            brokerService.addConnector("stomp://localhost:61613?trace=true")
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
//        val connect = connection.receive()
//        if (connect.action != Stomp.Responses.CONNECTED) {
//            throw Exception("Not connected")
//        }

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

    private lateinit var client: StompService
    private lateinit var clientProtocolEventObserver: TestStreamObserver<ProtocolEvent>
    private val clientLifecycleRegistry = LifecycleRegistry()

    private lateinit var queueTestClient: StompQueueTestService
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
        val protocol = GozirraStompClient(
            GozirraStompClient.SimpleRequestFactory {
                GozirraStompClient.ClientOpenRequest("localhost", 61613, "system", "manager")
            }
        )
        val configuration1 = Scarlet.Configuration(
            lifecycle = clientLifecycleRegistry,
            debug = true
        )
        val scarlet = Scarlet(protocol, configuration1)

        val destination = GozirraStompDestination(
            "/queue/test",
            GozirraStompDestination.SimpleRequestFactory {
                emptyMap()
            }
        )
        val configuration2 = Scarlet.Configuration(
            lifecycle = queueTestClientLifecycleRegistry,
            debug = true
        )
        val scarlet2 = Scarlet(destination, configuration2, scarlet)

        client = scarlet.create<StompService>()
        queueTestClient = scarlet2.create<StompQueueTestService>()
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
        val queueTextObserver = queueTestClient.observeText().test()
        clientLifecycleRegistry.onNext(LifecycleState.Started)
        queueTestClientLifecycleRegistry.onNext(LifecycleState.Started)

        clientProtocolEventObserver.awaitCount(1)

        LOGGER.info("${clientProtocolEventObserver.values}")

        queueTextObserver.awaitCount(2)
        LOGGER.info("${queueTextObserver.values}")
    }

    companion object {
        private val LOGGER =
            Logger.getLogger(MqttIntegrationTest::class.java.name)


        interface StompService {
            @Receive
            fun observeProtocolEvent(): Stream<ProtocolEvent>
        }

        interface StompQueueTestService {
            @Receive
            fun observeProtocolEvent(): Stream<ProtocolEvent>

            @Receive
            fun observeText(): Stream<String>

            @Send
            fun sendText(message: String)
        }
    }
}