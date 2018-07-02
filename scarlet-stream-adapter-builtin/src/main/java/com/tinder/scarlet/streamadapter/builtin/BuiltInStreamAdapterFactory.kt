/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.streamadapter.builtin

import com.tinder.scarlet.Stream
import com.tinder.scarlet.StreamAdapter
import com.tinder.scarlet.utils.getRawType
import java.lang.reflect.Type

class BuiltInStreamAdapterFactory : StreamAdapter.Factory {

    override fun create(type: Type): StreamAdapter<Any, Any> = when (type.getRawType()) {
        Stream::class.java -> IdentityStreamAdapter()
        else -> throw IllegalArgumentException("$type is not supported.")
    }
}
