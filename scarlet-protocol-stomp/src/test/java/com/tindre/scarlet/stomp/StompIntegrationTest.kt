/*
 * © 2018 Match Group, LLC.
 */

package com.tindre.scarlet.stomp

import com.tinder.scarlet.ProtocolEvent
import com.tinder.scarlet.Stream
import com.tinder.scarlet.testutils.rule.GozirraStompConnection
import com.tinder.scarlet.testutils.test
import com.tinder.scarlet.ws.Receive
import com.tinder.scarlet.ws.Send
import org.apache.activemq.junit.EmbeddedActiveMQBroker
import org.apache.activemq.transport.stomp.StompConnection
import org.junit.Rule
import org.junit.Test
import java.util.logging.Logger

class StompIntegrationTest {
    @get:Rule
    val broker = object : EmbeddedActiveMQBroker() {
        override fun configure() {
            brokerService.addConnector(BROKER_URL)
        }
    }
    @get:Rule
    val connection1 = GozirraStompConnection.create<StompQueueTestService>(
        { observeProtocolEvent() },
        GozirraStompConnection.Configuration(
            HOST,
            PORT,
            LOGIN,
            PASSWORD
        )
    )
    @get:Rule
    val connection2 = GozirraStompConnection.create<StompQueueTestService>(
        { observeProtocolEvent() },
        GozirraStompConnection.Configuration(
            HOST,
            PORT,
            LOGIN,
            PASSWORD
        )
    )

    @Test
    fun test() {
        val queueTextObserver = connection2.client.observeText().test()

        connection1.open()
        connection1.client.sendText("message1")
        connection1.client.sendText("message2")
        connection1.clientClosure()

        connection2.open()

        LOGGER.info("${queueTextObserver.values}")
        queueTextObserver.awaitCount(2)
    }

    @Test
    fun test2() {
        val connection1 = StompConnection()
        connection1.open("localhost", 61613)

        connection1.connect("system", "manager")
        connection1.begin("tx1")
        connection1.send("/queue/test", "message1", "tx1", null)
        connection1.send("/queue/test", "message2", "tx1", null)
        connection1.commit("tx1")

        connection1.disconnect()

        connection2.open()

        val queueTextObserver = connection2.client.observeText().test()

        queueTextObserver.awaitCount(2)
        LOGGER.info("${queueTextObserver.values}")
    }

    companion object {
        private val LOGGER =
            Logger.getLogger(StompIntegrationTest::class.java.name)

        private const val HOST = "localhost"
        private const val PORT = 61613
        private const val LOGIN = "system"
        private const val PASSWORD = "manager"
        private const val BROKER_URL = "stomp://$HOST:$PORT"

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