/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.v2.service

import com.tinder.scarlet.Message
import com.tinder.scarlet.v2.Channel
import com.tinder.scarlet.v2.Event
import com.tinder.scarlet.v2.MessageQueue
import com.tinder.scarlet.v2.Protocol
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
        val session = channelCache ?: return
        session.channel.forceClose()
    }

    fun openSession() {
        val session = channelCache ?: return
        val openRequest = session.openRequestFactory.create(session.channel)
        session.channel.open(openRequest)
    }

    fun send(message: Message) {
        val session = channelCache ?: return
        val messageQueue = session.messageQueue ?: return
        val metaData = session.sendingMessageMetaDataFactory.create(session.channel, message)
        messageQueue.send(message, metaData)
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
                    Protocol.Event.OnOpened(
                        channel,
                        null,
                        response
                    )
                )
            )
        }

        override fun onClosing(channel: Channel) {
            eventSourceCallback.onEvent(
                Event.OnProtocolEvent(
                    Protocol.Event.OnClosing(
                        channel
                    )
                )
            )
        }

        override fun onClosed(channel: Channel, response: Protocol.CloseResponse) {
            eventSourceCallback.onEvent(
                Event.OnProtocolEvent(
                    Protocol.Event.OnClosed(
                        channel,
                        response
                    )
                )
            )
        }

        override fun onFailed(channel: Channel, throwable: Throwable?) {
            eventSourceCallback.onEvent(
                Event.OnProtocolEvent(
                    Protocol.Event.OnFailed(
                        channel,
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
                    Protocol.Event.OnMessageReceived(
                        channel,
                        messageQueue,
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
                    Protocol.Event.OnMessageDelivered(
                        channel,
                        messageQueue,
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
