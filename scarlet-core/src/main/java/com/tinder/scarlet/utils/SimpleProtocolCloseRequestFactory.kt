/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.utils

import com.tinder.scarlet.Channel
import com.tinder.scarlet.Protocol

class SimpleProtocolCloseRequestFactory(
    val createCallable: (channel: Channel) -> Protocol.CloseRequest = { Protocol.CloseRequest.Empty }
) : Protocol.CloseRequest.Factory {
    override fun create(channel: Channel): Protocol.CloseRequest {
        return createCallable(channel)
    }
}