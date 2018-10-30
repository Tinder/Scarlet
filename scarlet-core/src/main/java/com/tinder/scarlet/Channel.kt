/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet

interface Channel : MessageQueue.Factory {

    fun open(openRequest: Protocol.OpenRequest)

    fun close(closeRequest: Protocol.CloseRequest)

    fun forceClose()

    interface Listener {
        fun onOpened(channel: Channel, response: Protocol.OpenResponse = Protocol.OpenResponse.Empty)
        fun onClosing(channel: Channel, response: Protocol.CloseResponse = Protocol.CloseResponse.Empty)
        fun onClosed(channel: Channel, response: Protocol.CloseResponse = Protocol.CloseResponse.Empty)
        fun onFailed(channel: Channel, shouldRetry: Boolean, throwable: Throwable?)
    }

    interface Factory {
        fun create(
            listener: Listener,
            parent: Channel?
        ): Channel?
    }
}
