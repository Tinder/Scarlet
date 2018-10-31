/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet

import com.tinder.scarlet.utils.SimpleProtocolCloseRequestFactory
import com.tinder.scarlet.utils.SimpleProtocolOpenRequestFactory

interface Protocol {
    fun createChannelFactory(): Channel.Factory

    fun createOpenRequestFactory(channel: Channel): OpenRequest.Factory {
        return SimpleProtocolOpenRequestFactory()
    }

    fun createCloseRequestFactory(channel: Channel): CloseRequest.Factory {
        return SimpleProtocolCloseRequestFactory()
    }

    fun createOutgoingMessageMetaDataFactory(channel: Channel): MessageMetaData.Factory {
        return object : MessageMetaData.Factory {}
    }

    fun createEventAdapterFactory(): ProtocolSpecificEventAdapter.Factory

    interface OpenRequest {
        interface Factory {
            fun create(channel: Channel): OpenRequest
        }

        object Empty : OpenRequest
    }

    interface OpenResponse {
        object Empty : OpenResponse
    }

    interface CloseRequest {
        interface Factory {
            fun create(channel: Channel): CloseRequest
        }

        object Empty : CloseRequest
    }

    interface CloseResponse {
        object Empty : CloseResponse
    }

    interface MessageMetaData {
        interface Factory {
            fun create(channel: Channel, message: Message): MessageMetaData = Empty
        }

        object Empty : MessageMetaData
    }
}
