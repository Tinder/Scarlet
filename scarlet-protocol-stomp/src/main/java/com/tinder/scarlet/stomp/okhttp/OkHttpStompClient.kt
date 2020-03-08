package com.tinder.scarlet.stomp.okhttp

import com.tinder.scarlet.Channel
import com.tinder.scarlet.Protocol
import com.tinder.scarlet.ProtocolSpecificEventAdapter
import com.tinder.scarlet.stomp.core.IdGenerator
import com.tinder.scarlet.stomp.core.StompMainChannel
import com.tinder.scarlet.utils.SimpleProtocolOpenRequestFactory
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocketListener

/**
 * Scarlet protocol implementation for create StompMainChannel
 * @see StompMainChannel
 */
class OkHttpStompClient(
    private val configuration: StompMainChannel.Configuration,
    private val okHttpClient: OkHttpClient,
    private val requestFactory: (Channel) -> ClientOpenRequest,
    private val idGenerator: IdGenerator
) : Protocol {

    override fun createChannelFactory() = StompMainChannel.Factory(
        idGenerator = idGenerator,
        configuration = configuration,
        webSocketFactory = object : WebSocketFactory {
            override fun createWebSocket(request: Request, listener: WebSocketListener) {
                okHttpClient.newWebSocket(request, listener)
            }
        }
    )

    override fun createOpenRequestFactory(
        channel: Channel
    ) = SimpleProtocolOpenRequestFactory {
        requestFactory.invoke(channel)
    }

    override fun createEventAdapterFactory(): ProtocolSpecificEventAdapter.Factory {
        return object : ProtocolSpecificEventAdapter.Factory {}
    }

    data class ClientOpenRequest(
        val okHttpRequest: Request,
        val login: String? = null,
        val passcode: String? = null
    ) : Protocol.OpenRequest
}