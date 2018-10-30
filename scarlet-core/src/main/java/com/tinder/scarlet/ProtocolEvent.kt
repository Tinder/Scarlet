/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet

sealed class ProtocolEvent {

    data class OnOpened(
        val response: Protocol.OpenResponse
    ) : ProtocolEvent()

    data class OnMessageReceived(
        val message: Message,
        val messageMetaData: Protocol.MessageMetaData
    ) : ProtocolEvent()

    data class OnMessageDelivered(
        val message: Message,
        val messageMetaData: Protocol.MessageMetaData
    ) : ProtocolEvent()

    data class OnClosing(
        val response: Protocol.CloseResponse
    ) : ProtocolEvent()

    data class OnClosed(
        val response: Protocol.CloseResponse
    ) : ProtocolEvent()

    data class OnFailed(
        val shouldRetry: Boolean,
        val throwable: Throwable?
    ) : ProtocolEvent()
}
