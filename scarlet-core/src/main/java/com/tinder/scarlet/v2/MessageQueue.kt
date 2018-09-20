/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.v2

import com.tinder.scarlet.Message

interface MessageQueue {

    fun send(message: Message, messageMetaData: Protocol.MessageMetaData)

    interface Listener {
        fun onMessageReceived(channel: Channel, message: Message, metadata: Protocol.MessageMetaData? = null)
        fun onMessageDelivered(channel: Channel, message: Message, metadata: Protocol.MessageMetaData? = null)
    }

    interface Factory {
        fun createMessageQueue(listener: Listener): MessageQueue? = null
    }
}
