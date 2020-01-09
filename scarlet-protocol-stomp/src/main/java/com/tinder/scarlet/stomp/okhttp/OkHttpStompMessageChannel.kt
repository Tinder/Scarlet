package com.tinder.scarlet.stomp.okhttp

import com.tinder.scarlet.Channel
import com.tinder.scarlet.Message
import com.tinder.scarlet.MessageQueue
import com.tinder.scarlet.Protocol
import com.tinder.scarlet.stomp.core.StompSubscriber

class OkHttpStompMessageChannel(
    private val destination: String,
    private val stompSubscriber: StompSubscriber,
    private val stompSender: MessageQueue,
    private val listener: Channel.Listener
) : Channel, MessageQueue {

    private var messageQueueListener: MessageQueue.Listener? = null

    override fun open(openRequest: Protocol.OpenRequest) {
        val destinationOpenRequest = openRequest as OkHttpStompDestination.DestinationOpenRequest
        stompSubscriber.subscribe(destination, destinationOpenRequest.headers) { message, headers ->
            messageQueueListener?.onMessageReceived(
                channel = this,
                messageQueue = this,
                message = message
//                metadata = OkHttpStompClient.MessageMetaData(headers)
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
        require(messageQueueListener == null)
        messageQueueListener = listener
        return this
    }

    override fun send(
        message: Message,
        messageMetaData: Protocol.MessageMetaData
    ): Boolean = when (message) {
        is Message.Text -> stompSender.send(message, messageMetaData)
        is Message.Bytes -> throw IllegalArgumentException("Bytes are not supported")
    }

}
