/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet

interface Protocol {

    fun open(listener: Listener)
    fun close()
    fun send(topic: Topic, message: Message, option: Any?)
    fun subscribe(topic: Topic, option: Any?)
    fun unsubscribe(topic: Topic, option: Any?)

    interface Listener {
        fun onProtocolOpened(clientOption: Any?, serverOption: Any?)
        fun onProtocolClosed(clientOption: Any?, serverOption: Any?)
        fun onProtocolFailed(error: Throwable)

        fun onMessageReceived(topic: Topic, message: Message, serverMessageInfo: Any?)
        fun onMessageSent(topic: Topic, message: Message, clientMessageInfo: Any?)
        fun onMessageFailedToSend(topic: Topic, message: Message, clientMessageInfo: Any?)
        fun onTopicSubscribed(topic: Topic)
        fun onTopicUnsubscribed(topic: Topic)
    }

    interface Factory {
        fun create(): Protocol
    }
}

