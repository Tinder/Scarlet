/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.utils

import com.tinder.scarlet.Channel
import com.tinder.scarlet.Protocol

class SimpleProtocolOpenRequestFactory(
    val createCallable: (channel: Channel) -> Protocol.OpenRequest = { Protocol.OpenRequest.Empty }
) : Protocol.OpenRequest.Factory {
    override fun create(channel: Channel): Protocol.OpenRequest {
        return createCallable(channel)
    }
}