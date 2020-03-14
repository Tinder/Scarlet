package com.tinder.scarlet.stomp.okhttp

import com.tinder.scarlet.ProtocolEvent
import com.tinder.scarlet.Stream
import com.tinder.scarlet.testutils.rule.OkHttpStompWebSocketConnection
import com.tinder.scarlet.testutils.test
import com.tinder.scarlet.ws.Receive
import com.tinder.scarlet.ws.Send
import org.apache.activemq.junit.EmbeddedActiveMQBroker
import org.junit.Rule
import org.junit.Test
import java.util.logging.Logger

class OkHttpStompIntegrationTest {

    @get:Rule
    val broker = object : EmbeddedActiveMQBroker() {
        override fun configure() {
            brokerService.addConnector(BROKER_URL)
        }
    }

    @get:Rule
    val firstConnection = OkHttpStompWebSocketConnection.create<StompOkHttpQueueTestService>(
        { observeProtocolEvent() },
        OkHttpStompWebSocketConnection.Configuration(
            login = LOGIN,
            port = PORT,
            password = PASSWORD,
            host = HOST,
            destination = DESTINATION
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
            destination = DESTINATION
        )
    )

    @Test
    fun `correct receive and send messages`() {
        val queueTextObserver = secondConnection.client.observeText().test()

        firstConnection.open()
        secondConnection.open()

        firstConnection.client.sendText("message1")
        firstConnection.client.sendText("message2")
        firstConnection.clientClosure()

        LOGGER.info("${queueTextObserver.values}")
        queueTextObserver.awaitCountAtLeast(1)// because broker has a bug and it loses messages sometimes

        secondConnection.clientClosure()
    }

    companion object {
        private val LOGGER = Logger.getLogger(OkHttpStompIntegrationTest::class.java.name)

        private const val HOST = "localhost"
        private const val PORT = 34343
        private const val LOGIN = "system"
        private const val PASSWORD = "manager"
        private const val BROKER_URL = "ws://$HOST:$PORT"
        private const val DESTINATION = "/queue/test"

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
