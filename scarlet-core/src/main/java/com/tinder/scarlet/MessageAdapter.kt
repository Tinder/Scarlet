/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet

import com.tinder.scarlet.MessageAdapter.Factory
import java.lang.reflect.Type

/**
 * Adapts objects of type `T` from and to [Message], their representation in WebSocket. Instances are created by
 * [a factory][Factory] which is [installed][Scarlet.Builder.addMessageAdapterFactory] into the [Scarlet] instance.
 */
interface MessageAdapter<T> {

    /** Returns an object of type `T` that represents a [Message]. */
    fun fromMessage(message: Message): T

    /** Returns a [Message] that represents [data]. */
    fun toMessage(data: T): Message

    /** Creates [MessageAdapter] instances based on a type and target usage. */
    interface Factory {

        /**
         * Returns a [MessageAdapter] for adapting an [type] from and to [Message], throws an exception if [type] cannot
         * be handled by this factory.
         */
        fun create(type: Type, annotations: Array<Annotation>): MessageAdapter<*>
    }
}
