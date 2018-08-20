/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet

interface Connection {

    fun open(listener: Listener)
    fun close()
    fun send(topic: Topic, message: Message, option: Any?)
    fun subscribe(topic: Topic, option: Any?)
    fun unsubscribe(topic: Topic, option: Any)

    interface Listener {
        fun onConnectionOpened(clientOption: Any, serverOption: Any)
        fun onConnectionClosed(clientOption: Any, serverOption: Any)
        fun onConnectionFailed(error: Throwable)

//            // Meta info
//        fun onMessageReceived(message: Message, serverMessageInfo: ServerMessageInfo)
//        fun onMessageEnqueued(message: Message, clientMessageInfo: ClientMessageInfo)
//        fun onMessageSent(message: Message, clientMessageInfo: ClientMessageInfo)
//        fun onMessageDelivered(message: Message, clientMessageInfo: ClientMessageInfo)
    }
}
