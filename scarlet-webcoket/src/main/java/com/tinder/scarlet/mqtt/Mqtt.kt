/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.mqtt

import com.tinder.scarlet.Message
import com.tinder.scarlet.v2.Channel
import com.tinder.scarlet.v2.MessageQueue
import com.tinder.scarlet.v2.Protocol
import com.tinder.scarlet.v2.Topic
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage

class Mqtt(
    private val mqttClientFactory: MqttClientFactory,
    private val mqttConnectOptionsFactory: MqttConnectOptionsFactory,
    private val qos: Int
) : Protocol, Channel.Factory, Protocol.OpenRequest.Factory {

    private var mainChannel: MqttMainChannel? = null

    override fun create(topic: Topic, listener: Channel.Listener): Channel {
        if (topic == Topic.Default) {
            mainChannel = MqttMainChannel(mqttClientFactory, listener)
            return mainChannel!!
        }
        return MqttMessageChannel(topic, listener)
    }

    override fun create(channel: Channel): Protocol.OpenRequest {
        if (channel.topic == Topic.Default) {
            return Mqtt.ClientOpenRequest(mqttConnectOptionsFactory.create())
        }
        return Mqtt.TopicOpenRequest(
            requireNotNull(mainChannel?.client),
            qos
        )
    }

    override fun createChannelFactory(): Channel.Factory {
        return this
    }

    override fun createOpenRequestFactory(channel: Channel): Protocol.OpenRequest.Factory {
        return this
    }

    override fun createEventAdapterFactory(channel: Channel): Protocol.EventAdapter.Factory {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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
    override val topic: Topic,
    private val listener: Channel.Listener
) : Channel, MessageQueue {

    private var client: MqttClient? = null
    private var messageQueueListener: MessageQueue.Listener? = null

    override fun open(openRequest: Protocol.OpenRequest) {
        val openRequest = openRequest as Mqtt.TopicOpenRequest
        client = openRequest.client
        client?.subscribe(
            topic.id, openRequest.qos
        ) { _, message ->
            messageQueueListener?.onMessageReceived(
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
        client?.unsubscribe(topic.id)
        client = null
        listener.onClosed(this)
    }

    override fun createMessageQueue(listener: MessageQueue.Listener): MessageQueue {
        require(messageQueueListener == null)
        messageQueueListener = listener
        return this
    }

    override fun send(message: Message, messageMetaData: Protocol.MessageMetaData) {
        when (message) {
            is Message.Text -> throw IllegalArgumentException("String are not supported")
            is Message.Bytes -> client?.publish(topic.id, MqttMessage(message.value)) // TODO qos
        }
    }
}

class MqttChannelFactory(
    private val mqttClientFactory: Mqtt.MqttClientFactory,
    private val mqttConnectOptionsFactory: Mqtt.MqttConnectOptionsFactory,
    private val qos: Int
) : Channel.Factory, Protocol.OpenRequest.Factory {

    private var mainChannel: MqttMainChannel? = null

    override fun create(topic: Topic, listener: Channel.Listener): Channel {
        if (topic == Topic.Default) {
            mainChannel = MqttMainChannel(mqttClientFactory, listener)
            return mainChannel!!
        }
        return MqttMessageChannel(topic, listener)
    }

    override fun create(channel: Channel): Protocol.OpenRequest {
        if (channel.topic == Topic.Default) {
            return Mqtt.ClientOpenRequest(mqttConnectOptionsFactory.create())
        }
        return Mqtt.TopicOpenRequest(
            requireNotNull(mainChannel?.client),
            qos
        )
    }
}
