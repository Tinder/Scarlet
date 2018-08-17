/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet

interface Connection {

    fun open(reason: ClientOpenOption, listener: Listener)
    fun close(reason: ClientCloseOption)
    fun send(topic: Topic, message: Message, clientMessageInfo: ClientMessageInfo)
    fun subscribe(topic: Topic)
    fun unsubscribe(topic: Topic)

    interface Listener {
//        fun onConnectionOpened(option: ServerOpenOption)
//        fun onConnectionClosed(option: ServerCloseOption)
//        fun onConnectionFailed(error: Throwable)

//            // Meta info
//        fun onMessageReceived(message: Message, serverMessageInfo: ServerMessageInfo)
//        fun onMessageEnqueued(message: Message, clientMessageInfo: ClientMessageInfo)
//        fun onMessageSent(message: Message, clientMessageInfo: ClientMessageInfo)
//        fun onMessageDelivered(message: Message, clientMessageInfo: ClientMessageInfo)
    }
}
