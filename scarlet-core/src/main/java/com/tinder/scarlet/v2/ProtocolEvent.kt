/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.v2

import com.tinder.scarlet.Message
import java.lang.reflect.Type

sealed class ProtocolEvent {
    abstract val channel: Channel

    data class OnOpened(
        override val channel: Channel,
        val messageQueue: MessageQueue?,
        val response: Protocol.OpenResponse
    ) : ProtocolEvent()

    data class OnMessageReceived(
        override val channel: Channel,
        val messageQueue: MessageQueue,
        val message: Message,
        val messageMetaData: Protocol.MessageMetaData
    ) : ProtocolEvent()

    data class OnMessageDelivered(
        override val channel: Channel,
        val messageQueue: MessageQueue,
        val message: Message,
        val messageMetaData: Protocol.MessageMetaData
    ) : ProtocolEvent()

    data class OnClosing(
        override val channel: Channel, val response: Protocol.CloseResponse
    ) : ProtocolEvent()

    data class OnClosed(
        override val channel: Channel, val response: Protocol.CloseResponse
    ) : ProtocolEvent()

    data class OnFailed(override val channel: Channel, val throwable: Throwable?) : ProtocolEvent()

    interface Adapter<T> {
        fun fromEvent(event: ProtocolEvent): T

        interface Factory {
            fun create(type: Type, annotations: Array<Annotation>): Adapter<*>
        }
    }
}
