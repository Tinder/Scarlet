/*
 * © 2018 Match Group, LLC.
 */
package com.tinder.scarlet.stomp.okhttp.client

import com.tinder.scarlet.Channel
import com.tinder.scarlet.Message
import com.tinder.scarlet.MessageQueue
import com.tinder.scarlet.Protocol
import com.tinder.scarlet.stomp.okhttp.core.StompSender
import com.tinder.scarlet.stomp.okhttp.core.StompSubscriber

/**
 * This channel is responsible for a specific subscription and its life cycle.
 */
class OkHttpStompMessageChannel(
    private val destination: String,
    private val stompSubscriber: StompSubscriber,
    private val stompSender: StompSender,
    private val listener: Channel.Listener
) : Channel, MessageQueue {

    private var messageQueueListener: MessageQueue.Listener? = null

    override fun open(openRequest: Protocol.OpenRequest) {
        val destinationOpenRequest = openRequest as? OkHttpStompDestination.DestinationOpenRequest
        val stompHeaders = destinationOpenRequest?.headers
        stompSubscriber.subscribe(destination, stompHeaders) { message ->
            messageQueueListener?.onMessageReceived(
                channel = this,
                messageQueue = this,
                message = Message.Text(message.payload.toString(Charsets.UTF_8)),
                metadata = OkHttpStompDestination.MessageMetaData(message.headers)
            )
        }
        listener.onOpened(this)
    }

    override fun close(closeRequest: Protocol.CloseRequest) {
        forceClose()
    }

    override fun forceClose() {
        stompSubscriber.unsubscribe(destination)
        listener.onClosed(this)
    }

    override fun createMessageQueue(listener: MessageQueue.Listener): MessageQueue {
        require(messageQueueListener == null) { "message queue was already created" }
        messageQueueListener = listener
        return this
    }

    override fun send(
        message: Message,
        messageMetaData: Protocol.MessageMetaData
    ): Boolean = when (message) {
        is Message.Text -> {
            val metaData = messageMetaData as? OkHttpStompDestination.MessageMetaData
            stompSender.convertAndSend(message.value.toByteArray(), destination, metaData?.headers)
        }
        is Message.Bytes -> {
            val metaData = messageMetaData as? OkHttpStompDestination.MessageMetaData
            stompSender.convertAndSend(message.value, destination, metaData?.headers)
        }
    }
}
