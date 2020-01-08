package com.tinder.scarlet.stomp.okhttp

import com.tinder.scarlet.Channel
import com.tinder.scarlet.Protocol
import com.tinder.scarlet.ProtocolSpecificEventAdapter
import com.tinder.scarlet.utils.SimpleProtocolOpenRequestFactory
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocketListener

class OkHttpStompClient(
    private val okHttpClient: OkHttpClient,
    private val openRequestFactory: RequestFactory
) : Protocol {

    override fun createChannelFactory(): Channel.Factory {
        return OkHttpStompMainChannel.Factory(
            object : WebSocketFactory {
                override fun createWebSocket(request: Request, listener: WebSocketListener) {
                    okHttpClient.newWebSocket(request, listener)
                }
            }
        )
    }

    override fun createOpenRequestFactory(channel: Channel): Protocol.OpenRequest.Factory {
        return SimpleProtocolOpenRequestFactory {
            openRequestFactory.createClientOpenRequest()
        }
    }

    override fun createEventAdapterFactory(): ProtocolSpecificEventAdapter.Factory {
        return object : ProtocolSpecificEventAdapter.Factory {}
    }

    interface RequestFactory {
        fun createClientOpenRequest(): ClientOpenSocketRequest
    }

    open class SimpleRequestFactory(
        private val createClientOpenRequestCallable: () -> ClientOpenSocketRequest
    ) : RequestFactory {
        override fun createClientOpenRequest(): ClientOpenSocketRequest {
            return createClientOpenRequestCallable()
        }
    }

    data class ClientOpenSocketRequest(
        val okHttpRequest: Request
    ) : Protocol.OpenRequest

}