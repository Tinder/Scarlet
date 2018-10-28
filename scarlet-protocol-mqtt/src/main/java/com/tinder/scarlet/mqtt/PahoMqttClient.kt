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

class PahoMqttClient(
    private val mqttClientFactory: MqttClientFactory,
    private val mqttConnectOptionsFactory: MqttConnectOptionsFactory
) : Protocol {

    override fun createChannelFactory(): Channel.Factory {
        return object : Channel.Factory {
            override fun create(
                listener: Channel.Listener,
                parent: Channel?
            ): Channel {
                return MqttMainChannel(mqttClientFactory, listener)
            }
        }
    }

    override fun createOpenRequestFactory(channel: Channel): Protocol.OpenRequest.Factory {
        return object : Protocol.OpenRequest.Factory {
            override fun create(channel: Channel): Protocol.OpenRequest {
                return PahoMqttClient.ClientOpenRequest(mqttConnectOptionsFactory.create())
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

    data class ReceivedMessageMetaData(
        val id: Int
    ) : Protocol.MessageMetaData
}

class PahoMqttTopicFilter(
    private val topicFilter: String,
    private val qos: Int = 1
) : Protocol {
    override fun createChannelFactory(): Channel.Factory {
        return object : Channel.Factory {
            override fun create(
                listener: Channel.Listener,
                parent: Channel?
            ): Channel {
                require(parent is MqttMainChannel)
                return MqttMessageChannel(parent as MqttMainChannel, topicFilter, listener)
            }
        }
    }

    override fun createOpenRequestFactory(channel: Channel): Protocol.OpenRequest.Factory {
        return object : Protocol.OpenRequest.Factory {
            override fun create(channel: Channel): Protocol.OpenRequest {
                return TopicFilterOpenRequest(
                    qos
                )
            }
        }
    }

    override fun createEventAdapterFactory(): ProtocolEventAdapter.Factory {
        return object : ProtocolEventAdapter.Factory {}
    }

    data class TopicFilterOpenRequest(
        val qos: Int
    ) : Protocol.OpenRequest
}

class MqttMainChannel(
    private val pahoMqttClientClientFactory: PahoMqttClient.MqttClientFactory,
    private val listener: Channel.Listener
) : Channel {
    var client: MqttClient? = null

    override fun open(openRequest: Protocol.OpenRequest) {
//        if (client != null) {
//            client?.reconnect()
//            return
//        }
        val (options) = openRequest as PahoMqttClient.ClientOpenRequest
        val client = pahoMqttClientClientFactory.create()
        client.setCallback(InnerMqttCallback())
        client.connect(options)
        listener.onOpened(this)
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
    private val mqttMainChannel: MqttMainChannel,
    private val topicFilter: String,
    private val listener: Channel.Listener
) : Channel, MessageQueue {

    private var client: MqttClient? = null
    private var messageQueueListener: MessageQueue.Listener? = null

    override fun open(openRequest: Protocol.OpenRequest) {
        val topicFilterOpenRequest = openRequest as PahoMqttTopicFilter.TopicFilterOpenRequest
        client = mqttMainChannel.client
        client?.subscribe(
            topicFilter, topicFilterOpenRequest.qos
        ) { topic, message ->
            // TODO topic may be different from topic filter. should be added to meta data
            messageQueueListener?.onMessageReceived(
                this,
                this,
                Message.Bytes(message.payload),
                PahoMqttClient.ReceivedMessageMetaData(message.id)
            )
        }
        listener.onOpened(this)
    }

    override fun close(closeRequest: Protocol.CloseRequest) {
        forceClose()
    }

    override fun forceClose() {
        client?.unsubscribe(topicFilter)
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
            is Message.Bytes -> client.publish(topicFilter, MqttMessage(message.value)) // TODO qos
        }
        return true
    }
}
