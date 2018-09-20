/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.v2

import com.tinder.scarlet.Message

interface MessageQueue {

    fun send(message: Message, messageMetaData: Protocol.MessageMetaData)

    interface Listener {
        fun onMessageReceived(
            channel: Channel,
            messageQueue: MessageQueue,
            message: Message,
            metadata: Protocol.MessageMetaData = Protocol.MessageMetaData.Empty
        )

        fun onMessageDelivered(
            channel: Channel,
            messageQueue: MessageQueue,
            message: Message,
            metadata: Protocol.MessageMetaData = Protocol.MessageMetaData.Empty
        )
    }

    interface Factory {
        fun createMessageQueue(listener: Listener): MessageQueue? = null
    }
}
