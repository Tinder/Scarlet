/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.messageadapter.protobuf

import com.google.protobuf.ExtensionRegistryLite
import com.google.protobuf.InvalidProtocolBufferException
import com.google.protobuf.MessageLite
import com.google.protobuf.Parser
import com.tinder.scarlet.Message
import com.tinder.scarlet.MessageAdapter
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Type

/**
 * A [message adapter][MessageAdapter] that uses Protobuf.
 */
class ProtobufMessageAdapter<T : MessageLite> private constructor(
    private val parser: Parser<T>,
    private val registry: ExtensionRegistryLite?
) : MessageAdapter<T> {

    override fun fromMessage(message: Message): T {
        val bytesValue = when (message) {
            is Message.Text -> throw IllegalArgumentException("Text messages are not supported")
            is Message.Bytes -> message.value
        }
        try {
            return when (registry) {
                null -> parser.parseFrom(bytesValue)
                else -> parser.parseFrom(bytesValue, registry)
            }
        } catch (e: InvalidProtocolBufferException) {
            throw RuntimeException(e) // Despite extending IOException, this is data mismatch.
        }
    }

    override fun toMessage(data: T): Message = Message.Bytes(data.toByteArray())

    class Factory(
        private val registry: ExtensionRegistryLite? = null
    ) : MessageAdapter.Factory {

        override fun create(type: Type, annotations: Array<Annotation>): MessageAdapter<*> {
            require(type is Class<*>)
            val c = type as Class<*>
            require(MessageLite::class.java.isAssignableFrom(type))

            var parser: Parser<MessageLite>
            try {
                val method = c.getDeclaredMethod("parser")
                @Suppress("UNCHECKED_CAST")
                parser = method.invoke(null) as Parser<MessageLite>
            } catch (e: InvocationTargetException) {
                throw RuntimeException(e.cause)
            } catch (ignored: NoSuchMethodException) {
                // If the method is missing, fall back to original static field for pre-3.0 support.
                try {
                    val field = c.getDeclaredField("PARSER")
                    @Suppress("UNCHECKED_CAST")
                    parser = field.get(null) as Parser<MessageLite>
                } catch (e: NoSuchFieldException) {
                    throw IllegalArgumentException(
                        "Found a protobuf message but ${c.name} had no parser() method or PARSER field."
                    )
                } catch (e: IllegalAccessException) {
                    throw IllegalArgumentException("Found a protobuf message but ${c.name} had no parser() method or PARSER field.")
                }
            } catch (ignored: IllegalAccessException) {
                try {
                    val field = c.getDeclaredField("PARSER")
                    @Suppress("UNCHECKED_CAST")
                    parser = field.get(null) as Parser<MessageLite>
                } catch (e: NoSuchFieldException) {
                    throw IllegalArgumentException("Found a protobuf message but ${c.name} had no parser() method or PARSER field.")
                } catch (e: IllegalAccessException) {
                    throw IllegalArgumentException("Found a protobuf message but ${c.name} had no parser() method or PARSER field.")
                }
            }
            return ProtobufMessageAdapter(parser, registry)
        }
    }
}
