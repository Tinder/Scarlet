/*
 * © 2013 - 2018 Tinder, Inc., ALL RIGHTS RESERVED
 */

package com.tinder.streamadapter.coroutines

import com.tinder.scarlet.StreamAdapter
import com.tinder.scarlet.utils.getRawType
import kotlinx.coroutines.channels.ReceiveChannel
import java.lang.reflect.Type

/**
 * A [stream adapter factory][StreamAdapter.Factory] that uses ReceiveChannel.
 */
class CoroutinesStreamAdapterFactory : StreamAdapter.Factory {

    override fun create(type: Type): StreamAdapter<Any, Any> {
        return when (type.getRawType()) {
            ReceiveChannel::class.java -> ReceiveChannelStreamAdapter()
            else -> throw IllegalArgumentException()
        }
    }
}