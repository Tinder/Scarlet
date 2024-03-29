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
 * A [stream adapter factory][StreamAdapter.Factory] that allows for [ReceiveChannel]
 * and [Flow] based streams.
 * [bufferSize] is configurable for the underlying channels, defaults to [DEFAULT_BUFFER]
 */
private const val DEFAULT_BUFFER = 128

class CoroutinesStreamAdapterFactory(
    private val bufferSize: Int = DEFAULT_BUFFER
) : StreamAdapter.Factory {

    override fun create(type: Type): StreamAdapter<Any, Any> {
        return when (type.getRawType()) {
            Flow::class.java -> FlowStreamAdapter(bufferSize)
            ReceiveChannel::class.java -> ReceiveChannelStreamAdapter(bufferSize)
            else -> throw IllegalArgumentException()
        }
    }
}
