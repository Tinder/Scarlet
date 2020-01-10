package com.tinder.scarlet.stomp.okhttp

import com.tinder.scarlet.Channel
import com.tinder.scarlet.Message
import com.tinder.scarlet.Protocol
import com.tinder.scarlet.ProtocolSpecificEventAdapter
import com.tinder.scarlet.stomp.core.StompHeader
import com.tinder.scarlet.utils.SimpleChannelFactory
import com.tinder.scarlet.utils.SimpleProtocolOpenRequestFactory

typealias DestinationOpenRequestHeaderFactory = () -> OkHttpStompDestination.DestinationOpenRequest
typealias MessageMetaDataFactory = (channel: Channel, message: Message) -> OkHttpStompDestination.MessageMetaData

class OkHttpStompDestination(
    private val destination: String,
    private val openRequestFactory: DestinationOpenRequestHeaderFactory,
    private val createMessageMetaDataCallable: MessageMetaDataFactory
) : Protocol {

    override fun createChannelFactory() = SimpleChannelFactory { listener, parent ->
        require(parent is OkHttpStompMainChannel)
        OkHttpStompMessageChannel(destination, parent, parent, listener)
    }

    override fun createOpenRequestFactory(channel: Channel) = SimpleProtocolOpenRequestFactory {
        openRequestFactory()
    }

    override fun createOutgoingMessageMetaDataFactory(channel: Channel): Protocol.MessageMetaData.Factory {
        return SimpleMessageMetaDataFactory(createMessageMetaDataCallable)
    }

    override fun createEventAdapterFactory(): ProtocolSpecificEventAdapter.Factory {
        return object : ProtocolSpecificEventAdapter.Factory {}
    }

    class SimpleMessageMetaDataFactory(
        private val createMessageMetaDataCallable: MessageMetaDataFactory
    ) : Protocol.MessageMetaData.Factory {

        override fun create(channel: Channel, message: Message): Protocol.MessageMetaData {
            return createMessageMetaDataCallable(channel, message)
        }
    }

    data class MessageMetaData(
        val headers: StompHeader
    ) : Protocol.MessageMetaData

    data class DestinationOpenRequest(
        val headers: StompHeader
    ) : Protocol.OpenRequest

}