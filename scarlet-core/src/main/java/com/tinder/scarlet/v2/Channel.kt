/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.v2

interface Channel : MessageQueue.Factory {
    val topic: Topic
        get() = Topic.Default

    fun open(openRequest: Protocol.OpenRequest)

    fun close(closeRequest: Protocol.CloseRequest)

    fun forceClose()

    interface Listener {
        fun onOpened(channel: Channel, response: Protocol.OpenResponse = Protocol.OpenResponse.Empty)
        fun onClosing(channel: Channel)
        fun onClosed(channel: Channel, response: Protocol.CloseResponse = Protocol.CloseResponse.Empty)
        fun onFailed(channel: Channel, throwable: Throwable?)
    }

    interface Factory {
        fun create(topic: Topic, listener: Listener): Channel?
    }
}
