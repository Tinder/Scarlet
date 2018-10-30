/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.internal.coordinator

import com.tinder.scarlet.Channel
import com.tinder.scarlet.Event
import com.tinder.scarlet.Message
import com.tinder.scarlet.MessageQueue
import com.tinder.scarlet.Protocol
import com.tinder.scarlet.ProtocolEvent

internal class Session(
    private val protocol: Protocol,
    private val parent: Session?
) {
    private val channelFactory = protocol.createChannelFactory()
    private val listener = Listener()
    private var channelDefinition: ChannelDefinition? = null
    private lateinit var eventSourceCallback: EventCallback

    fun start(eventSourceCallback: EventCallback) {
        this.eventSourceCallback = eventSourceCallback
        val channel =
            requireNotNull(channelFactory.create(listener, parent?.channelDefinition?.channel))
        channelDefinition = ChannelDefinition(
            channel,
            channel.createMessageQueue(listener),
            protocol.createOpenRequestFactory(channel),
            protocol.createCloseRequestFactory(channel),
            protocol.createOutgoingMessageMetaDataFactory(channel)
        )
    }

    fun stop() {
        channelDefinition = null
    }

    fun openSession() {
        val session = channelDefinition ?: return
        val openRequest = session.openRequestFactory.create(session.channel)
        session.channel.open(openRequest)
    }

    fun send(message: Message): Boolean {
        val session = channelDefinition ?: return false
        val messageQueue = session.messageQueue ?: return false
        val metaData = session.sendingMessageMetaDataFactory.create(session.channel, message)
        return messageQueue.send(message, metaData)
    }

    fun closeSession() {
        val session = channelDefinition ?: return
        val closeRequest = session.closeRequestFactory.create(session.channel)
        session.channel.close(closeRequest)
    }

    fun forceCloseSession() {
        val session = channelDefinition ?: return
        session.channel.forceClose()
    }

    inner class Listener : Channel.Listener,
        MessageQueue.Listener {
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

        override fun onFailed(channel: Channel, shouldRetry: Boolean, throwable: Throwable?) {
            eventSourceCallback.onEvent(
                Event.OnProtocolEvent(
                    ProtocolEvent.OnFailed(
                        shouldRetry,
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

    private class ChannelDefinition(
        val channel: Channel,
        val messageQueue: MessageQueue?,
        val openRequestFactory: Protocol.OpenRequest.Factory,
        val closeRequestFactory: Protocol.CloseRequest.Factory,
        val sendingMessageMetaDataFactory: Protocol.MessageMetaData.Factory
    )
}
