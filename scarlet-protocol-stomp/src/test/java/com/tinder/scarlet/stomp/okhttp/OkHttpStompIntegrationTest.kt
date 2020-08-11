package com.tinder.scarlet.stomp.okhttp

import com.tinder.scarlet.ProtocolEvent
import com.tinder.scarlet.Stream
import com.tinder.scarlet.testutils.rule.OkHttpStompWebSocketConnection
import com.tinder.scarlet.testutils.test
import com.tinder.scarlet.ws.Receive
import com.tinder.scarlet.ws.Send
import io.reactivex.observers.BaseTestConsumer.TestWaitStrategy.SLEEP_100MS
import org.apache.activemq.command.ActiveMQDestination
import org.apache.activemq.junit.EmbeddedActiveMQBroker
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.logging.Logger

class OkHttpStompIntegrationTest {

    private var broker: TestEmbeddedActiveMQBroker = TestEmbeddedActiveMQBroker()

    @get:Rule
    val firstConnection = OkHttpStompWebSocketConnection.create<StompOkHttpQueueTestService>(
        { observeProtocolEvent() },
        OkHttpStompWebSocketConnection.Configuration(
            login = LOGIN,
            port = PORT,
            password = PASSWORD,
            host = HOST,
            destination = CLIENT_DESTINATION
        )
    )

    @get:Rule
    val secondConnection = OkHttpStompWebSocketConnection.create<StompOkHttpQueueTestService>(
        { observeProtocolEvent() },
        OkHttpStompWebSocketConnection.Configuration(
            login = LOGIN,
            port = PORT,
            password = PASSWORD,
            host = HOST,
            destination = CLIENT_DESTINATION
        )
    )

    @Before
    fun setUp() {
        startServer()
    }

    @After
    fun tearDown() {
        stopServer()
    }

    @Test
    fun `correct receive and send messages`() {
        val queueTextObserver = secondConnection.client.observeText().test()

        firstConnection.open()
        secondConnection.open()

        for (index in 0 until 9) {
            firstConnection.client.sendText("message $index")
        }
        firstConnection.clientClosure()

        LOGGER.info("${queueTextObserver.values}")
        queueTextObserver.awaitCountAndCheck(9, SLEEP_100MS)
        secondConnection.clientClosure()
    }

    @Test
    fun `reconnect test`() {
        val queueTextObserver = secondConnection.client.observeText().test()

        firstConnection.open()
        secondConnection.open()

        restartServer()

        firstConnection.clientProtocolEventObserver.awaitCountAndCheck(3) // Open -> Close -> Open
        secondConnection.clientProtocolEventObserver.awaitCountAndCheck(3) // Open -> Close -> Open

        for (index in 0 until 9) {
            firstConnection.client.sendText("message $index")
        }

        firstConnection.clientClosure()

        LOGGER.info("${queueTextObserver.values}")
        queueTextObserver.awaitCountAndCheck(9, SLEEP_100MS)
        secondConnection.clientClosure()
    }

    private fun startServer() {
        broker = TestEmbeddedActiveMQBroker()
        broker.start()
    }

    private fun stopServer() {
        broker.stop()
    }

    private fun restartServer() {
        broker.stop()
        broker = TestEmbeddedActiveMQBroker()
        broker.start()
    }

    private class TestEmbeddedActiveMQBroker : EmbeddedActiveMQBroker() {

        override fun configure() {
            val destination = ActiveMQDestination.createDestination(SERVER_DESTINATION, 0)
            brokerService.destinations = arrayOf(destination)
            brokerService.addConnector(BROKER_URL)
            brokerService.isPersistent = false
        }
    }

    companion object {
        private val LOGGER = Logger.getLogger(OkHttpStompIntegrationTest::class.java.name)

        private const val HOST = "localhost"
        private const val PORT = 61613
        private const val LOGIN = "system"
        private const val PASSWORD = "manager"
        private const val BROKER_URL = "ws://$HOST:$PORT"
        private const val SERVER_DESTINATION = "topic://queue_test"
        private const val CLIENT_DESTINATION = "/topic/test"

        interface StompOkHttpQueueTestService {
            @Receive
            fun observeProtocolEvent(): Stream<ProtocolEvent>

            @Receive
            fun observeText(): Stream<String>

            @Send
            fun sendText(message: String)
        }
    }
}
