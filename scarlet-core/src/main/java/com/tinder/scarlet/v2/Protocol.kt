/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.v2

import com.tinder.scarlet.Message
import java.lang.reflect.Type

// plugin
interface Protocol {
    fun createChannelFactory(): Channel.Factory

    fun createOpenRequestFactory(channel: Channel): OpenRequest.Factory {
        return object : Protocol.OpenRequest.Factory {}
    }

    fun createCloseRequestFactory(channel: Channel): CloseRequest.Factory {
        return object : Protocol.CloseRequest.Factory {}
    }

    fun createSendingMessageMetaDataFactory(channel: Channel): MessageMetaData.Factory {
        return object : Protocol.MessageMetaData.Factory {}
    }

    fun createEventAdapterFactory(channel: Channel): EventAdapter.Factory

    interface OpenRequest {
        interface Factory {
            fun create(channel: Channel): OpenRequest = Empty
        }

        object Empty : OpenRequest
    }

    interface OpenResponse {
        object Empty : OpenResponse
    }

    interface CloseRequest {
        interface Factory {
            fun create(channel: Channel): CloseRequest = Empty
        }

        object Empty : CloseRequest
    }

    interface CloseResponse {
        object Empty : CloseResponse
    }

    interface MessageMetaData {
        interface Factory {
            fun create(channel: Channel, message: Message): MessageMetaData? = null
        }
    }

    sealed class Event {
        data class OnOpening(
            val channel: Channel, val request: OpenRequest
        ) : Event()

        data class OnOpened(
            val channel: Channel, val request: OpenRequest, val response: OpenResponse
        ) : Event()

        data class OnMessageReceived(
            val channel: Channel, val message: Message, val messageMetaData: MessageMetaData
        ) : Event()

        data class OnMessageDelivered(
            val channel: Channel, val message: Message, val messageMetaData: MessageMetaData
        ) : Event()

        data class OnClosing(
            val channel: Channel, val request: CloseRequest
        ) : Event()

        data class OnClosed(
            val channel: Channel, val request: CloseRequest, val response: CloseResponse
        ) : Event()

        data class OnFailed(val channel: Channel, val throwable: Throwable?) : Event()
    }

    interface EventAdapter<T> {
        fun fromEvent(event: Protocol.Event): T

        interface Factory {
            fun create(type: Type, annotations: Array<Annotation>): EventAdapter<*>
        }
    }
}
