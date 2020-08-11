/*
 * © 2018 Match Group, LLC.
 */
package com.tinder.scarlet.stomp.okhttp.client

import com.tinder.scarlet.Channel
import com.tinder.scarlet.Message
import com.tinder.scarlet.Protocol
import com.tinder.scarlet.ProtocolSpecificEventAdapter
import com.tinder.scarlet.stomp.okhttp.models.StompHeader
import com.tinder.scarlet.utils.SimpleChannelFactory
import com.tinder.scarlet.utils.SimpleProtocolOpenRequestFactory

private typealias DestinationOpenRequestHeaderFactory = (Channel) -> OkHttpStompDestination.DestinationOpenRequest
private typealias MessageMetaDataFactory = (Channel, Message) -> OkHttpStompDestination.MessageMetaData

/**
 * Scarlet protocol implementation for create channel (OkHttpStompMessageChannel) for subscribe to
 * queue by destination.
 * @see OkHttpStompMessageChannel
 *
 * MessageMetaDataFactory is optional factory for create custom header for each message which will be sent
 * by this StompMessageChannel.
 *
 * DestinationOpenRequestHeaderFactory is optional factory for create open request header which will be sent
 * with subscribe message.
 */
class OkHttpStompDestination(
    private val destination: String,
    private val openRequestFactory: DestinationOpenRequestHeaderFactory? = null,
    private val createMessageMetaDataCallable: MessageMetaDataFactory? = null
) : Protocol {

    override fun createChannelFactory() = SimpleChannelFactory { listener, parent ->
        require(parent is OkHttpStompMainChannel)
        OkHttpStompMessageChannel(destination, parent, parent, listener)
    }

    override fun createOpenRequestFactory(channel: Channel) = SimpleProtocolOpenRequestFactory {
        openRequestFactory?.invoke(channel) ?: Protocol.OpenRequest.Empty
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