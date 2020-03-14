package com.tinder.scarlet.stomp

import com.tinder.scarlet.ProtocolEvent
import com.tinder.scarlet.Stream
import com.tinder.scarlet.testutils.rule.OkHttpStompWebSocketConnection
import com.tinder.scarlet.testutils.test
import com.tinder.scarlet.ws.Receive
import com.tinder.scarlet.ws.Send
import org.apache.activemq.broker.jmx.ManagementContext
import org.apache.activemq.command.ActiveMQTopic
import org.apache.activemq.junit.EmbeddedActiveMQBroker
import org.junit.Rule
import org.junit.Test
import java.util.logging.Logger


class OkHttpStompIntegrationTest {

    @get:Rule
    val broker = object : EmbeddedActiveMQBroker() {
        override fun configure() {
            brokerService.addConnector(BROKER_URL)

            val topic = ActiveMQTopic(DESTINATION)
            brokerService.destinations = arrayOf(topic)

            val managementContext = ManagementContext()
            managementContext.isCreateConnector = true
            brokerService.managementContext = managementContext
        }
    }

    @get:Rule
    val firstConnection = OkHttpStompWebSocketConnection.create<StompQueueTestService>(
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
    val secondConnection = OkHttpStompWebSocketConnection.create<StompQueueTestService>(
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
        firstConnection.client.sendText("message1")
        firstConnection.client.sendText("message2")
        firstConnection.clientClosure()

        secondConnection.open()

        LOGGER.info("${queueTextObserver.values}")
        queueTextObserver.awaitCount(2)
    }

    companion object {
        private val LOGGER = Logger.getLogger(OkHttpStompIntegrationTest::class.java.name)

        private const val HOST = "localhost"
        private const val PORT = 34343
        private const val LOGIN = "system"
        private const val PASSWORD = "manager"
        private const val BROKER_URL = "ws://$HOST:$PORT"
        private const val DESTINATION = "/queue/test"

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
