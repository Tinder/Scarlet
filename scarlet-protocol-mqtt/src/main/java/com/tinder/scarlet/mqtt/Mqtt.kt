/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.mqtt

import com.tinder.scarlet.Channel
import com.tinder.scarlet.Message
import com.tinder.scarlet.MessageQueue
import com.tinder.scarlet.Protocol
import com.tinder.scarlet.ProtocolEventAdapter
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage

class Mqtt(
    private val mqttClientFactory: MqttClientFactory,
    private val mqttConnectOptionsFactory: MqttConnectOptionsFactory,
    private val qos: Int
) : Protocol {

    private var mainChannel: MqttMainChannel? = null

    override fun createChannelFactory(): Channel.Factory {
        return object : Channel.Factory {
            override fun create(
                listener: Channel.Listener,
                parent: Channel?
            ): Channel {
                return MqttMainChannel(mqttClientFactory, listener)
//                if (topic == Topic.Main) {
//                    mainChannel = MqttMainChannel(mqttClientFactory, listener)
//                    return mainChannel!!
//                }
//                return MqttMessageChannel(topic, listener)
            }
        }
    }

    override fun createOpenRequestFactory(channel: Channel): Protocol.OpenRequest.Factory {
        return object : Protocol.OpenRequest.Factory {
            override fun create(channel: Channel): Protocol.OpenRequest {
                return Mqtt.ClientOpenRequest(mqttConnectOptionsFactory.create())
//                if (channel.topic == Topic.Main) {
//                    return Mqtt.ClientOpenRequest(mqttConnectOptionsFactory.create())
//                }
//                return Mqtt.TopicOpenRequest(
//                    requireNotNull(mainChannel?.client),
//                    qos
//                )
            }
        }
    }

    override fun createEventAdapterFactory(): ProtocolEventAdapter.Factory {
        return object : ProtocolEventAdapter.Factory {}
    }

    interface MqttClientFactory {
        fun create(): MqttClient
    }

    interface MqttConnectOptionsFactory {
        fun create(): MqttConnectOptions
    }

    data class ClientOpenRequest(
        val options: MqttConnectOptions
    ) : Protocol.OpenRequest

    data class TopicOpenRequest(
        val client: MqttClient,
        val qos: Int
    ) : Protocol.OpenRequest

    data class ReceivedMessageMetaData(
        val id: Int
    ) : Protocol.MessageMetaData
}

class MqttMainChannel(
    private val mqttClientFactory: Mqtt.MqttClientFactory,
    private val listener: Channel.Listener
) : Channel {
    var client: MqttClient? = null

    override fun open(openRequest: Protocol.OpenRequest) {
        if (client != null) {
            client?.reconnect()
            return
        }
        val (options) = openRequest as Mqtt.ClientOpenRequest
        val client = mqttClientFactory.create()
        client.setCallback(InnerMqttCallback())
        client.connect(options)
        this.client = client
    }

    override fun close(closeRequest: Protocol.CloseRequest) {
        forceClose()
    }

    override fun forceClose() {
        try {
            client?.disconnect()
            listener.onClosed(this)
        } catch (e: Throwable) {
            listener.onFailed(this, e)
        }
    }

    override fun createMessageQueue(listener: MessageQueue.Listener): MessageQueue? {
        return null
    }

    inner class InnerMqttCallback : MqttCallback {
        override fun messageArrived(topic: String, message: MqttMessage) {
        }

        override fun connectionLost(cause: Throwable) {
            listener.onFailed(this@MqttMainChannel, cause)
        }

        override fun deliveryComplete(token: IMqttDeliveryToken) {
        }
    }
}

class MqttMessageChannel(
    private val topic: String,
    private val listener: Channel.Listener
) : Channel, MessageQueue {

    private var client: MqttClient? = null
    private var messageQueueListener: MessageQueue.Listener? = null

    override fun open(openRequest: Protocol.OpenRequest) {
        val openRequest = openRequest as Mqtt.TopicOpenRequest
        client = openRequest.client
        client?.subscribe(
            topic, openRequest.qos
        ) { _, message ->
            messageQueueListener?.onMessageReceived(
                this,
                this,
                Message.Bytes(message.payload),
                Mqtt.ReceivedMessageMetaData(message.id)
            )
        }
        listener.onOpened(this)
    }

    override fun close(closeRequest: Protocol.CloseRequest) {
        forceClose()
    }

    override fun forceClose() {
        client?.unsubscribe(topic)
        client = null
        listener.onClosed(this)
    }

    override fun createMessageQueue(listener: MessageQueue.Listener): MessageQueue {
        require(messageQueueListener == null)
        messageQueueListener = listener
        return this
    }

    override fun send(message: Message, messageMetaData: Protocol.MessageMetaData): Boolean {
        val client = client ?: return false
        when (message) {
            is Message.Text -> throw IllegalArgumentException("String are not supported")
            is Message.Bytes -> client.publish(topic, MqttMessage(message.value)) // TODO qos
        }
        return true
    }
}
