/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.mqtt

import com.tinder.scarlet.ProtocolEvent
import com.tinder.scarlet.Stream
import com.tinder.scarlet.testutils.rule.PahoMqttConnection
import com.tinder.scarlet.testutils.test
import com.tinder.scarlet.ws.Receive
import com.tinder.scarlet.ws.Send
import org.apache.activemq.junit.EmbeddedActiveMQBroker
import org.junit.Rule
import org.junit.Test
import java.nio.charset.StandardCharsets
import java.util.logging.Logger

class PahoMqttClientIntegrationTest {
    @get:Rule
    val broker = object : EmbeddedActiveMQBroker() {
        override fun configure() {
            brokerService.addConnector(BROKER_CONNECTOR_URL)
        }
    }

    @get:Rule
    val connection1 = PahoMqttConnection.create<MqttQueueTestService>(
        { observeProtocolEvent() },
        PahoMqttConnection.Configuration(
            BROKER_URL,
            "$CLIENT_ID_PREFIX 1",
            TOPIC
        )
    )

    @get:Rule
    val connection2 = PahoMqttConnection.create<MqttQueueTestService>(
        { observeProtocolEvent() },
        PahoMqttConnection.Configuration(
            BROKER_URL,
            "$CLIENT_ID_PREFIX 2",
            "$TOPIC_FILTER#"
        )
    )

    @Test
    fun test3() {
        val queueTextObserver = connection2.client.observeByteArray().test()

        connection1.open()
        connection2.open()

        val s = "should get everything"
        connection1.client.sendBytes(s.toByteArray(StandardCharsets.UTF_8))
        connection1.client.sendBytes("232".toByteArray(StandardCharsets.UTF_8))
        connection1.clientClosure()

        LOGGER.info("${queueTextObserver.values}")
        queueTextObserver.awaitCount(2)
    }

    companion object {
        private val LOGGER =
            Logger.getLogger(PahoMqttClientIntegrationTest::class.java.name)

        private const val BROKER_CONNECTOR_URL = "mqtt://localhost:1883"
        private const val BROKER_URL = "tcp://localhost:1883"
        private const val CLIENT_ID_PREFIX = "client"
        private const val TOPIC_FILTER = "aTopic/"
        private const val TOPIC = "aTopic/13"

        interface MqttQueueTestService {
            @Receive
            fun observeProtocolEvent(): Stream<ProtocolEvent>

            @Receive
            fun observeByteArray(): Stream<ByteArray>

            @Send
            fun sendBytes(message: ByteArray)
        }
    }
}