/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.v2

import com.tinder.scarlet.Message

// plugin
interface Protocol {
    // TODO to val?
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

    fun createEventAdapterFactory(): ProtocolEventAdapter.Factory

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
            fun create(channel: Channel, message: Message): MessageMetaData = MessageMetaData.Empty
        }

        object Empty : MessageMetaData
    }


}

