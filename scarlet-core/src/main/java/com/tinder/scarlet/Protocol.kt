/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet

interface Protocol {
    fun createChannelFactory(): Channel.Factory

    fun createOpenRequestFactory(channel: Channel): OpenRequest.Factory {
        return object : OpenRequest.Factory {}
    }

    fun createCloseRequestFactory(channel: Channel): CloseRequest.Factory {
        return object : CloseRequest.Factory {}
    }

    fun createOutgoingMessageMetaDataFactory(channel: Channel): MessageMetaData.Factory {
        return object : MessageMetaData.Factory {}
    }

    fun createEventAdapterFactory(): ProtocolEventAdapter.Factory

    interface OpenRequest {
        interface Factory {
            fun create(channel: Channel): OpenRequest =
                Empty
        }

        object Empty : OpenRequest
    }

    interface OpenResponse {
        object Empty : OpenResponse
    }

    interface CloseRequest {
        interface Factory {
            fun create(channel: Channel): CloseRequest =
                Empty
        }

        object Empty : CloseRequest
    }

    interface CloseResponse {
        object Empty : CloseResponse
    }

    interface MessageMetaData {
        interface Factory {
            fun create(channel: Channel, message: Message): MessageMetaData =
                Empty
        }

        object Empty : MessageMetaData
    }
}
