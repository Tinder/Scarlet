/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.v2

import com.tinder.scarlet.Message
import java.lang.reflect.Type

sealed class ProtocolEvent {

    data class OnOpened(
        val messageQueue: MessageQueue?,
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

    data class OnFailed(val throwable: Throwable?) : ProtocolEvent()

    interface Adapter {
        fun fromEvent(event: ProtocolEvent): ProtocolSpecificEvent = ProtocolSpecificEvent.Empty

        interface Factory {
            fun create(type: Type, annotations: Array<Annotation>): Adapter = object : Adapter {}
        }
    }
}
