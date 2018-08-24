/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.internal.servicemethod

import com.tinder.scarlet.MessageAdapter
import io.reactivex.exceptions.CompositeException
import java.lang.reflect.Type
import java.util.Arrays

internal class MessageAdapterResolver(
    private val messageAdapterFactories: List<MessageAdapter.Factory>
) {

    private val messageAdapterCache = mutableMapOf<MessageAdapterKey, MessageAdapter<Any>>()

    fun resolve(type: Type, annotations: Array<Annotation>): MessageAdapter<Any> {
        val key = MessageAdapterKey(type, annotations)
        if (messageAdapterCache.contains(key)) {
            return messageAdapterCache[key]!!
        }
        val messageAdapter = findMessageAdapter(type, annotations)
        messageAdapterCache[key] = messageAdapter
        return messageAdapter
    }

    private fun findMessageAdapter(type: Type, annotations: Array<Annotation>): MessageAdapter<Any> {
        val throwables = mutableListOf<Throwable>()
        for (adapterFactory in messageAdapterFactories) {
            try {
                @Suppress("UNCHECKED_CAST")
                return adapterFactory.create(type, annotations) as MessageAdapter<Any>
            } catch (e: Throwable) {
                // This type is not supported by this adapter
                throwables.add(e)
            }
        }
        val compositeException = CompositeException(*throwables.toTypedArray())
        throw IllegalStateException(
            "Cannot resolve message adapter for type: $type, annotations: $annotations.",
            compositeException
        )
    }

    private data class MessageAdapterKey(val type: Type, val annotations: Array<Annotation>) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as MessageAdapterKey

            if (type != other.type) return false
            if (!Arrays.equals(annotations, other.annotations)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = type.hashCode()
            result = 31 * result + Arrays.hashCode(annotations)
            return result
        }
    }
}
