/*
 * © 2018 Match Group, LLC.
 */
package com.tinder.scarlet.stomp.okhttp.client

import com.tinder.scarlet.Channel
import com.tinder.scarlet.Protocol
import com.tinder.scarlet.ProtocolSpecificEventAdapter
import com.tinder.scarlet.stomp.okhttp.core.IdGenerator
import com.tinder.scarlet.stomp.okhttp.core.WebSocketFactory
import com.tinder.scarlet.stomp.okhttp.generator.UuidGenerator
import com.tinder.scarlet.utils.SimpleProtocolOpenRequestFactory
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocketListener

/**
 * Scarlet protocol implementation for create StompMainChannel
 * @see OkHttpStompMainChannel
 */
class OkHttpStompClient(
    private val configuration: OkHttpStompMainChannel.Configuration,
    private val okHttpClient: OkHttpClient,
    private val requestFactory: (Channel) -> ClientOpenRequest,
    private val idGenerator: IdGenerator = UuidGenerator()
) : Protocol {

    override fun createChannelFactory() = OkHttpStompMainChannel.Factory(
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