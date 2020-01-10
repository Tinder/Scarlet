package com.tinder.scarlet.stomp.okhttp

import com.tinder.scarlet.Channel
import com.tinder.scarlet.Protocol
import com.tinder.scarlet.ProtocolSpecificEventAdapter
import com.tinder.scarlet.stomp.core.StompHeader
import com.tinder.scarlet.utils.SimpleProtocolCloseRequestFactory
import com.tinder.scarlet.utils.SimpleProtocolOpenRequestFactory
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocketListener

class OkHttpStompClient(
    private val okHttpClient: OkHttpClient,
    private val requestFactory: RequestFactory
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
            requestFactory.createClientOpenRequest()
        }
    }

    override fun createCloseRequestFactory(channel: Channel): Protocol.CloseRequest.Factory {
        return SimpleProtocolCloseRequestFactory {
            requestFactory.createClientCloseRequest()
        }
    }

    override fun createEventAdapterFactory(): ProtocolSpecificEventAdapter.Factory {
        return object : ProtocolSpecificEventAdapter.Factory {}
    }

    interface RequestFactory {
        fun createClientCloseRequest(): ClientCloseRequest
        fun createClientOpenRequest(): ClientOpenRequest
    }

    open class SimpleRequestFactory(
        private val createClientCloseRequestCallable: () -> ClientCloseRequest,
        private val createClientOpenRequestCallable: () -> ClientOpenRequest
    ) : RequestFactory {

        override fun createClientCloseRequest(): ClientCloseRequest {
            return createClientCloseRequestCallable()
        }

        override fun createClientOpenRequest(): ClientOpenRequest {
            return createClientOpenRequestCallable()
        }
    }

    data class ClientOpenRequest(
        val host: String,
        val login: String? = null,
        val passcode: String? = null,
        val okHttpRequest: Request
    ) : Protocol.OpenRequest

    data class ClientCloseRequest(
        val code: Int,
        val reason: String
    ) : Protocol.CloseRequest

}