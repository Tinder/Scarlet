package com.tindre.scarlet.stomp

import com.tinder.scarlet.LifecycleState
import com.tinder.scarlet.ProtocolEvent
import com.tinder.scarlet.Scarlet
import com.tinder.scarlet.Stream
import com.tinder.scarlet.lifecycle.LifecycleRegistry
import com.tinder.scarlet.stomp.GozirraStompClient
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


class StompIntegrationTest {
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

    private fun createClientAndConnect() {
        client = createClient()
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
        clientLifecycleRegistry.onNext(LifecycleState.Started)
    }

    private fun createClient(): StompService {
        val protocol = GozirraStompClient(
            GozirraStompClient.SimpleRequestFactory({
                GozirraStompClient.ClientOpenRequest("localhost", 61613, "system", "manager")
            }, {
                emptyMap()
            })
        )
        val configuration = Scarlet.Configuration(
            lifecycle = clientLifecycleRegistry,
            debug = true
        )
        val scarlet = Scarlet(protocol, configuration)
        return scarlet.create<StompService>()
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
        clientProtocolEventObserver.awaitCount(3)

        LOGGER.info("${clientProtocolEventObserver.values}")
    }

    companion object {
        private val LOGGER =
            Logger.getLogger(StompIntegrationTest::class.java.name)


        interface StompService {
            @Receive
            fun observeProtocolEvent(): Stream<ProtocolEvent>

            @Receive
            fun observeText(): Stream<String>

            @Receive
            fun observeBytes(): Stream<ByteArray>

            @Send
            fun sendText(message: String)

            @Send
            fun sendTextAndConfirm(message: String): Boolean

            @Send
            fun sendBytes(message: ByteArray)

            @Send
            fun sendBytesAndConfirm(message: ByteArray): Boolean
        }
    }
}