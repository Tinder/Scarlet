package com.tinder.scarlet.stomp.okhttp

import com.tinder.scarlet.Channel
import com.tinder.scarlet.Message
import com.tinder.scarlet.Protocol
import com.tinder.scarlet.ProtocolSpecificEventAdapter
import com.tinder.scarlet.stomp.core.StompHeader
import com.tinder.scarlet.utils.SimpleChannelFactory
import com.tinder.scarlet.utils.SimpleProtocolOpenRequestFactory

typealias DestinationOpenRequestHeaderFactory = () -> StompDestination.DestinationOpenRequest
typealias MessageMetaDataFactory = (channel: Channel, message: Message) -> StompDestination.MessageMetaData

/**
 *
 */
class StompDestination(
    private val destination: String,
    private val openRequestFactory: DestinationOpenRequestHeaderFactory? = null,
    private val createMessageMetaDataCallable: MessageMetaDataFactory? = null
) : Protocol {

    override fun createChannelFactory() = SimpleChannelFactory { listener, parent ->
        require(parent is OkHttpStompMainChannel)
        StompMessageChannel(destination, parent, parent, listener)
    }

    override fun createOpenRequestFactory(channel: Channel) = SimpleProtocolOpenRequestFactory {
        openRequestFactory?.invoke() ?: Protocol.OpenRequest.Empty
    }

    override fun createOutgoingMessageMetaDataFactory(
        channel: Channel
    ) = SimpleMessageMetaDataFactory(createMessageMetaDataCallable)

    override fun createEventAdapterFactory(): ProtocolSpecificEventAdapter.Factory {
        return object : ProtocolSpecificEventAdapter.Factory {}
    }

    class SimpleMessageMetaDataFactory(
        private val createMessageMetaDataCallable: MessageMetaDataFactory?
    ) : Protocol.MessageMetaData.Factory {

        override fun create(
            channel: Channel,
            message: Message
        ) = createMessageMetaDataCallable?.invoke(channel, message)
                ?: Protocol.MessageMetaData.Empty

    }

    data class MessageMetaData(
        val headers: StompHeader
    ) : Protocol.MessageMetaData

    data class DestinationOpenRequest(
        val headers: StompHeader
    ) : Protocol.OpenRequest

}