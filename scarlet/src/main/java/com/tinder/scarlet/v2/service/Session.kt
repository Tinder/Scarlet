/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.v2.service

import com.tinder.scarlet.Message
import com.tinder.scarlet.v2.Channel
import com.tinder.scarlet.v2.Event
import com.tinder.scarlet.v2.MessageQueue
import com.tinder.scarlet.v2.Protocol
import com.tinder.scarlet.v2.ProtocolEvent
import com.tinder.scarlet.v2.Topic

internal class Session(
    private val protocol: Protocol,
    private val topic: Topic
) {
    private val channelFactory = protocol.createChannelFactory()
    private val listener = Listener()
    private var channelCache: ChannelCache? = null
    private lateinit var eventSourceCallback: EventCallback

    fun start(eventSourceCallback: EventCallback) {
        this.eventSourceCallback = eventSourceCallback
        val channel = requireNotNull(channelFactory.create(topic, listener))
        channelCache = ChannelCache(
            channel,
            channel.createMessageQueue(listener),
            protocol.createOpenRequestFactory(channel),
            protocol.createCloseRequestFactory(channel),
            protocol.createSendingMessageMetaDataFactory(channel)
        )
    }

    fun stop() {
        channelCache = null
    }

    fun openSession() {
        val session = channelCache ?: return
        val openRequest = session.openRequestFactory.create(session.channel)
        session.channel.open(openRequest)
    }

    fun send(message: Message): Boolean {
        val session = channelCache ?: return false
        val messageQueue = session.messageQueue ?: return false
        val metaData = session.sendingMessageMetaDataFactory.create(session.channel, message)
        return messageQueue.send(message, metaData)
    }

    fun closeSession() {
        val session = channelCache ?: return
        val closeRequest = session.closeRequestFactory.create(session.channel)
        session.channel.close(closeRequest)
    }

    fun forceCloseSession() {
        val session = channelCache ?: return
        session.channel.forceClose()
    }

    inner class Listener : Channel.Listener, MessageQueue.Listener {
        override fun onOpened(channel: Channel, response: Protocol.OpenResponse) {
            eventSourceCallback.onEvent(
                Event.OnProtocolEvent(
                    ProtocolEvent.OnOpened(
                        response
                    )
                )
            )
        }

        override fun onClosing(channel: Channel, response: Protocol.CloseResponse) {
            eventSourceCallback.onEvent(
                Event.OnProtocolEvent(
                    ProtocolEvent.OnClosing(
                        response
                    )
                )
            )
        }

        override fun onClosed(channel: Channel, response: Protocol.CloseResponse) {
            eventSourceCallback.onEvent(
                Event.OnProtocolEvent(
                    ProtocolEvent.OnClosed(
                        response
                    )
                )
            )
        }

        override fun onFailed(channel: Channel, throwable: Throwable?) {
            eventSourceCallback.onEvent(
                Event.OnProtocolEvent(
                    ProtocolEvent.OnFailed(
                        throwable
                    )
                )
            )
        }

        override fun onMessageReceived(
            channel: Channel,
            messageQueue: MessageQueue,
            message: Message,
            metadata: Protocol.MessageMetaData
        ) {
            eventSourceCallback.onEvent(
                Event.OnProtocolEvent(
                    ProtocolEvent.OnMessageReceived(
                        message,
                        metadata
                    )
                )
            )
        }

        override fun onMessageDelivered(
            channel: Channel,
            messageQueue: MessageQueue,
            message: Message,
            metadata: Protocol.MessageMetaData
        ) {
            eventSourceCallback.onEvent(
                Event.OnProtocolEvent(
                    ProtocolEvent.OnMessageDelivered(
                        message,
                        metadata
                    )
                )
            )
        }
    }

    private class ChannelCache(
        val channel: Channel,
        val messageQueue: MessageQueue?,
        val openRequestFactory: Protocol.OpenRequest.Factory,
        val closeRequestFactory: Protocol.CloseRequest.Factory,
        val sendingMessageMetaDataFactory: Protocol.MessageMetaData.Factory
    )
}
