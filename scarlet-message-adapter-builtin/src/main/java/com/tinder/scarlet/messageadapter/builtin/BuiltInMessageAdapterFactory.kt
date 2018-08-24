/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.messageadapter.builtin

import com.tinder.scarlet.MessageAdapter
import com.tinder.scarlet.utils.getRawType
import java.lang.reflect.Type

class BuiltInMessageAdapterFactory : MessageAdapter.Factory {

    override fun create(type: Type, annotations: Array<Annotation>): MessageAdapter<*> = when (type.getRawType()) {
        String::class.java -> TextMessageAdapter()
        ByteArray::class.java -> ByteArrayMessageAdapter()
        else -> throw IllegalArgumentException("Type is not supported by this MessageAdapterFactory: $type")
    }
}
