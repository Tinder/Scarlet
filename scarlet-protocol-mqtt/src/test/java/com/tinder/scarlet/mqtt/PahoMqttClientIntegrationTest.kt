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
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
import org.apache.activemq.junit.EmbeddedActiveMQBroker
import org.apache.activemq.transport.stomp.Stomp
import org.apache.activemq.transport.stomp.StompConnection
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttAsyncClient
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.nio.charset.StandardCharsets
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.Level
import java.util.logging.Logger


// TODO mqtt sandboxes https://iot.eclipse.org/getting-started/#sandboxes


class PahoMqttClientIntegrationTest {
    @get:Rule
    val broker = object : EmbeddedActiveMQBroker() {
        override fun configure() {
            brokerService.addConnector("stomp://localhost:61613?trace=true")
            brokerService.addConnector("mqtt://localhost:1883")
            brokerService.addConnector("mqtt://localhost:1884")
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
        val topic = "/queue/test"
        val content = "Message from MqttPublishSample"
        val qos = 1
        val broker = "tcp://localhost:1883"
        val clientId = "Scarlet"
        val persistence = MemoryPersistence()

        val sampleClient1 = MqttClient(broker, clientId, persistence)
        sampleClient1.setCallback(object : MqttCallback {
            override fun messageArrived(topic: String?, message: MqttMessage?) {
                topic
                LOGGER.info("messageArrived: $topic")

            }

            override fun connectionLost(cause: Throwable?) {

            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {

            }

        })
        val connOpts1 = MqttConnectOptions()
//        connOpts1.isCleanSession = true
        LOGGER.info("Connecting to broker: $broker")
        sampleClient1.connect(connOpts1)
        LOGGER.info("Connected")
        sampleClient1.subscribe(topic)
        LOGGER.info("subscribed ")

        LOGGER.info("Publishing message: $content")
        val message = MqttMessage(content.toByteArray())
        message.qos = qos
        message.isRetained = false
        sampleClient1.publish(topic, message)
        LOGGER.info("Message published")


        sampleClient1.subscribe(topic)

        sampleClient1.disconnect()
        LOGGER.info("Disconnected")

        val sampleClient = MqttAsyncClient(broker, clientId, persistence)
        val connOpts = MqttConnectOptions()
        connOpts.isCleanSession = false
//        LOGGER.info("Connecting to broker: $broker")
//        sampleClient.connect(connOpts)
//        LOGGER.info("Connected")
////        LOGGER.info("Publishing message: $content")
////        val message = MqttMessage(content.toByteArray())
////        message.qos = qos
////        sampleClient.publish(topic, message)
////        LOGGER.info("Message published")
//
//        sampleClient.disconnect()
//        LOGGER.info("Disconnected")

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
        val configuration1 = Scarlet.Configuration(
            lifecycle = clientLifecycleRegistry,
            // TODO fix this retrying with trampoline
            debug = false
        )
        val scarlet = Scarlet(protocol, configuration1)

        val topicFilter = PahoMqttTopicFilter(
            "$ACCOUNT_PREFIX#"
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
//        val connection = StompConnection()
//        connection.open("localhost", 61613)
//
//        connection.connect("system", "manager")
//        connection.begin("tx1")
//        connection.send("/queue/test", "message1", "tx1", null)
//        connection.send("/queue/test", "message2", "tx1", null)
//        connection.commit("tx1")
//
//        connection.disconnect()

        createClientAndConnect()
        val queueTextObserver = queueTestClient.observeProtocolEvent().test()
        clientLifecycleRegistry.onNext(LifecycleState.Started)
        queueTestClientLifecycleRegistry.onNext(LifecycleState.Started)

        clientProtocolEventObserver.awaitCount(1)

        LOGGER.info("client event values ${clientProtocolEventObserver.values}")

//        b()
        a()

        queueTextObserver.awaitCount(2)
        LOGGER.info("${queueTextObserver.values}")
    }

    fun a() {
        val clientId = "JavaSample"
        val listener = DefaultListener()
        val sampleClient1 = createClient(false, clientId, listener)

        LOGGER.info("Connected")

        sampleClient1.subscribe(ACCOUNT_PREFIX + "1/2/3")
        sampleClient1.subscribe(ACCOUNT_PREFIX + "a/+/#")
        sampleClient1.subscribe("$ACCOUNT_PREFIX#")
        assertTrue(sampleClient1.getPendingDeliveryTokens().size == 0)

//        val message = MqttMessage(content.toByteArray())
//        message.qos = 0
//        sampleClient1.publish(topic, message)

        var expectedResult = "should get everything"
        sampleClient1.publish(
            ACCOUNT_PREFIX + "1/2/3/4",
            expectedResult.toByteArray(StandardCharsets.UTF_8),
            0,
            false
        )

        LOGGER.info("Message published")

        // One delivery for topic  ACCOUNT_PREFIX + "#"
        var result = listener.messageQ.poll(20, TimeUnit.SECONDS).value
        assertTrue(sampleClient1.getPendingDeliveryTokens().size == 0)
    }

    fun b() {
        val listener = DefaultListener()
        // subscriber connects and creates durable sub
        val client = createClient(false, "receive", listener)


        client.subscribe(ACCOUNT_PREFIX + "1/2/3")
        client.subscribe(ACCOUNT_PREFIX + "a/+/#")
        client.subscribe("$ACCOUNT_PREFIX#")
        assertTrue(client.getPendingDeliveryTokens().size == 0)

        var expectedResult = "should get everything"
        client.publish(
            ACCOUNT_PREFIX + "1/2/3/4",
            expectedResult.toByteArray(StandardCharsets.UTF_8),
            0,
            false
        )

        // One delivery for topic  ACCOUNT_PREFIX + "#"
        var result = listener.messageQ.poll(20, TimeUnit.SECONDS).value
        assertTrue(client.getPendingDeliveryTokens().size == 0)
        assertEquals(expectedResult, result)
    }

    val ACCOUNT_PREFIX = "test/"

    @Test
    fun testSubs() {

        createClientAndConnect()
        val queueTextObserver = queueTestClient.observeProtocolEvent().test()
        clientLifecycleRegistry.onNext(LifecycleState.Started)
        queueTestClientLifecycleRegistry.onNext(LifecycleState.Started)

        clientProtocolEventObserver.awaitCount(1)


        val listener = DefaultListener()
        // subscriber connects and creates durable sub
        val client = createClient(false, "receive", listener)


        client.subscribe(ACCOUNT_PREFIX + "1/2/3")
        client.subscribe(ACCOUNT_PREFIX + "a/+/#")
        client.subscribe("$ACCOUNT_PREFIX#")
        assertTrue(client.getPendingDeliveryTokens().size == 0)

        var expectedResult = "should get everything"
        client.publish(
            ACCOUNT_PREFIX + "1/2/3/4",
            expectedResult.toByteArray(StandardCharsets.UTF_8),
            0,
            false
        )

        // One delivery for topic  ACCOUNT_PREFIX + "#"
        var result = listener.messageQ.poll(20, TimeUnit.SECONDS).value
        assertTrue(client.getPendingDeliveryTokens().size == 0)
        assertEquals(expectedResult, result)

        expectedResult = "should get everything"
        client.publish(
            ACCOUNT_PREFIX + "a/1/2",
            expectedResult.toByteArray(StandardCharsets.UTF_8),
            0,
            false
        )

        // One delivery for topic  ACCOUNT_PREFIX + "a/1/2"
        result = listener.messageQ.poll(20, TimeUnit.SECONDS).value
        assertEquals(expectedResult, result)
        // One delivery for topic  ACCOUNT_PREFIX + "#"
        result = listener.messageQ.poll(20, TimeUnit.SECONDS).value
        assertEquals(expectedResult, result)
        assertTrue(client.getPendingDeliveryTokens().size == 0)

        client.unsubscribe(ACCOUNT_PREFIX + "a/+/#")
        client.unsubscribe("$ACCOUNT_PREFIX#")
        assertTrue(client.getPendingDeliveryTokens().size == 0)

        expectedResult = "should still get 1/2/3"
        client.publish(
            ACCOUNT_PREFIX + "1/2/3",
            expectedResult.toByteArray(StandardCharsets.UTF_8),
            0,
            false
        )

        // One delivery for topic  ACCOUNT_PREFIX + "1/2/3"
        result = listener.messageQ.poll(20, TimeUnit.SECONDS).value
        assertEquals(expectedResult, result)
        assertTrue(client.getPendingDeliveryTokens().size == 0)

        client.disconnect()
        client.close()





        LOGGER.info("client event values ${clientProtocolEventObserver.values}")

        queueTextObserver.awaitCount(4)
        LOGGER.info("${queueTextObserver.values}")
    }


    @Throws(Exception::class)
    protected fun createClient(
        cleanSession: Boolean,
        clientId: String,
        listener: MqttCallback
    ): MqttClient {
        val options = MqttConnectOptions()
        options.isCleanSession = cleanSession
        return createClient(options, clientId, listener)
    }

    @Throws(Exception::class)
    protected fun createClient(
        options: MqttConnectOptions,
        clientId: String,
        listener: MqttCallback
    ): MqttClient {
        val client = MqttClient("tcp://localhost:" + 1883, clientId, MemoryPersistence())
        client.setCallback(listener)
        client.connect(options)
        Wait.waitFor(object : Wait.Condition {
            override val isSatisified: Boolean
                @Throws(Exception::class)
                get() = client.isConnected
        }, TimeUnit.SECONDS.toMillis(15), TimeUnit.MILLISECONDS.toMillis(100))
        return client
    }

//    @Throws(Exception::class)
//    protected fun disconnect(client: MqttClient) {
//        client.disconnect()
//        client.close()
//        Wait.waitFor(object : Wait.Condition() {
//            val isSatisified: Boolean
//                @Throws(Exception::class)
//                get() = !client.isConnected
//        }, TimeUnit.SECONDS.toMillis(15), TimeUnit.MILLISECONDS.toMillis(100))
//    }

//    @Throws(Exception::class)
//    protected fun waitForDelivery(client: MqttClient) {
//        Wait.waitFor(object : Wait.Condition() {
//            val isSatisified: Boolean
//                @Throws(Exception::class)
//                get() = client.pendingDeliveryTokens.size == 0
//        }, TimeUnit.SECONDS.toMillis(30), TimeUnit.MILLISECONDS.toMillis(100))
//        assertTrue(client.pendingDeliveryTokens.size == 0)
//    }

    internal class DefaultListener : MqttCallback {

        val received = AtomicInteger()
        val messageQ: BlockingQueue<java.util.AbstractMap.SimpleEntry<String, String>> =
            ArrayBlockingQueue<java.util.AbstractMap.SimpleEntry<String, String>>(10)

        override fun connectionLost(cause: Throwable) {}

        @Throws(Exception::class)
        override fun messageArrived(topic: String, message: MqttMessage) {
            LOGGER.info("Received: {}" + message)
            received.incrementAndGet()
            messageQ.put(
                java.util.AbstractMap.SimpleEntry(
                    topic,
                    String(message.payload, StandardCharsets.UTF_8)
                )
            )
        }

        override fun deliveryComplete(token: IMqttDeliveryToken) {}
    }

    object Wait {

        val MAX_WAIT_MILLIS = (30 * 1000).toLong()
        val SLEEP_MILLIS: Long = 1000

        interface Condition {
            val isSatisified: Boolean
        }

        @Throws(Exception::class)
        @JvmOverloads
        fun waitFor(
            condition: Condition,
            duration: Long = MAX_WAIT_MILLIS,
            sleepMillis: Long = SLEEP_MILLIS
        ): Boolean {

            val expiry = System.currentTimeMillis() + duration
            var conditionSatisified = condition.isSatisified
            while (!conditionSatisified && System.currentTimeMillis() < expiry) {
                TimeUnit.MILLISECONDS.sleep(sleepMillis)
                conditionSatisified = condition.isSatisified
            }
            return conditionSatisified
        }
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