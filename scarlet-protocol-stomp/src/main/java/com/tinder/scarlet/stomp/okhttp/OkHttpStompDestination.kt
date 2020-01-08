package com.tinder.scarlet.stomp.okhttp

import com.tinder.scarlet.Channel
import com.tinder.scarlet.Protocol
import com.tinder.scarlet.ProtocolSpecificEventAdapter
import com.tinder.scarlet.utils.SimpleChannelFactory
import com.tinder.scarlet.utils.SimpleProtocolOpenRequestFactory

class OkHttpStompDestination(
    private val destination: String,
    private val openRequestFactory: RequestFactory
) : Protocol {

    interface RequestFactory {

        fun createDestinationOpenRequestHeader(destination: String): Map<String, String>

    }

    override fun createChannelFactory() = SimpleChannelFactory { listener, parent ->
        require(parent is OkHttpStompMainChannel)
        OkHttpStompMessageChannel(parent, destination, listener)
    }

    override fun createOpenRequestFactory(channel: Channel) = SimpleProtocolOpenRequestFactory {
        DestinationOpenRequest(openRequestFactory.createDestinationOpenRequestHeader(destination))
    }

    override fun createEventAdapterFactory(): ProtocolSpecificEventAdapter.Factory {
        return object : ProtocolSpecificEventAdapter.Factory {}
    }

    open class SimpleRequestFactory(
        private val createDestinationOpenRequestHeaderCallable: (String) -> Map<String, String>
    ) : RequestFactory {

        override fun createDestinationOpenRequestHeader(destination: String): Map<String, String> {
            return createDestinationOpenRequestHeaderCallable(destination)
        }
    }

    data class MessageMetaData(
        val headers: Map<String, String>
    ) : Protocol.MessageMetaData

    data class DestinationOpenRequest(
        val headers: Map<String, String>
    ) : Protocol.OpenRequest

}