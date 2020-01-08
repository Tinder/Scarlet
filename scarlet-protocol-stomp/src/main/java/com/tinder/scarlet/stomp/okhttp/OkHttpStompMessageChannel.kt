package com.tinder.scarlet.stomp.okhttp

import com.tinder.scarlet.Channel
import com.tinder.scarlet.Message
import com.tinder.scarlet.MessageQueue
import com.tinder.scarlet.Protocol

class OkHttpStompMessageChannel(
    private val mainChannel: OkHttpStompMainChannel,
    private val destination: String,
    private val listener: Channel.Listener
) : Channel, MessageQueue {

    private var messageQueueListener: MessageQueue.Listener? = null

    override fun open(openRequest: Protocol.OpenRequest) {
        val destinationOpenRequest = openRequest as OkHttpStompDestination.DestinationOpenRequest
        mainChannel.subscribe(
            destination,
            destinationOpenRequest.headers
        ) { message, headers ->
            messageQueueListener?.onMessageReceived(
                this,
                this,
                Message.Text(message),
                OkHttpStompDestination.MessageMetaData(headers)
            )
        }
        listener.onOpened(this)
    }

    override fun close(closeRequest: Protocol.CloseRequest) {
        forceClose()
    }

    override fun forceClose() {
        mainChannel.unSubscribe(destination)
        listener.onClosed(this)
    }

    override fun createMessageQueue(listener: MessageQueue.Listener): MessageQueue {
        require(messageQueueListener == null)
        messageQueueListener = listener
        return this
    }

    override fun send(message: Message, messageMetaData: Protocol.MessageMetaData): Boolean {
        val headers = messageMetaData as? OkHttpStompDestination.MessageMetaData
        return when (message) {
            is Message.Text -> mainChannel.sendMessage(
                destination,
                message.value,
                headers?.headers.orEmpty()
            )
            is Message.Bytes -> throw IllegalArgumentException("Bytes are not supported")
        }
    }

}
