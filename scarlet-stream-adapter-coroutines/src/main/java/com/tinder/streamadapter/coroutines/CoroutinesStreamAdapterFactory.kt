/*
 * © 2013 - 2018 Tinder, Inc., ALL RIGHTS RESERVED
 */

package com.tinder.streamadapter.coroutines

import com.tinder.scarlet.StreamAdapter
import com.tinder.scarlet.utils.getRawType
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.Flow
import java.lang.reflect.Type

/**
 * A [stream adapter factory][StreamAdapter.Factory] that uses ReceiveChannel.
 */
class CoroutinesStreamAdapterFactory : StreamAdapter.Factory {

    override fun create(type: Type): StreamAdapter<Any, Any> {
        return when (type.getRawType()) {
            Flow::class.java -> FlowStreamAdapter()
            ReceiveChannel::class.java -> ReceiveChannelAdapter()
            else -> throw IllegalArgumentException()
        }
    }
}
